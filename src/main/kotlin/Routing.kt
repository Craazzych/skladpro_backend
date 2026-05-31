package com.skladpro

import com.skladpro.employees.repository.InMemoryEmployeeRepository
import com.skladpro.employees.repository.PostgresEmployeeRepository
import com.skladpro.employees.routes.authRoutes
import com.skladpro.employees.routes.employeeRoutes
import com.skladpro.employees.security.PasswordHasher
import com.skladpro.employees.service.EmployeeService
import com.skladpro.inventory.routes.inventoryRoutes
import com.skladpro.inventory.repository.InMemoryInventoryRepository
import com.skladpro.inventory.repository.PostgresInventoryRepository
import com.skladpro.inventory.service.InventoryService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val inventoryService = InventoryService(createInventoryRepository())
    val passwordHasher = PasswordHasher()
    val employeeService = EmployeeService(
        repository = createEmployeeRepository(passwordHasher),
        passwordHasher = passwordHasher
    )
    configureAppRouting(inventoryService, employeeService)
}

fun Application.configureInventoryRouting(inventoryService: InventoryService) {
    routing {
        inventoryRoutes(inventoryService)
    }
}

fun Application.configureAppRouting(
    inventoryService: InventoryService,
    employeeService: EmployeeService
) {
    routing {
        get("/") {
            call.respondText("SkladPRO backend is running")
        }
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        inventoryRoutes(inventoryService)
        employeeRoutes(employeeService)
        authRoutes(employeeService)
    }
}

private fun Application.createInventoryRepository() =
    when (environment.config.propertyOrNull("storage.mode")?.getString()) {
        "postgres" -> PostgresInventoryRepository.create(
            jdbcUrl = environment.config.property("database.jdbcUrl").getString(),
            user = environment.config.property("database.user").getString(),
            password = environment.config.property("database.password").getString(),
            maximumPoolSize = environment.config
                .propertyOrNull("database.maximumPoolSize")
                ?.getString()
                ?.toIntOrNull()
                ?: 5
        )
        else -> InMemoryInventoryRepository()
    }

private fun Application.createEmployeeRepository(passwordHasher: PasswordHasher) =
    when (environment.config.propertyOrNull("storage.mode")?.getString()) {
        "postgres" -> PostgresEmployeeRepository.create(
            jdbcUrl = environment.config.property("database.jdbcUrl").getString(),
            user = environment.config.property("database.user").getString(),
            password = environment.config.property("database.password").getString(),
            maximumPoolSize = environment.config
                .propertyOrNull("database.maximumPoolSize")
                ?.getString()
                ?.toIntOrNull()
                ?: 5,
            passwordHasher = passwordHasher
        )
        else -> InMemoryEmployeeRepository()
    }
