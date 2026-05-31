package com.skladpro.employees.model

data class Employee(
    val id: String,
    val fullName: String,
    val login: String,
    val role: EmployeeRole,
    val status: EmployeeStatus,
    val temporaryPassword: String? = null,
    val passwordHash: String? = null
)

enum class EmployeeRole(val storageValue: String) {
    Admin("admin"),
    Worker("worker");

    companion object {
        fun fromStorageValue(value: String): EmployeeRole =
            entries.first { it.storageValue == value }

        fun fromRequest(value: String): EmployeeRole? =
            entries.firstOrNull { it.storageValue.equals(value.trim(), ignoreCase = true) }
    }
}

enum class EmployeeStatus(val storageValue: String) {
    PendingActivation("pending_activation"),
    Active("active");

    companion object {
        fun fromStorageValue(value: String): EmployeeStatus =
            entries.first { it.storageValue == value }
    }
}
