package com.schengen.tracker.domain

import java.time.LocalDate
import java.time.YearMonth

class SchengenCalculator {
    fun usedDaysOn(
        date: LocalDate,
        stays: List<Stay>,
        plannedTrips: List<PlannedTrip> = emptyList()
    ): Int {
        val windowStart = date.minusDays(179)
        val occupiedDays = mutableSetOf<LocalDate>()

        stays.forEach { stay ->
            val stayEnd = stay.exitDate ?: date
            addRange(
                occupiedDays,
                maxOf(stay.entryDate, windowStart),
                minOf(stayEnd, date)
            )
        }

        plannedTrips.forEach { trip ->
            addRange(
                occupiedDays,
                maxOf(trip.entryDate, windowStart),
                minOf(trip.exitDate, date)
            )
        }

        return occupiedDays.size
    }

    fun availableDaysOn(
        date: LocalDate,
        stays: List<Stay>,
        plannedTrips: List<PlannedTrip> = emptyList()
    ): Int {
        return (90 - usedDaysOn(date, stays, plannedTrips)).coerceIn(0, 90)
    }

    fun nextDateWithMoreAvailability(
        fromDate: LocalDate,
        stays: List<Stay>,
        plannedTrips: List<PlannedTrip> = emptyList()
    ): LocalDate? {
        val baseline = availableDaysOn(fromDate, stays, plannedTrips)
        for (offset in 1..3650) {
            val candidate = fromDate.plusDays(offset.toLong())
            if (availableDaysOn(candidate, stays, plannedTrips) > baseline) {
                return candidate
            }
        }
        return null
    }

    fun occupiedDaysInMonth(month: YearMonth, stays: List<Stay>): Set<LocalDate> {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val occupied = mutableSetOf<LocalDate>()

        stays.forEach { stay ->
            val stayEnd = stay.exitDate ?: LocalDate.now()
            addRange(occupied, maxOf(stay.entryDate, start), minOf(stayEnd, end))
        }
        return occupied
    }

    fun plannedDaysInMonth(month: YearMonth, plannedTrips: List<PlannedTrip>): Set<LocalDate> {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val occupied = mutableSetOf<LocalDate>()

        plannedTrips.forEach { trip ->
            addRange(occupied, maxOf(trip.entryDate, start), minOf(trip.exitDate, end))
        }
        return occupied
    }

    fun firstPlannedOverstayDate(
        today: LocalDate,
        stays: List<Stay>,
        plannedTrips: List<PlannedTrip>
    ): LocalDate? {
        if (plannedTrips.isEmpty()) return null
        val firstDate = minOf(today, plannedTrips.minOf { it.entryDate })
        val lastDate = plannedTrips.maxOf { it.exitDate }

        var cursor = firstDate
        while (!cursor.isAfter(lastDate)) {
            if (usedDaysOn(cursor, stays, plannedTrips) > 90) {
                return cursor
            }
            cursor = cursor.plusDays(1)
        }
        return null
    }

    fun nextAlertThreshold(availableDays: Int): Int? {
        val thresholds = listOf(30, 15, 7, 1)
        return thresholds.firstOrNull { availableDays <= it }
    }

    private fun addRange(days: MutableSet<LocalDate>, start: LocalDate, end: LocalDate) {
        if (start.isAfter(end)) return
        var cursor = start
        while (!cursor.isAfter(end)) {
            days.add(cursor)
            cursor = cursor.plusDays(1)
        }
    }
}
