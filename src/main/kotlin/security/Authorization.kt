package com.skladpro.security

import com.skladpro.employees.model.EmployeeRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respond

fun ApplicationCall.currentEmployee(): EmployeePrincipal {
    return principal<EmployeePrincipal>()
        ?: error("Authenticated employee principal is missing")
}

suspend fun ApplicationCall.requireAdmin(): Boolean {
    if (currentEmployee().role == EmployeeRole.Admin) return true
    respond(HttpStatusCode.Forbidden, mapOf("message" to "Недостаточно прав"))
    return false
}
