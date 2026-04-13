package com.schengen.tracker.ui.screens

import com.schengen.tracker.date
import com.schengen.tracker.stay
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SchengenScreenTextTest {
    private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    @Test
    fun parseIsoDate_acceptsTrimmedIsoDateAndRejectsInvalidInput() {
        assertEquals(date("2024-03-10"), SchengenScreenText.parseIsoDate(" 2024-03-10 "))
        assertNull(SchengenScreenText.parseIsoDate("10/03/2024"))
        assertNull(SchengenScreenText.parseIsoDate(""))
    }

    @Test
    fun durationSummariesCountDaysInclusively() {
        assertEquals(
            "Duration: 1 day",
            SchengenScreenText.plannedTripDurationSummary(date("2024-03-10"), date("2024-03-10"))
        )
        assertEquals(
            "Duration: 3 days",
            SchengenScreenText.plannedTripDurationSummary(date("2024-03-10"), date("2024-03-12"))
        )
        assertEquals(
            "Days so far: 4 days",
            SchengenScreenText.stayDurationSummary(
                entry = date("2024-03-10"),
                exit = null,
                hasExitDate = false,
                todayDate = date("2024-03-13")
            )
        )
        assertEquals(
            "Duration: 2 days",
            SchengenScreenText.stayDurationSummary(stay("2024-03-10", "2024-03-11"))
        )
    }

    @Test
    fun durationSummariesReturnNullForIncompleteOrInvalidDates() {
        assertNull(SchengenScreenText.stayDurationSummary(null, null, hasExitDate = true))
        assertNull(SchengenScreenText.stayDurationSummary(date("2024-03-12"), date("2024-03-10"), true))
        assertNull(SchengenScreenText.stayDurationSummary(date("2024-03-12"), null, true))
        assertNull(SchengenScreenText.plannedTripDurationSummary(date("2024-03-12"), date("2024-03-10")))
    }

    @Test
    fun countrySelectionTogglesCodesWithoutReorderingOtherSelections() {
        assertEquals(
            listOf("DE", "FR", "ES"),
            SchengenScreenText.toggleCountrySelection(listOf("DE", "FR"), "ES")
        )
        assertEquals(
            listOf("DE", "ES"),
            SchengenScreenText.toggleCountrySelection(listOf("DE", "FR", "ES"), "FR")
        )
    }

    @Test
    fun formatCountryNamesNormalizesKnownCodesAndNames() {
        assertEquals("Germany, France", SchengenScreenText.formatCountryNames(listOf("de", "France")))
    }

    @Test
    fun summarizeUnlockPeriodsGroupsConsecutiveDates() {
        val periods = SchengenScreenText.summarizeUnlockPeriods(
            mapOf(
                date("2024-06-29") to 1,
                date("2024-06-30") to 2,
                date("2024-07-02") to 1
            )
        )

        assertEquals(
            listOf(
                UnlockPeriod(date("2024-06-29"), date("2024-06-30"), 3),
                UnlockPeriod(date("2024-07-02"), date("2024-07-02"), 1)
            ),
            periods
        )
        assertEquals("29 Jun 2024 - 30 Jun 2024: 3 days", SchengenScreenText.formatUnlockPeriod(periods[0], formatter))
        assertEquals("02 Jul 2024: 1 day", SchengenScreenText.formatUnlockPeriod(periods[1], formatter))
    }

    @Test
    fun formatDayCountAndDeltaUseReadableLabels() {
        assertEquals("1 day", SchengenScreenText.formatDayCount(1))
        assertEquals("2 days", SchengenScreenText.formatDayCount(2))
        assertEquals("+3 days", SchengenScreenText.formatDelta(3))
        assertEquals("-2 days", SchengenScreenText.formatDelta(-2))
        assertEquals("0 days", SchengenScreenText.formatDelta(0))
    }
}
