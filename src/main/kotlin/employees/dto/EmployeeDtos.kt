package com.skladpro.employees.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmployeeResponse(
    val id: String,
    val fullName: String,
    val login: String,
    val role: String,
    val status: String,
    val temporaryPassword: String? = null
)

@Serializable
data class CreateEmployeeRequest(
    val fullName: String,
    val login: String,
    val role: String = "worker"
)

@Serializable
data class ActivateEmployeeRequest(
    val login: String,
    val temporaryPassword: String,
    val newPassword: String
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val employee: EmployeeResponse,
    val token: String
)

@Serializable
data class EmployeeErrorResponse(
    val message: String
)
