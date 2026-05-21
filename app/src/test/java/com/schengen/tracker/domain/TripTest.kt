package com.schengen.tracker.domain

import com.schengen.tracker.date
import com.schengen.tracker.trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTest {

    @Test
    fun classify_pastTripWithExitBeforeToday() {
        val past = trip("2024-01-01", "2024-01-05")
        assertEquals(TripStatus.PAST, past.classify(today = date("2024-05-21")))
        assertTrue(past.isPast(today = date("2024-05-21")))
        assertFalse(past.isOngoing(today = date("2024-05-21")))
        assertFalse(past.isUpcoming(today = date("2024-05-21")))
    }

    @Test
    fun classify_ongoingTripWithEntryBeforeOrOnTodayAndExitAfterOrOnToday() {
        val today = date("2024-05-21")
        val ongoing = trip("2024-05-19", "2024-05-28")

        assertEquals(TripStatus.ONGOING, ongoing.classify(today))
        assertTrue(ongoing.isOngoing(today))
    }

    @Test
    fun classify_ongoingTripOnExitDateItself() {
        val today = date("2024-05-28")
        val ongoing = trip("2024-05-19", "2024-05-28")

        // The day a trip exits still counts as ongoing — they're still in Schengen that day.
        assertEquals(TripStatus.ONGOING, ongoing.classify(today))
    }

    @Test
    fun classify_pastTripOneDayAfterExit() {
        val today = date("2024-05-29")
        val pastTrip = trip("2024-05-19", "2024-05-28")

        assertEquals(TripStatus.PAST, pastTrip.classify(today))
    }

    @Test
    fun classify_upcomingTripWithEntryAfterToday() {
        val today = date("2024-05-21")
        val upcoming = trip("2024-06-01", "2024-06-10")

        assertEquals(TripStatus.UPCOMING, upcoming.classify(today))
        assertTrue(upcoming.isUpcoming(today))
    }

    @Test
    fun classify_openTripWithoutExit_isOngoingOnceEntryHasPassed() {
        val today = date("2024-05-21")
        val open = trip(entryDate = "2024-05-15", exitDate = null)

        assertEquals(TripStatus.ONGOING, open.classify(today))
    }

    @Test
    fun classify_openTripBeforeEntry_isUpcoming() {
        val today = date("2024-05-21")
        val openFuture = trip(entryDate = "2024-06-01", exitDate = null)

        assertEquals(TripStatus.UPCOMING, openFuture.classify(today))
    }

    @Test
    fun classify_singleDayTripOnSameDay_isOngoing() {
        val today = date("2024-05-21")
        val sameDay = trip("2024-05-21", "2024-05-21")

        assertEquals(TripStatus.ONGOING, sameDay.classify(today))
    }
}
