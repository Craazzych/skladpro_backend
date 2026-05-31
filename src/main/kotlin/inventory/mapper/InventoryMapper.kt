package com.skladpro.inventory.mapper

import com.skladpro.inventory.dto.InventoryItemResponse
import com.skladpro.inventory.model.InventoryItem

fun InventoryItem.toResponse(): InventoryItemResponse {
    return InventoryItemResponse(
        id = id,
        name = name,
        sku = sku,
        category = category,
        unit = unit,
        quantity = quantity,
        minQuantity = minQuantity,
        department = department,
        shelf = shelf,
        cell = cell,
        expectedDeliveryDate = expectedDeliveryDate,
        expectedDeliveryQuantity = expectedDeliveryQuantity,
        requiresPurchase = requiresPurchase,
        isLowStock = isLowStock
    )
}
