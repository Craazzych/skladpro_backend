package com.skladpro.inventory.repository

import com.skladpro.inventory.model.InventoryItem

interface InventoryRepository {
    fun getAll(): List<InventoryItem>

    fun search(query: String): List<InventoryItem>

    fun getById(id: String): InventoryItem?

    fun create(item: InventoryItem): InventoryItem

    fun update(item: InventoryItem): InventoryItem?

    fun delete(id: String): Boolean
}
