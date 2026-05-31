package com.skladpro.employees.routes

import com.skladpro.employees.dto.ActivateEmployeeRequest
import com.skladpro.employees.dto.CreateEmployeeRequest
import com.skladpro.employees.dto.EmployeeErrorResponse
import com.skladpro.employees.dto.LoginRequest
import com.skladpro.employees.dto.LoginResponse
import com.skladpro.employees.mapper.toResponse
import com.skladpro.employees.model.Employee
import com.skladpro.employees.service.EmployeeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.employeeRoutes(service: EmployeeService) {
    route("/api/employees") {
        get {
            call.respond(service.getAll().map { it.toResponse() })
        }

        post {
            val request = call.receive<CreateEmployeeRequest>()
            call.respondEmployeeResult(
                result = service.create(request),
                successStatus = HttpStatusCode.Created
            )
        }

        delete("/{id}") {
            val id = call.parameters["id"].orEmpty()
            val actorId = call.request.headers["X-Actor-Employee-Id"].orEmpty()
            service.delete(id, actorId).fold(
                onSuccess = { call.respond(HttpStatusCode.NoContent) },
                onFailure = { call.respondEmployeeError(it) }
            )
        }
    }
}

fun Route.authRoutes(service: EmployeeService) {
    route("/api/auth") {
        post("/activate") {
            val request = call.receive<ActivateEmployeeRequest>()
            call.respondEmployeeResult(service.activate(request))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = service.login(request)
            val employee = result.getOrNull()
            if (employee == null) {
                call.respondEmployeeError(result.exceptionOrNull()!!)
            } else {
                call.respond(LoginResponse(employee.toResponse()))
            }
        }
    }
}

private suspend fun ApplicationCall.respondEmployeeResult(
    result: Result<Employee>,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    val employee = result.getOrNull()
    if (employee == null) {
        respondEmployeeError(result.exceptionOrNull()!!)
    } else {
        respond(successStatus, employee.toResponse())
    }
}

private suspend fun ApplicationCall.respondEmployeeError(error: Throwable) {
    val status = when (error) {
        is NoSuchElementException -> HttpStatusCode.NotFound
        is IllegalStateException -> HttpStatusCode.Conflict
        is IllegalArgumentException -> HttpStatusCode.BadRequest
        else -> HttpStatusCode.InternalServerError
    }
    respond(status, EmployeeErrorResponse(error.message ?: "Ошибка обработки запроса"))
}
