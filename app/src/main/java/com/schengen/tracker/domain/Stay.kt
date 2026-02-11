package com.schengen.tracker.domain

import com.schengen.tracker.data.EntrySource
import java.time.LocalDate

data class Stay(
    val id: Long,
    val profileId: Long,
    val entryDate: LocalDate,
    val exitDate: LocalDate?,
    val source: EntrySource
)
