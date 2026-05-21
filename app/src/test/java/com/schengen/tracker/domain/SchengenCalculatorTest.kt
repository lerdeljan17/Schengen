package com.schengen.tracker.domain

import com.schengen.tracker.date
import com.schengen.tracker.trip
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SchengenCalculatorTest {
    private val calculator = SchengenCalculator()

    @Test
    fun usedDaysOn_countsEntryAndExitDatesInclusively() {
        val used = calculator.usedDaysOn(
            date = date("2024-03-10"),
            trips = listOf(trip("2024-03-01", "2024-03-10"))
        )

        assertEquals(10, used)
    }

    @Test
    fun usedDaysOn_onlyCountsDaysInsideRolling180DayWindow() {
        val used = calculator.usedDaysOn(
            date = date("2024-06-29"),
            trips = listOf(trip("2024-01-01", "2024-01-03"))
        )

        assertEquals(2, used)
    }

    @Test
    fun usedDaysOn_deduplicatesOverlappingTripDays() {
        val used = calculator.usedDaysOn(
            date = date("2024-03-10"),
            trips = listOf(
                trip("2024-03-01", "2024-03-05", id = 1L),
                trip("2024-03-04", "2024-03-08", id = 2L)
            )
        )

        assertEquals(8, used)
    }

    @Test
    fun availableDaysOn_clampsToZeroWhenUsedDaysExceedLimit() {
        val available = calculator.availableDaysOn(
            date = date("2024-04-10"),
            trips = listOf(trip("2024-01-01", "2024-04-10"))
        )

        assertEquals(0, available)
    }

    @Test
    fun nextDateWithMoreAvailability_returnsFirstDateAvailabilityIncreases() {
        val recovery = calculator.nextDateWithMoreAvailability(
            fromDate = date("2024-03-30"),
            trips = listOf(trip("2024-01-01", "2024-03-30"))
        )

        assertEquals(date("2024-06-29"), recovery)
    }

    @Test
    fun unlockedDaysInMonth_reportsDailyAvailabilityIncreases() {
        val unlocked = calculator.unlockedDaysInMonth(
            month = YearMonth.of(2024, 6),
            trips = listOf(trip("2024-01-01", "2024-03-30"))
        )

        assertEquals(mapOf(date("2024-06-29") to 1, date("2024-06-30") to 1), unlocked)
    }

    @Test
    fun occupiedDaysInMonth_usesProvidedTodayForOpenTrips() {
        val occupied = calculator.occupiedDaysInMonth(
            month = YearMonth.of(2024, 3),
            trips = listOf(trip(entryDate = "2024-03-28", exitDate = null)),
            todayDate = date("2024-04-02")
        )

        assertEquals(
            setOf(date("2024-03-28"), date("2024-03-29"), date("2024-03-30"), date("2024-03-31")),
            occupied
        )
    }

    @Test
    fun firstOverstayDate_returnsFirstDateOverNinetyDays() {
        val overstay = calculator.firstOverstayDate(
            today = date("2024-03-30"),
            trips = listOf(
                trip("2024-01-01", "2024-03-30", id = 1L),
                trip("2024-03-31", "2024-04-05", id = 2L)
            )
        )

        assertEquals(date("2024-03-31"), overstay)
    }

    @Test
    fun firstOverstayDate_returnsNullWhenNoTrips() {
        val overstay = calculator.firstOverstayDate(
            today = date("2024-03-30"),
            trips = emptyList()
        )

        assertNull(overstay)
    }

    @Test
    fun nextAlertThreshold_returnsSpecificCrossedThreshold() {
        assertNull(calculator.nextAlertThreshold(31))
        assertEquals(30, calculator.nextAlertThreshold(30))
        assertEquals(15, calculator.nextAlertThreshold(15))
        assertEquals(7, calculator.nextAlertThreshold(7))
        assertEquals(1, calculator.nextAlertThreshold(1))
        assertEquals(1, calculator.nextAlertThreshold(0))
    }

    @Test
    fun usedDaysOnConfirmed_includesOngoingTripsClippedToToday() {
        val ongoingTrip = trip("2024-05-19", "2024-05-28", id = 1L)
        val today = date("2024-05-21")

        val used = SchengenCalculator().usedDaysOnConfirmed(
            date = today,
            trips = listOf(ongoingTrip),
            today = today
        )

        assertEquals(3, used)
    }

    @Test
    fun availableDaysOnConfirmed_returnsEightySevenForSwedenScenario() {
        // Regression: home screen hero used to read 90 (with 0 used) for an ongoing trip
        // because the previous implementation excluded the trip entirely when its exit
        // was in the future. It should show 87 days available with 3 days used.
        val ongoingTrip = trip("2024-05-19", "2024-05-28", id = 1L)
        val today = date("2024-05-21")

        val available = SchengenCalculator().availableDaysOnConfirmed(
            date = today,
            trips = listOf(ongoingTrip),
            today = today
        )

        assertEquals(87, available)
    }

    @Test
    fun usedDaysOnConfirmed_clipsOpenEndedTripToToday() {
        val today = date("2024-05-21")
        val openTrip = trip(entryDate = "2024-05-19", exitDate = null)

        val used = SchengenCalculator().usedDaysOnConfirmed(
            date = today,
            trips = listOf(openTrip),
            today = today
        )

        assertEquals(3, used)
    }

    @Test
    fun availableDaysOn_naturallyClipsOngoingTripsForTodayQuery() {
        // Without going through the "confirmed" wrapper, availableDaysOn already clips
        // the effective end of an ongoing trip to the queried date.
        val ongoingTrip = trip("2024-05-19", "2024-05-28", id = 1L)
        val today = date("2024-05-21")

        val available = SchengenCalculator().availableDaysOn(today, listOf(ongoingTrip))

        assertEquals(87, available)
    }

    @Test
    fun usedDaysOnConfirmed_excludesPurelyFutureTrips() {
        val futureTrip = trip("2024-06-10", "2024-06-15", id = 1L)
        val today = date("2024-05-21")

        val used = SchengenCalculator().usedDaysOnConfirmed(
            date = today,
            trips = listOf(futureTrip),
            today = today
        )

        assertEquals(0, used)
    }

    @Test
    fun latestPlannedExit_returnsFurthestFutureExit() {
        val today = date("2024-05-21")
        val trips = listOf(
            trip("2024-05-19", "2024-05-28", id = 1L),
            trip("2024-07-10", "2024-07-20", id = 2L),
            trip("2024-01-01", "2024-01-05", id = 3L)
        )

        val latest = SchengenCalculator().latestPlannedExit(today, trips)

        assertEquals(date("2024-07-20"), latest)
    }

    @Test
    fun latestPlannedExit_returnsNullWithoutFutureExits() {
        val today = date("2024-05-21")
        val latest = SchengenCalculator().latestPlannedExit(
            today = today,
            trips = listOf(trip("2024-01-01", "2024-01-05", id = 1L))
        )

        assertEquals(null, latest)
    }

    @Test
    fun statusForTrip_reflectsUsageAtTripEndDate() {
        val withinLimits = calculator.statusForTrip(
            trip = trip("2024-04-01", "2024-04-10", id = 1L),
            allTrips = listOf(trip("2024-04-01", "2024-04-10", id = 1L))
        )
        assertEquals(TripLimitStatus.WITHIN_LIMITS, withinLimits)

        val overLimit = calculator.statusForTrip(
            trip = trip("2024-03-25", "2024-04-25", id = 2L),
            allTrips = listOf(
                trip("2024-01-01", "2024-03-25", id = 3L),
                trip("2024-03-25", "2024-04-25", id = 2L)
            )
        )
        assertEquals(TripLimitStatus.OVER_LIMIT, overLimit)
    }
}
