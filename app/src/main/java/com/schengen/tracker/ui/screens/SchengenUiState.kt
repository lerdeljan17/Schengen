package com.schengen.tracker.ui.screens

import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.Stay
import java.time.LocalDate
import java.time.YearMonth

data class SchengenUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long? = null,
    val stays: List<Stay> = emptyList(),
    val plannedTrips: List<PlannedTrip> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val usedDays: Int = 0,
    val availableDays: Int = 90,
    val nextRecoveryDate: LocalDate? = null,
    val selectedMonth: YearMonth = YearMonth.now(),
    val highlightedDays: Set<LocalDate> = emptySet(),
    val plannedHighlightedDays: Set<LocalDate> = emptySet(),
    val firstPlannedOverstayDate: LocalDate? = null,
    val locationTrackingEnabled: Boolean = false,
    val validationMessage: String? = null,
    val importExportMessage: String? = null
)
