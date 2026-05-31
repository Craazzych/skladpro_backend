package com.skladpro.inventory.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

private val DeliveryDateFormatter = DateTimeFormatter
    .ofPattern("dd.MM.uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

fun validateDeliveryDate(dateText: String?): String? {
    val cleanDate = dateText?.trim().orEmpty()
    if (cleanDate.isBlank()) return null

    val parsedDate = try {
        LocalDate.parse(cleanDate, DeliveryDateFormatter)
    } catch (_: DateTimeParseException) {
        return "Дата должна существовать и быть не раньше сегодняшней"
    }

    return if (parsedDate.isBefore(LocalDate.now())) {
        "Дата поставки не может быть раньше сегодняшней"
    } else {
        null
    }
}
