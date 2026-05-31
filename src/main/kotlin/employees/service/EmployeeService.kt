package com.skladpro.employees.service

import com.skladpro.employees.dto.ActivateEmployeeRequest
import com.skladpro.employees.dto.CreateEmployeeRequest
import com.skladpro.employees.dto.LoginRequest
import com.skladpro.employees.model.Employee
import com.skladpro.employees.model.EmployeeRole
import com.skladpro.employees.model.EmployeeStatus
import com.skladpro.employees.repository.EmployeeRepository
import com.skladpro.employees.security.PasswordHasher
import java.security.SecureRandom
import java.util.UUID

class EmployeeService(
    private val repository: EmployeeRepository,
    private val passwordHasher: PasswordHasher = PasswordHasher()
) {
    private val random = SecureRandom()

    fun getAll(): List<Employee> = repository.getAll()

    fun create(request: CreateEmployeeRequest): Result<Employee> {
        val fullName = request.fullName.trim()
        val login = request.login.trim()
        val role = EmployeeRole.fromRequest(request.role)
            ?: return Result.failure(IllegalArgumentException("Неизвестная роль сотрудника"))

        when {
            fullName.isBlank() ->
                return Result.failure(IllegalArgumentException("ФИО обязательно"))
            login.length < 3 ->
                return Result.failure(IllegalArgumentException("Логин должен содержать минимум 3 символа"))
            repository.getByLogin(login) != null ->
                return Result.failure(IllegalStateException("Сотрудник с таким логином уже существует"))
        }

        return Result.success(
            repository.create(
                Employee(
                    id = UUID.randomUUID().toString(),
                    fullName = fullName,
                    login = login,
                    role = role,
                    status = EmployeeStatus.PendingActivation,
                    temporaryPassword = generateTemporaryPassword()
                )
            )
        )
    }

    fun delete(id: String, actorId: String): Result<Unit> {
        if (actorId.isBlank()) {
            return Result.failure(IllegalArgumentException("Не удалось определить текущего пользователя"))
        }
        if (id == actorId) {
            return Result.failure(IllegalStateException("Нельзя удалить собственный профиль"))
        }

        val employee = repository.getById(id)
            ?: return Result.failure(NoSuchElementException("Сотрудник не найден"))

        if (
            employee.role == EmployeeRole.Admin &&
            repository.countByRole(EmployeeRole.Admin) <= 1
        ) {
            return Result.failure(
                IllegalStateException("Нельзя удалить последнего администратора")
            )
        }

        if (!repository.delete(id)) {
            return Result.failure(NoSuchElementException("Сотрудник не найден"))
        }
        return Result.success(Unit)
    }

    fun activate(request: ActivateEmployeeRequest): Result<Employee> {
        if (request.newPassword.length < 6) {
            return Result.failure(
                IllegalArgumentException("Новый пароль должен содержать минимум 6 символов")
            )
        }

        val employee = repository.getByLogin(request.login.trim())
            ?: return Result.failure(NoSuchElementException("Сотрудник не найден"))

        if (employee.status != EmployeeStatus.PendingActivation) {
            return Result.failure(IllegalStateException("Учетная запись уже активирована"))
        }
        if (employee.temporaryPassword != request.temporaryPassword) {
            return Result.failure(IllegalArgumentException("Неверный временный пароль"))
        }

        val activatedEmployee = employee.copy(
            status = EmployeeStatus.Active,
            temporaryPassword = null,
            passwordHash = passwordHasher.hash(request.newPassword)
        )
        return Result.success(repository.update(activatedEmployee) ?: activatedEmployee)
    }

    fun login(request: LoginRequest): Result<Employee> {
        val employee = repository.getByLogin(request.login.trim())
            ?: return invalidCredentials()

        if (employee.status != EmployeeStatus.Active) {
            return Result.failure(IllegalStateException("Сначала активируйте учетную запись"))
        }

        val passwordHash = employee.passwordHash ?: return invalidCredentials()
        if (!passwordHasher.matches(request.password, passwordHash)) {
            return invalidCredentials()
        }

        return Result.success(employee)
    }

    private fun generateTemporaryPassword(): String {
        return "TMP-${random.nextInt(900_000) + 100_000}"
    }

    private fun invalidCredentials(): Result<Employee> {
        return Result.failure(IllegalArgumentException("Неверный логин или пароль"))
    }
}
