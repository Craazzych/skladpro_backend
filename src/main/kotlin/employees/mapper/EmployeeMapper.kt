package com.skladpro.employees.mapper

import com.skladpro.employees.dto.EmployeeResponse
import com.skladpro.employees.model.Employee

fun Employee.toResponse(): EmployeeResponse {
    return EmployeeResponse(
        id = id,
        fullName = fullName,
        login = login,
        role = role.storageValue,
        status = status.storageValue,
        temporaryPassword = temporaryPassword
    )
}
