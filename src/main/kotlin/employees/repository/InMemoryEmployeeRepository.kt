package com.skladpro.employees.repository

import com.skladpro.employees.model.Employee
import com.skladpro.employees.model.EmployeeRole

class InMemoryEmployeeRepository(
    initialEmployees: List<Employee> = emptyList()
) : EmployeeRepository {
    private val employees = initialEmployees.toMutableList()

    override fun getAll(): List<Employee> = employees.toList()

    override fun getByLogin(login: String): Employee? {
        return employees.firstOrNull { it.login.equals(login, ignoreCase = true) }
    }

    override fun getById(id: String): Employee? = employees.firstOrNull { it.id == id }

    override fun countByRole(role: EmployeeRole): Int = employees.count { it.role == role }

    override fun create(employee: Employee): Employee {
        employees.add(0, employee)
        return employee
    }

    override fun update(employee: Employee): Employee? {
        val index = employees.indexOfFirst { it.id == employee.id }
        if (index == -1) return null
        employees[index] = employee
        return employee
    }

    override fun delete(id: String): Boolean {
        return employees.removeIf { it.id == id }
    }
}
