package com.skladpro.inventory.dto

import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemResponse(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val quantity: Double,
    val minQuantity: Double,
    val department: String,
    val shelf: String,
    val cell: String,
    val expectedDeliveryDate: String? = null,
    val expectedDeliveryQuantity: Double? = null,
    val requiresPurchase: Boolean,
    val isLowStock: Boolean
)

@Serializable
data class InventoryItemRequest(
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val quantity: Double,
    val minQuantity: Double,
    val department: String,
    val shelf: String,
    val cell: String,
    val expectedDeliveryDate: String? = null,
    val expectedDeliveryQuantity: Double? = null
)

@Serializable
data class StockOperationRequest(
    val quantityDelta: Double
)

@Serializable
data class DeliveryRequest(
    val expectedDeliveryDate: String? = null,
    val expectedDeliveryQuantity: Double? = null
)

@Serializable
data class ErrorResponse(
    val message: String
)
