package com.skladpro.employees.repository

import com.skladpro.employees.model.Employee

interface EmployeeRepository {
    fun getAll(): List<Employee>

    fun getByLogin(login: String): Employee?

    fun getById(id: String): Employee?

    fun countByRole(role: com.skladpro.employees.model.EmployeeRole): Int

    fun create(employee: Employee): Employee

    fun update(employee: Employee): Employee?

    fun delete(id: String): Boolean
}
