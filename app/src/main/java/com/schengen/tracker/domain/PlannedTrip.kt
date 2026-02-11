package com.schengen.tracker.domain

import java.time.LocalDate

data class PlannedTrip(
    val id: Long,
    val profileId: Long,
    val entryDate: LocalDate,
    val exitDate: LocalDate,
    val note: String
)
