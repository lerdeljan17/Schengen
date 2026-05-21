package com.schengen.tracker.ui.screens

import com.schengen.tracker.domain.Trip
import com.schengen.tracker.location.SchengenCountryCatalog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TextHelpers {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun parseIsoDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.trim()) }.getOrNull()

    fun formatCountryNames(countryCodes: List<String>): String =
        SchengenCountryCatalog.displayNames(countryCodes).joinToString()

    fun toggleCountrySelection(selectedCountryCodes: List<String>, code: String): List<String> =
        if (code in selectedCountryCodes) {
            selectedCountryCodes.filterNot { it == code }
        } else {
            selectedCountryCodes + code
        }

    fun formatDayCount(value: Int): String =
        if (value == 1) "1 day" else "$value days"

    fun inclusiveDayCount(start: LocalDate, end: LocalDate): Int =
        ChronoUnit.DAYS.between(start, end).toInt() + 1

    fun tripDuration(trip: Trip, today: LocalDate = LocalDate.now()): String {
        val end = trip.exitDate ?: today
        return formatDayCount(inclusiveDayCount(trip.entryDate, maxOf(trip.entryDate, end)))
    }

    fun tripLabel(trip: Trip): String {
        val baseName = trip.countries
            .firstOrNull()
            ?.let(SchengenCountryCatalog::nameForCode)
        val note = trip.note.trim().takeIf { it.isNotBlank() }
        return when {
            note != null && baseName != null -> note
            note != null -> note
            baseName != null -> baseName
            else -> "Trip"
        }
    }
}
