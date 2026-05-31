package com.skladpro.inventory.repository

import com.skladpro.inventory.model.InventoryItem

fun seedInventoryItems(): List<InventoryItem> {
    return listOf(
        InventoryItem(
            id = "1",
            name = "Стальные болты М8",
            sku = "BLT-M8-001",
            category = "Крепеж",
            unit = "шт.",
            quantity = 1240.0,
            minQuantity = 300.0,
            department = "Производственный склад",
            shelf = "A-01",
            cell = "A-01-03"
        ),
        InventoryItem(
            id = "2",
            name = "Промышленное масло ISO VG 46",
            sku = "OIL-046-010",
            category = "Расходные материалы",
            unit = "л",
            quantity = 42.0,
            minQuantity = 50.0,
            department = "Расходные материалы",
            shelf = "B-04",
            cell = "B-04-02",
            expectedDeliveryDate = "15.06.2026",
            expectedDeliveryQuantity = 80.0
        ),
        InventoryItem(
            id = "3",
            name = "Подшипник 6204",
            sku = "BRG-6204",
            category = "Комплектующие",
            unit = "шт.",
            quantity = 86.0,
            minQuantity = 20.0,
            department = "Комплектующие",
            shelf = "C-02",
            cell = "C-02-07"
        )
    )
}
