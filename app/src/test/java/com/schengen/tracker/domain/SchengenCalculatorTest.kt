package com.schengen.tracker.domain

import com.schengen.tracker.date
import com.schengen.tracker.plannedTrip
import com.schengen.tracker.stay
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
            stays = listOf(stay("2024-03-01", "2024-03-10"))
        )

        assertEquals(10, used)
    }

    @Test
    fun usedDaysOn_onlyCountsDaysInsideRolling180DayWindow() {
        val used = calculator.usedDaysOn(
            date = date("2024-06-29"),
            stays = listOf(stay("2024-01-01", "2024-01-03"))
        )

        assertEquals(2, used)
    }

    @Test
    fun usedDaysOn_deduplicatesOverlappingConfirmedAndPlannedDays() {
        val used = calculator.usedDaysOn(
            date = date("2024-03-10"),
            stays = listOf(stay("2024-03-01", "2024-03-05")),
            plannedTrips = listOf(plannedTrip("2024-03-04", "2024-03-08"))
        )

        assertEquals(8, used)
    }

    @Test
    fun availableDaysOn_clampsToZeroWhenUsedDaysExceedLimit() {
        val available = calculator.availableDaysOn(
            date = date("2024-04-10"),
            stays = listOf(stay("2024-01-01", "2024-04-10"))
        )

        assertEquals(0, available)
    }

    @Test
    fun nextDateWithMoreAvailability_returnsFirstDateAvailabilityIncreases() {
        val recovery = calculator.nextDateWithMoreAvailability(
            fromDate = date("2024-03-30"),
            stays = listOf(stay("2024-01-01", "2024-03-30"))
        )

        assertEquals(date("2024-06-29"), recovery)
    }

    @Test
    fun unlockedDaysInMonth_reportsDailyAvailabilityIncreases() {
        val unlocked = calculator.unlockedDaysInMonth(
            month = YearMonth.of(2024, 6),
            stays = listOf(stay("2024-01-01", "2024-03-30"))
        )

        assertEquals(mapOf(date("2024-06-29") to 1, date("2024-06-30") to 1), unlocked)
    }

    @Test
    fun occupiedDaysInMonth_usesProvidedTodayForOpenStays() {
        val occupied = calculator.occupiedDaysInMonth(
            month = YearMonth.of(2024, 3),
            stays = listOf(stay(entryDate = "2024-03-28", exitDate = null)),
            todayDate = date("2024-04-02")
        )

        assertEquals(
            setOf(date("2024-03-28"), date("2024-03-29"), date("2024-03-30"), date("2024-03-31")),
            occupied
        )
    }

    @Test
    fun plannedDaysInMonth_limitsTripsToRequestedMonth() {
        val planned = calculator.plannedDaysInMonth(
            month = YearMonth.of(2024, 4),
            plannedTrips = listOf(plannedTrip("2024-03-30", "2024-04-02"))
        )

        assertEquals(setOf(date("2024-04-01"), date("2024-04-02")), planned)
    }

    @Test
    fun firstPlannedOverstayDate_returnsFirstDateOverNinetyDays() {
        val overstay = calculator.firstPlannedOverstayDate(
            today = date("2024-03-30"),
            stays = listOf(stay("2024-01-01", "2024-03-30")),
            plannedTrips = listOf(plannedTrip("2024-03-31", "2024-04-05"))
        )

        assertEquals(date("2024-03-31"), overstay)
    }

    @Test
    fun firstPlannedOverstayDate_returnsNullWithoutPlannedTrips() {
        val overstay = calculator.firstPlannedOverstayDate(
            today = date("2024-03-30"),
            stays = listOf(stay("2024-01-01", "2024-03-30")),
            plannedTrips = emptyList()
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
}
