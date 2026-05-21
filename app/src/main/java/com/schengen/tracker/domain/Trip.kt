package com.schengen.tracker.domain

import com.schengen.tracker.data.EntrySource
import java.time.LocalDate

data class Trip(
    val id: Long,
    val profileId: Long,
    val entryDate: LocalDate,
    val exitDate: LocalDate?,
    val source: EntrySource,
    val note: String,
    val countries: List<String>
) {
    fun classify(today: LocalDate = LocalDate.now()): TripStatus {
        val exit = exitDate
        return when {
            exit == null -> if (!entryDate.isAfter(today)) TripStatus.ONGOING else TripStatus.UPCOMING
            exit.isBefore(today) -> TripStatus.PAST
            entryDate.isAfter(today) -> TripStatus.UPCOMING
            else -> TripStatus.ONGOING
        }
    }

    fun isPast(today: LocalDate = LocalDate.now()): Boolean = classify(today) == TripStatus.PAST
    fun isOngoing(today: LocalDate = LocalDate.now()): Boolean = classify(today) == TripStatus.ONGOING
    fun isUpcoming(today: LocalDate = LocalDate.now()): Boolean = classify(today) == TripStatus.UPCOMING
}

enum class TripStatus {
    PAST,
    ONGOING,
    UPCOMING
}
