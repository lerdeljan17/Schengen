package com.schengen.tracker.domain

import java.time.LocalDate
import java.time.YearMonth

class SchengenCalculator {

    /**
     * Computes the number of days physically present in Schengen during
     * the trailing 180-day window ending on [date].
     *
     * - Trips with no exitDate (open / ongoing) are considered to extend up to
     *   [date] (or the open trip's entryDate, whichever is later).
     * - Future trips are included so the calculator can also run projections.
     */
    fun usedDaysOn(date: LocalDate, trips: List<Trip>): Int {
        val windowStart = date.minusDays(179)
        val occupiedDays = mutableSetOf<LocalDate>()

        trips.forEach { trip ->
            val effectiveEnd = trip.exitDate ?: date
            addRange(
                occupiedDays,
                maxOf(trip.entryDate, windowStart),
                minOf(effectiveEnd, date)
            )
        }

        return occupiedDays.size
    }

    fun availableDaysOn(date: LocalDate, trips: List<Trip>): Int =
        (90 - usedDaysOn(date, trips)).coerceIn(0, 90)

    fun rollingWindowStart(date: LocalDate): LocalDate = date.minusDays(179)

    fun rollingWindowEnd(date: LocalDate): LocalDate = date

    fun tripsInWindow(date: LocalDate, trips: List<Trip>): List<Trip> {
        val windowStart = rollingWindowStart(date)
        return trips.filter { trip ->
            val effectiveEnd = trip.exitDate ?: date
            !effectiveEnd.isBefore(windowStart) && !trip.entryDate.isAfter(date)
        }.sortedBy { it.entryDate }
    }

    fun daysCountedInWindow(date: LocalDate, trip: Trip): Int {
        val windowStart = rollingWindowStart(date)
        val effectiveEnd = trip.exitDate ?: date
        val rangeStart = maxOf(trip.entryDate, windowStart)
        val rangeEnd = minOf(effectiveEnd, date)
        if (rangeStart.isAfter(rangeEnd)) return 0
        return rangeStart.until(rangeEnd).days + 1
    }

    /**
     * Days used at [date] counting only what has actually happened by [today].
     * Trips that have not started yet are excluded entirely. Trips that are ongoing
     * (exit date is `null` or in the future) are clipped to [today], so their
     * already-elapsed portion still counts.
     */
    fun usedDaysOnConfirmed(
        date: LocalDate,
        trips: List<Trip>,
        today: LocalDate = LocalDate.now()
    ): Int = usedDaysOn(date, confirmedAsOf(trips, today))

    fun availableDaysOnConfirmed(
        date: LocalDate,
        trips: List<Trip>,
        today: LocalDate = LocalDate.now()
    ): Int = (90 - usedDaysOnConfirmed(date, trips, today)).coerceIn(0, 90)

    /**
     * Returns the date of the latest exit of an upcoming or ongoing trip whose exit
     * is strictly after [today]. Useful for "how many days will I have when my
     * planned trips finish?" projections.
     */
    fun latestPlannedExit(today: LocalDate, trips: List<Trip>): LocalDate? =
        trips.mapNotNull { it.exitDate }.filter { it.isAfter(today) }.maxOrNull()

    private fun confirmedAsOf(trips: List<Trip>, today: LocalDate): List<Trip> =
        trips.filter { !it.entryDate.isAfter(today) }
            .map { trip ->
                if (trip.exitDate != null && trip.exitDate.isAfter(today)) {
                    trip.copy(exitDate = today)
                } else trip
            }

    /**
     * Returns the next date on which the user will have *more* available days than
     * they do at [fromDate], assuming no further trips are taken beyond what is
     * already confirmed by [today] (i.e. ongoing trips are clipped to [today] and
     * purely-future planned trips are excluded). This keeps the answer aligned
     * with the "Days available" number on the home screen, which is also based on
     * confirmed presence. Without this alignment, a large future planned trip
     * (e.g. a month abroad) could artificially push the recovery date months out,
     * because the candidate date's availability would be dragged down by those
     * still-hypothetical days while the baseline was not.
     */
    fun nextDateWithMoreAvailability(
        fromDate: LocalDate,
        trips: List<Trip>,
        today: LocalDate = fromDate
    ): LocalDate? {
        val baseline = availableDaysOnConfirmed(fromDate, trips, today)
        for (offset in 1..3650) {
            val candidate = fromDate.plusDays(offset.toLong())
            if (availableDaysOnConfirmed(candidate, trips, today) > baseline) return candidate
        }
        return null
    }

    fun unlockedDaysInMonth(month: YearMonth, trips: List<Trip>): Map<LocalDate, Int> {
        val unlockedDays = linkedMapOf<LocalDate, Int>()
        var cursor = month.atDay(1)
        val end = month.atEndOfMonth()
        while (!cursor.isAfter(end)) {
            val previousDate = cursor.minusDays(1)
            val unlockedCount = availableDaysOn(cursor, trips) - availableDaysOn(previousDate, trips)
            if (unlockedCount > 0) unlockedDays[cursor] = unlockedCount
            cursor = cursor.plusDays(1)
        }
        return unlockedDays
    }

    fun occupiedDaysInMonth(
        month: YearMonth,
        trips: List<Trip>,
        todayDate: LocalDate = LocalDate.now()
    ): Set<LocalDate> {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        val occupied = mutableSetOf<LocalDate>()
        trips.forEach { trip ->
            val effectiveEnd = trip.exitDate ?: todayDate
            addRange(occupied, maxOf(trip.entryDate, start), minOf(effectiveEnd, end))
        }
        return occupied
    }

    fun firstOverstayDate(today: LocalDate, trips: List<Trip>): LocalDate? {
        if (trips.isEmpty()) return null
        val firstDate = minOf(today, trips.minOf { it.entryDate })
        val lastDate = trips.mapNotNull { it.exitDate }.maxOrNull() ?: today.plusDays(180)

        var cursor = firstDate
        while (!cursor.isAfter(lastDate)) {
            if (usedDaysOn(cursor, trips) > 90) return cursor
            cursor = cursor.plusDays(1)
        }
        return null
    }

    fun nextAlertThreshold(availableDays: Int): Int? {
        val thresholds = listOf(30, 15, 7, 1)
        return thresholds.lastOrNull { availableDays <= it }
    }

    /**
     * Computes a "trip status" for a single [trip]. Used by the trip card status badge.
     * - WITHIN_LIMITS: usedDaysOn at trip's exit (or today if open) is below 80.
     * - CLOSE_TO_LIMIT: between 80 and 90 inclusive.
     * - OVER_LIMIT: above 90.
     */
    fun statusForTrip(
        trip: Trip,
        allTrips: List<Trip>,
        today: LocalDate = LocalDate.now()
    ): TripLimitStatus {
        val evaluateOn = trip.exitDate ?: maxOf(trip.entryDate, today)
        val used = usedDaysOn(evaluateOn, allTrips)
        return when {
            used > 90 -> TripLimitStatus.OVER_LIMIT
            used >= 80 -> TripLimitStatus.CLOSE_TO_LIMIT
            else -> TripLimitStatus.WITHIN_LIMITS
        }
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

enum class TripLimitStatus {
    WITHIN_LIMITS,
    CLOSE_TO_LIMIT,
    OVER_LIMIT
}
