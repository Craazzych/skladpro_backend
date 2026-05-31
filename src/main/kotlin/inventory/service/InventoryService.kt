package com.skladpro.inventory.service

import com.skladpro.inventory.dto.DeliveryRequest
import com.skladpro.inventory.dto.InventoryItemRequest
import com.skladpro.inventory.model.InventoryItem
import com.skladpro.inventory.repository.InventoryRepository
import java.util.UUID

class InventoryService(
    private val repository: InventoryRepository
) {
    fun getAll(): List<InventoryItem> = repository.getAll()

    fun search(query: String): List<InventoryItem> = repository.search(query)

    fun getById(id: String): InventoryItem? = repository.getById(id)

    fun create(request: InventoryItemRequest): Result<InventoryItem> {
        validateRequest(request)?.let {
            return Result.failure(IllegalArgumentException(it))
        }

        val item = request.toItem(id = UUID.randomUUID().toString())
        return Result.success(repository.create(item))
    }

    fun update(id: String, request: InventoryItemRequest): Result<InventoryItem> {
        validateRequest(request)?.let {
            return Result.failure(IllegalArgumentException(it))
        }

        val item = request.toItem(id)
        val updatedItem = repository.update(item)
            ?: return Result.failure(NoSuchElementException("Товар не найден"))

        return Result.success(updatedItem)
    }

    fun delete(id: String): Boolean = repository.delete(id)

    fun applyStockOperation(id: String, quantityDelta: Double): Result<InventoryItem> {
        if (quantityDelta == 0.0) {
            return Result.failure(IllegalArgumentException("Количество операции не должно быть равно 0"))
        }

        val currentItem = repository.getById(id)
            ?: return Result.failure(NoSuchElementException("Товар не найден"))

        val updatedQuantity = currentItem.quantity + quantityDelta
        if (updatedQuantity < 0.0) {
            return Result.failure(IllegalArgumentException("Остаток не может быть меньше 0"))
        }

        val updatedItem = currentItem.copy(quantity = updatedQuantity)
        return Result.success(repository.update(updatedItem) ?: updatedItem)
    }

    fun updateDelivery(id: String, request: DeliveryRequest): Result<InventoryItem> {
        validateDeliveryDate(request.expectedDeliveryDate)?.let {
            return Result.failure(IllegalArgumentException(it))
        }
        if (request.expectedDeliveryDate != null &&
            (request.expectedDeliveryQuantity == null || request.expectedDeliveryQuantity <= 0.0)
        ) {
            return Result.failure(IllegalArgumentException("Для поставки укажите количество больше 0"))
        }

        val currentItem = repository.getById(id)
            ?: return Result.failure(NoSuchElementException("Товар не найден"))

        val updatedItem = currentItem.copy(
            expectedDeliveryDate = request.expectedDeliveryDate?.trim()?.ifBlank { null },
            expectedDeliveryQuantity = request.expectedDeliveryQuantity
        )

        return Result.success(repository.update(updatedItem) ?: updatedItem)
    }

    private fun validateRequest(request: InventoryItemRequest): String? {
        return when {
            request.name.isBlank() -> "Наименование обязательно"
            request.sku.isBlank() -> "Артикул обязателен"
            request.category.isBlank() -> "Категория обязательна"
            request.unit.isBlank() -> "Единица измерения обязательна"
            request.quantity < 0.0 -> "Остаток не может быть меньше 0"
            request.minQuantity < 0.0 -> "Минимальный остаток не может быть меньше 0"
            request.department.isBlank() -> "Отдел обязателен"
            request.shelf.isBlank() -> "Стеллаж обязателен"
            request.cell.isBlank() -> "Ячейка обязательна"
            validateDeliveryDate(request.expectedDeliveryDate) != null -> validateDeliveryDate(request.expectedDeliveryDate)
            request.expectedDeliveryDate != null &&
                (request.expectedDeliveryQuantity == null || request.expectedDeliveryQuantity <= 0.0) ->
                "Для поставки укажите количество больше 0"
            else -> null
        }
    }

    private fun InventoryItemRequest.toItem(id: String): InventoryItem {
        return InventoryItem(
            id = id,
            name = name.trim(),
            sku = sku.trim(),
            category = category.trim(),
            unit = unit.trim(),
            quantity = quantity,
            minQuantity = minQuantity,
            department = department.trim(),
            shelf = shelf.trim(),
            cell = cell.trim(),
            expectedDeliveryDate = expectedDeliveryDate?.trim()?.ifBlank { null },
            expectedDeliveryQuantity = expectedDeliveryQuantity
        )
    }
}
