package com.skladpro.inventory.repository

import com.skladpro.inventory.model.InventoryItem
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet

class PostgresInventoryRepository(
    private val dataSource: HikariDataSource
) : InventoryRepository {
    init {
        createSchema()
        seedIfEmpty()
    }

    override fun getAll(): List<InventoryItem> {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                select * from inventory_items
                order by created_at desc, name asc
                """.trimIndent()
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    return resultSet.toInventoryItems()
                }
            }
        }
    }

    override fun search(query: String): List<InventoryItem> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return getAll()

        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                select * from inventory_items
                where lower(name) like lower(?)
                   or lower(sku) like lower(?)
                   or lower(category) like lower(?)
                order by created_at desc, name asc
                """.trimIndent()
            ).use { statement ->
                val pattern = "%$cleanQuery%"
                statement.setString(1, pattern)
                statement.setString(2, pattern)
                statement.setString(3, pattern)
                statement.executeQuery().use { resultSet ->
                    return resultSet.toInventoryItems()
                }
            }
        }
    }

    override fun getById(id: String): InventoryItem? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "select * from inventory_items where id = ?"
            ).use { statement ->
                statement.setString(1, id)
                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) resultSet.toInventoryItem() else null
                }
            }
        }
    }

    override fun create(item: InventoryItem): InventoryItem {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                insert into inventory_items (
                    id, name, sku, category, unit, quantity, min_quantity,
                    department, shelf, cell, expected_delivery_date,
                    expected_delivery_quantity
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.bindItem(item)
                statement.executeUpdate()
            }
        }
        return item
    }

    override fun update(item: InventoryItem): InventoryItem? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                update inventory_items
                set name = ?,
                    sku = ?,
                    category = ?,
                    unit = ?,
                    quantity = ?,
                    min_quantity = ?,
                    department = ?,
                    shelf = ?,
                    cell = ?,
                    expected_delivery_date = ?,
                    expected_delivery_quantity = ?,
                    updated_at = current_timestamp
                where id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, item.name)
                statement.setString(2, item.sku)
                statement.setString(3, item.category)
                statement.setString(4, item.unit)
                statement.setDouble(5, item.quantity)
                statement.setDouble(6, item.minQuantity)
                statement.setString(7, item.department)
                statement.setString(8, item.shelf)
                statement.setString(9, item.cell)
                statement.setString(10, item.expectedDeliveryDate)
                statement.setObject(11, item.expectedDeliveryQuantity)
                statement.setString(12, item.id)
                return if (statement.executeUpdate() == 1) item else null
            }
        }
    }

    override fun delete(id: String): Boolean {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "delete from inventory_items where id = ?"
            ).use { statement ->
                statement.setString(1, id)
                return statement.executeUpdate() == 1
            }
        }
    }

    private fun createSchema() {
        val schema = javaClass.classLoader
            .getResource("db/schema.sql")
            ?.readText()
            ?: error("db/schema.sql not found")

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(schema)
            }
        }
    }

    private fun seedIfEmpty() {
        if (getAll().isNotEmpty()) return
        seedInventoryItems().forEach(::create)
    }

    private fun ResultSet.toInventoryItems(): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        while (next()) {
            items.add(toInventoryItem())
        }
        return items
    }

    private fun ResultSet.toInventoryItem(): InventoryItem {
        return InventoryItem(
            id = getString("id"),
            name = getString("name"),
            sku = getString("sku"),
            category = getString("category"),
            unit = getString("unit"),
            quantity = getDouble("quantity"),
            minQuantity = getDouble("min_quantity"),
            department = getString("department"),
            shelf = getString("shelf"),
            cell = getString("cell"),
            expectedDeliveryDate = getString("expected_delivery_date"),
            expectedDeliveryQuantity = getDoubleOrNull("expected_delivery_quantity")
        )
    }

    private fun java.sql.PreparedStatement.bindItem(item: InventoryItem) {
        setString(1, item.id)
        setString(2, item.name)
        setString(3, item.sku)
        setString(4, item.category)
        setString(5, item.unit)
        setDouble(6, item.quantity)
        setDouble(7, item.minQuantity)
        setString(8, item.department)
        setString(9, item.shelf)
        setString(10, item.cell)
        setString(11, item.expectedDeliveryDate)
        setObject(12, item.expectedDeliveryQuantity)
    }

    private fun ResultSet.getDoubleOrNull(column: String): Double? {
        val value = getDouble(column)
        return if (wasNull()) null else value
    }

    companion object {
        fun create(
            jdbcUrl: String,
            user: String,
            password: String,
            maximumPoolSize: Int
        ): PostgresInventoryRepository {
            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                username = user
                this.password = password
                this.maximumPoolSize = maximumPoolSize
            }
            return PostgresInventoryRepository(HikariDataSource(config))
        }
    }
}
