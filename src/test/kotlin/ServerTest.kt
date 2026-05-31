package com.skladpro

import com.skladpro.employees.model.Employee
import com.skladpro.employees.model.EmployeeRole
import com.skladpro.employees.model.EmployeeStatus
import com.skladpro.employees.repository.InMemoryEmployeeRepository
import com.skladpro.employees.service.EmployeeService
import com.skladpro.inventory.repository.InMemoryInventoryRepository
import com.skladpro.inventory.service.InventoryService
import com.skladpro.plugins.configureSerialization
import com.skladpro.security.JwtService
import com.skladpro.security.configureJwtAuthentication
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerTest {
    @Test
    fun `test root endpoint`() = testApplication {
        configureTestApplication()

        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `items endpoint returns seeded inventory`() = testApplication {
        val jwtService = configureTestApplication()

        val response = client.get("/api/items") {
            bearerAuth(jwtService.issueToken(testAdmin))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Стальные болты М8"))
    }

    @Test
    fun `stock operation marks item as purchase required`() = testApplication {
        val jwtService = configureTestApplication()

        val response = client.post("/api/items/3/operations") {
            bearerAuth(jwtService.issueToken(testWorker))
            contentType(ContentType.Application.Json)
            setBody("""{"quantityDelta":-80.0}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(""""requiresPurchase": true"""))
    }

    @Test
    fun `invalid delivery date is rejected`() = testApplication {
        val jwtService = configureTestApplication()

        val response = client.post("/api/items") {
            bearerAuth(jwtService.issueToken(testAdmin))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name":"Тестовый товар",
                  "sku":"TEST-1",
                  "category":"Тест",
                  "unit":"шт.",
                  "quantity":10.0,
                  "minQuantity":2.0,
                  "department":"A",
                  "shelf":"B",
                  "cell":"C",
                  "expectedDeliveryDate":"12.18.2022",
                  "expectedDeliveryQuantity":5.0
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `created employee receives temporary password`() = testApplication {
        val jwtService = configureTestApplication()

        val response = client.post("/api/employees") {
            bearerAuth(jwtService.issueToken(testAdmin))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "fullName":"Петров Петр Петрович",
                  "login":"p.petrov",
                  "role":"worker"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("pending_activation", body["status"]?.jsonPrimitive?.content)
        assertTrue(body["temporaryPassword"]?.jsonPrimitive?.content?.startsWith("TMP-") == true)
    }

    @Test
    fun `employee can activate account and login`() = testApplication {
        val jwtService = configureTestApplication()

        val createResponse = client.post("/api/employees") {
            bearerAuth(jwtService.issueToken(testAdmin))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "fullName":"Петров Петр Петрович",
                  "login":"p.petrov",
                  "role":"worker"
                }
                """.trimIndent()
            )
        }
        val temporaryPassword = Json
            .parseToJsonElement(createResponse.bodyAsText())
            .jsonObject["temporaryPassword"]
            ?.jsonPrimitive
            ?.content
            ?: error("Temporary password missing")

        val activateResponse = client.post("/api/auth/activate") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "login":"p.petrov",
                  "temporaryPassword":"$temporaryPassword",
                  "newPassword":"strong-password"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, activateResponse.status)
        assertFalse(activateResponse.bodyAsText().contains("temporaryPassword"))

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"p.petrov","password":"strong-password"}""")
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginBody = Json.parseToJsonElement(loginResponse.bodyAsText()).jsonObject
        assertEquals(
            "p.petrov",
            loginBody["employee"]?.jsonObject?.get("login")?.jsonPrimitive?.content
        )
        assertNotNull(loginBody["token"]?.jsonPrimitive?.content)
    }

    @Test
    fun `protected endpoint rejects anonymous request`() = testApplication {
        configureTestApplication()

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/items").status)
    }

    @Test
    fun `worker cannot create inventory item`() = testApplication {
        val jwtService = configureTestApplication()

        val response = client.post("/api/items") {
            bearerAuth(jwtService.issueToken(testWorker))
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name":"Тестовый товар",
                  "sku":"TEST-WORKER",
                  "category":"Тест",
                  "unit":"шт.",
                  "quantity":10.0,
                  "minQuantity":2.0,
                  "department":"A",
                  "shelf":"B",
                  "cell":"C"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `worker cannot read employees`() = testApplication {
        val jwtService = configureTestApplication()

        val response = client.get("/api/employees") {
            bearerAuth(jwtService.issueToken(testWorker))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `administrator cannot delete own profile`() {
        val repository = InMemoryEmployeeRepository(listOf(testAdmin))
        val result = EmployeeService(repository).delete(id = "admin-1", actorId = "admin-1")

        assertEquals("Нельзя удалить собственный профиль", result.exceptionOrNull()?.message)
        assertNotNull(repository.getById("admin-1"))
    }

    @Test
    fun `administrator cannot delete last administrator`() {
        val repository = InMemoryEmployeeRepository(listOf(testAdmin, testWorker))
        val result = EmployeeService(repository).delete(id = "admin-1", actorId = "worker-1")

        assertEquals("Нельзя удалить последнего администратора", result.exceptionOrNull()?.message)
        assertNotNull(repository.getById("admin-1"))
    }

    @Test
    fun `administrator can delete another administrator when replacement exists`() {
        val secondAdmin = testAdmin.copy(id = "admin-2", login = "admin.two")
        val repository = InMemoryEmployeeRepository(listOf(testAdmin, secondAdmin))
        val result = EmployeeService(repository).delete(id = "admin-2", actorId = "admin-1")

        assertTrue(result.isSuccess)
        assertNull(repository.getById("admin-2"))
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.configureTestApplication(): JwtService {
        val jwtService = JwtService.test()
        application {
            configureHttp()
            configureSerialization()
            configureStatusPages()
            configureJwtAuthentication(jwtService)
            configureAppRouting(
                inventoryService = InventoryService(InMemoryInventoryRepository()),
                employeeService = EmployeeService(
                    InMemoryEmployeeRepository(listOf(testAdmin, testWorker))
                ),
                jwtService = jwtService
            )
        }
        return jwtService
    }

    private companion object {
        val testAdmin = Employee(
            id = "admin-1",
            fullName = "Администратор",
            login = "admin",
            role = EmployeeRole.Admin,
            status = EmployeeStatus.Active,
            passwordHash = "hash"
        )

        val testWorker = Employee(
            id = "worker-1",
            fullName = "Работник",
            login = "worker",
            role = EmployeeRole.Worker,
            status = EmployeeStatus.Active,
            passwordHash = "hash"
        )
    }
}
