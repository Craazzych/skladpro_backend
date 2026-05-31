package com.skladpro.inventory.model

import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
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
    val expectedDeliveryQuantity: Double? = null
) {
    val requiresPurchase: Boolean
        get() = quantity < minQuantity

    val isLowStock: Boolean
        get() = quantity <= minQuantity
}
