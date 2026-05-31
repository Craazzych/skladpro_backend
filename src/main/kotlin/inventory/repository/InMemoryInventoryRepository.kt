package com.skladpro.inventory.repository

import com.skladpro.inventory.model.InventoryItem

class InMemoryInventoryRepository(
    initialItems: List<InventoryItem> = seedInventoryItems()
) : InventoryRepository {
    private val items = initialItems.toMutableList()

    override fun getAll(): List<InventoryItem> = items.toList()

    override fun search(query: String): List<InventoryItem> {
        val cleanQuery = query.trim()
        return if (cleanQuery.isBlank()) {
            getAll()
        } else {
            items.filter { item ->
                item.name.contains(cleanQuery, ignoreCase = true) ||
                    item.sku.contains(cleanQuery, ignoreCase = true) ||
                    item.category.contains(cleanQuery, ignoreCase = true)
            }
        }
    }

    override fun getById(id: String): InventoryItem? {
        return items.firstOrNull { it.id == id }
    }

    override fun create(item: InventoryItem): InventoryItem {
        items.add(0, item)
        return item
    }

    override fun update(item: InventoryItem): InventoryItem? {
        val index = items.indexOfFirst { it.id == item.id }
        if (index == -1) return null
        items[index] = item
        return item
    }

    override fun delete(id: String): Boolean {
        return items.removeIf { it.id == id }
    }
}
