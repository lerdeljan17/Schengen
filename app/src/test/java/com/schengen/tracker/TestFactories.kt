package com.schengen.tracker

import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Stay
import java.time.LocalDate

internal fun date(value: String): LocalDate = LocalDate.parse(value)

internal fun stay(
    entryDate: String,
    exitDate: String? = entryDate,
    id: Long = 1L,
    profileId: Long = 1L,
    source: EntrySource = EntrySource.MANUAL,
    note: String = "",
    countries: List<String> = emptyList()
): Stay = Stay(
    id = id,
    profileId = profileId,
    entryDate = date(entryDate),
    exitDate = exitDate?.let(::date),
    source = source,
    note = note,
    countries = countries
)

internal fun plannedTrip(
    entryDate: String,
    exitDate: String,
    id: Long = 1L,
    profileId: Long = 1L,
    note: String = "",
    countries: List<String> = emptyList()
): PlannedTrip = PlannedTrip(
    id = id,
    profileId = profileId,
    entryDate = date(entryDate),
    exitDate = date(exitDate),
    note = note,
    countries = countries
)
