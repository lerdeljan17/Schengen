package com.schengen.tracker.ui.components

import com.schengen.tracker.domain.Trip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

internal val mondayFirstWeekdays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

internal val sundayFirstWeekdays = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

internal fun buildDayCells(month: YearMonth, firstWeekday: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leadingEmpty = (first.dayOfWeek.value - firstWeekday.value + 7) % 7
    val totalDays = month.lengthOfMonth()

    val cells = mutableListOf<LocalDate?>()
    repeat(leadingEmpty) { cells.add(null) }
    for (day in 1..totalDays) {
        cells.add(month.atDay(day))
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

internal fun tripDaysInRange(
    trips: List<Trip>,
    today: LocalDate,
    rangeStart: LocalDate,
    rangeEnd: LocalDate
): Set<LocalDate> {
    val out = mutableSetOf<LocalDate>()
    trips.forEach { trip ->
        val effectiveEnd = trip.exitDate ?: today
        var cursor = maxOf(trip.entryDate, rangeStart)
        val finalEnd = minOf(effectiveEnd, rangeEnd)
        while (!cursor.isAfter(finalEnd)) {
            out.add(cursor)
            cursor = cursor.plusDays(1)
        }
    }
    return out
}
