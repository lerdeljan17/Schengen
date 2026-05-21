package com.schengen.tracker

import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.domain.Trip
import java.time.LocalDate

internal fun date(value: String): LocalDate = LocalDate.parse(value)

internal fun trip(
    entryDate: String,
    exitDate: String? = entryDate,
    id: Long = 1L,
    profileId: Long = 1L,
    source: EntrySource = EntrySource.MANUAL,
    note: String = "",
    countries: List<String> = emptyList()
): Trip = Trip(
    id = id,
    profileId = profileId,
    entryDate = date(entryDate),
    exitDate = exitDate?.let(::date),
    source = source,
    note = note,
    countries = countries
)
