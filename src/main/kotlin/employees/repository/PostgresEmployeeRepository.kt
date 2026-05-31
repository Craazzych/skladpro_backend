package com.skladpro.employees.repository

import com.skladpro.employees.model.Employee
import com.skladpro.employees.model.EmployeeRole
import com.skladpro.employees.model.EmployeeStatus
import com.skladpro.employees.security.PasswordHasher
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class PostgresEmployeeRepository(
    private val dataSource: HikariDataSource,
    private val passwordHasher: PasswordHasher
) : EmployeeRepository {
    init {
        createSchema()
        seedIfEmpty()
    }

    override fun getAll(): List<Employee> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                select * from employees
                order by created_at desc, full_name asc
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    return resultSet.toEmployees()
                }
            }
        }
    }

    override fun getByLogin(login: String): Employee? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "select * from employees where lower(login) = lower(?)"
            ).use { statement ->
                statement.setString(1, login)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) resultSet.toEmployee() else null
                }
            }
        }
    }

    override fun getById(id: String): Employee? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "select * from employees where id = ?"
            ).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) resultSet.toEmployee() else null
                }
            }
        }
    }

    override fun countByRole(role: EmployeeRole): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "select count(*) from employees where role = ?"
            ).use { statement ->
                statement.setString(1, role.storageValue)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    return resultSet.getInt(1)
                }
            }
        }
    }

    override fun create(employee: Employee): Employee {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                insert into employees (
                    id, full_name, login, role, status, temporary_password, password_hash
                ) values (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.bindEmployee(employee)
                statement.executeUpdate()
            }
        }
        return employee
    }

    override fun update(employee: Employee): Employee? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                update employees
                set full_name = ?,
                    login = ?,
                    role = ?,
                    status = ?,
                    temporary_password = ?,
                    password_hash = ?,
                    updated_at = current_timestamp
                where id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, employee.fullName)
                statement.setString(2, employee.login)
                statement.setString(3, employee.role.storageValue)
                statement.setString(4, employee.status.storageValue)
                statement.setString(5, employee.temporaryPassword)
                statement.setString(6, employee.passwordHash)
                statement.setString(7, employee.id)
                return if (statement.executeUpdate() == 1) employee else null
            }
        }
    }

    override fun delete(id: String): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "delete from employees where id = ?"
            ).use { statement ->
                statement.setString(1, id)
                return statement.executeUpdate() == 1
            }
        }
    }

    private fun createSchema() {
        val schema = javaClass.classLoader
            .getResource("db/schema.sql")
            ?.readText()
            ?: error("db/schema.sql not found")

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(schema)
            }
        }
    }

    private fun seedIfEmpty() {
        if (getAll().isNotEmpty()) return

        create(
            Employee(
                id = "admin-1",
                fullName = "Администратор СкладПРО",
                login = "admin",
                role = EmployeeRole.Admin,
                status = EmployeeStatus.Active,
                passwordHash = passwordHasher.hash("admin123")
            )
        )
        create(
            Employee(
                id = "employee-1",
                fullName = "Орлова Мария Андреевна",
                login = "m.orlova",
                role = EmployeeRole.Worker,
                status = EmployeeStatus.PendingActivation,
                temporaryPassword = "TMP-654321"
            )
        )
    }

    private fun ResultSet.toEmployees(): List<Employee> {
        val employees = mutableListOf<Employee>()
        while (next()) {
            employees.add(toEmployee())
        }
        return employees
    }

    private fun ResultSet.toEmployee(): Employee {
        return Employee(
            id = getString("id"),
            fullName = getString("full_name"),
            login = getString("login"),
            role = EmployeeRole.fromStorageValue(getString("role")),
            status = EmployeeStatus.fromStorageValue(getString("status")),
            temporaryPassword = getString("temporary_password"),
            passwordHash = getString("password_hash")
        )
    }

    private fun java.sql.PreparedStatement.bindEmployee(employee: Employee) {
        setString(1, employee.id)
        setString(2, employee.fullName)
        setString(3, employee.login)
        setString(4, employee.role.storageValue)
        setString(5, employee.status.storageValue)
        setString(6, employee.temporaryPassword)
        setString(7, employee.passwordHash)
    }

    companion object {
        fun create(
            jdbcUrl: String,
            user: String,
            password: String,
            maximumPoolSize: Int,
            passwordHasher: PasswordHasher
        ): PostgresEmployeeRepository {
            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                username = user
                this.password = password
                this.maximumPoolSize = maximumPoolSize
            }
            return PostgresEmployeeRepository(
                dataSource = HikariDataSource(config),
                passwordHasher = passwordHasher
            )
        }
    }
}
