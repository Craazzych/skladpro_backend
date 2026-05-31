package com.skladpro.inventory.routes

import com.skladpro.inventory.dto.DeliveryRequest
import com.skladpro.inventory.dto.ErrorResponse
import com.skladpro.inventory.dto.InventoryItemRequest
import com.skladpro.inventory.dto.StockOperationRequest
import com.skladpro.inventory.mapper.toResponse
import com.skladpro.inventory.service.InventoryService
import com.skladpro.security.requireAdmin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.inventoryRoutes(service: InventoryService) {
    route("/api/items") {
        get {
            val query = call.request.queryParameters["query"].orEmpty()
            val items = service.search(query).map { it.toResponse() }
            call.respond(items)
        }

        post {
            if (!call.requireAdmin()) return@post
            val request = call.receive<InventoryItemRequest>()
            val result = service.create(request)
            call.respondResult(
                result = result,
                successStatus = HttpStatusCode.Created
            )
        }

        get("/{id}") {
            val id = call.parameters["id"].orEmpty()
            val item = service.getById(id)

            if (item == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Товар не найден"))
            } else {
                call.respond(item.toResponse())
            }
        }

        put("/{id}") {
            if (!call.requireAdmin()) return@put
            val id = call.parameters["id"].orEmpty()
            val request = call.receive<InventoryItemRequest>()
            val result = service.update(id, request)
            call.respondResult(result)
        }

        delete("/{id}") {
            if (!call.requireAdmin()) return@delete
            val id = call.parameters["id"].orEmpty()
            val deleted = service.delete(id)

            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Товар не найден"))
            }
        }

        post("/{id}/operations") {
            val id = call.parameters["id"].orEmpty()
            val request = call.receive<StockOperationRequest>()
            val result = service.applyStockOperation(id, request.quantityDelta)
            call.respondResult(result)
        }

        put("/{id}/delivery") {
            if (!call.requireAdmin()) return@put
            val id = call.parameters["id"].orEmpty()
            val request = call.receive<DeliveryRequest>()
            val result = service.updateDelivery(id, request)
            call.respondResult(result)
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondResult(
    result: Result<com.skladpro.inventory.model.InventoryItem>,
    successStatus: HttpStatusCode = HttpStatusCode.OK
) {
    result.fold(
        onSuccess = { item ->
            respond(successStatus, item.toResponse())
        },
        onFailure = { error ->
            val status = when (error) {
                is NoSuchElementException -> HttpStatusCode.NotFound
                is IllegalArgumentException -> HttpStatusCode.BadRequest
                else -> HttpStatusCode.InternalServerError
            }
            respond(status, ErrorResponse(error.message ?: "Ошибка обработки запроса"))
        }
    )
}
