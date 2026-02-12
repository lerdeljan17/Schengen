package com.schengen.tracker.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schengen.tracker.SchengenApp
import com.schengen.tracker.alerts.AlertScheduler
import com.schengen.tracker.data.StayRepository
import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.SchengenCalculator
import com.schengen.tracker.domain.Stay
import com.schengen.tracker.location.LocationTrackingScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class SchengenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StayRepository = (application as SchengenApp).repository
    private val calculator = SchengenCalculator()

    private val _uiState = MutableStateFlow(SchengenUiState())
    val uiState: StateFlow<SchengenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
            AlertScheduler.schedule(getApplication())
        }
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.observeProfiles(),
                repository.observeActiveProfileId(),
                repository.observeStaysForActiveProfile(),
                repository.observePlannedTripsForActiveProfile()
            ) { profiles, activeId, stays, plannedTrips ->
                CombinedState(profiles, activeId, stays, plannedTrips)
            }.collect { result ->
                val today = LocalDate.now()
                val month = _uiState.value.selectedMonth
                val hasPlannedTrips = result.plannedTrips.isNotEmpty()
                val simulatedAsOfDate = result.plannedTrips.maxOfOrNull { it.exitDate }?.let { maxOf(today, it) }
                val projectedUsedDaysToday = if (hasPlannedTrips) {
                    calculator.usedDaysOn(today, result.stays, result.plannedTrips)
                } else null
                val projectedAvailableDaysToday = if (hasPlannedTrips) {
                    calculator.availableDaysOn(today, result.stays, result.plannedTrips)
                } else null
                val projectedDeltaDaysToday = projectedAvailableDaysToday?.let { it - calculator.availableDaysOn(today, result.stays) }
                val simulatedUsedDaysAtPlanEnd = simulatedAsOfDate?.let {
                    calculator.usedDaysOn(it, result.stays, result.plannedTrips)
                }
                val simulatedAvailableDaysAtPlanEnd = simulatedAsOfDate?.let {
                    calculator.availableDaysOn(it, result.stays, result.plannedTrips)
                }
                val simulatedDeltaDaysAtPlanEnd = simulatedAsOfDate?.let {
                    calculator.availableDaysOn(it, result.stays, result.plannedTrips) -
                        calculator.availableDaysOn(it, result.stays)
                }
                _uiState.update {
                    it.copy(
                        profiles = result.profiles,
                        activeProfileId = result.activeId,
                        stays = result.stays,
                        plannedTrips = result.plannedTrips,
                        today = today,
                        usedDays = calculator.usedDaysOn(today, result.stays),
                        availableDays = calculator.availableDaysOn(today, result.stays),
                        projectedUsedDaysToday = projectedUsedDaysToday,
                        projectedAvailableDaysToday = projectedAvailableDaysToday,
                        projectedDeltaDaysToday = projectedDeltaDaysToday,
                        simulatedUsedDaysAtPlanEnd = simulatedUsedDaysAtPlanEnd,
                        simulatedAvailableDaysAtPlanEnd = simulatedAvailableDaysAtPlanEnd,
                        simulatedDeltaDaysAtPlanEnd = simulatedDeltaDaysAtPlanEnd,
                        simulatedAsOfDate = simulatedAsOfDate,
                        nextRecoveryDate = calculator.nextDateWithMoreAvailability(today, result.stays),
                        highlightedDays = calculator.occupiedDaysInMonth(month, result.stays),
                        plannedHighlightedDays = calculator.plannedDaysInMonth(month, result.plannedTrips),
                        firstPlannedOverstayDate = calculator.firstPlannedOverstayDate(
                            today,
                            result.stays,
                            result.plannedTrips
                        )
                    )
                }
            }
        }
    }

    fun addProfile(name: String, passportNumber: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(validationMessage = "Profile name is required.") }
            return
        }
        viewModelScope.launch {
            val id = repository.addProfile(name, passportNumber)
            repository.setActiveProfile(id)
            _uiState.update { it.copy(validationMessage = null) }
        }
    }

    fun selectProfile(id: Long) {
        viewModelScope.launch {
            repository.setActiveProfile(id)
            _uiState.update { it.copy(validationMessage = null, importExportMessage = null) }
        }
    }

    fun addManualEntry(date: LocalDate) {
        viewModelScope.launch {
            repository.addManualEntry(date)
            _uiState.update { it.copy(validationMessage = null) }
        }
    }

    fun addManualExit(date: LocalDate) {
        viewModelScope.launch {
            val ok = repository.addManualExit(date)
            _uiState.update {
                it.copy(validationMessage = if (ok) null else "No open entry found or exit date is invalid.")
            }
        }
    }

    fun addPlannedTrip(entry: LocalDate, exit: LocalDate, note: String) {
        viewModelScope.launch {
            val ok = repository.addPlannedTrip(entry, exit, note)
            _uiState.update {
                it.copy(validationMessage = if (ok) null else "Planned trip has invalid dates.")
            }
        }
    }

    fun deleteStay(id: Long) {
        viewModelScope.launch {
            repository.deleteStayById(id)
        }
    }

    fun deletePlannedTrip(id: Long) {
        viewModelScope.launch {
            repository.deletePlannedTripById(id)
        }
    }

    fun previousMonth() {
        val month = _uiState.value.selectedMonth.minusMonths(1)
        _uiState.update {
            it.copy(
                selectedMonth = month,
                highlightedDays = calculator.occupiedDaysInMonth(month, it.stays),
                plannedHighlightedDays = calculator.plannedDaysInMonth(month, it.plannedTrips)
            )
        }
    }

    fun nextMonth() {
        val month = _uiState.value.selectedMonth.plusMonths(1)
        _uiState.update {
            it.copy(
                selectedMonth = month,
                highlightedDays = calculator.occupiedDaysInMonth(month, it.stays),
                plannedHighlightedDays = calculator.plannedDaysInMonth(month, it.plannedTrips)
            )
        }
    }

    fun setLocationTracking(enabled: Boolean) {
        _uiState.update { it.copy(locationTrackingEnabled = enabled) }
        if (enabled) {
            LocationTrackingScheduler.schedule(getApplication())
        } else {
            LocationTrackingScheduler.cancel(getApplication())
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    repository.exportCsv(writer)
                } ?: error("Could not open export destination")
            }.onSuccess {
                _uiState.update { it.copy(importExportMessage = "CSV export completed.") }
            }.onFailure { e ->
                _uiState.update { it.copy(importExportMessage = "CSV export failed: ${e.message}") }
            }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    repository.importCsv(reader)
                } ?: error("Could not open CSV source")
            }.onSuccess { count ->
                _uiState.update { it.copy(importExportMessage = "Imported $count rows from CSV.") }
            }.onFailure { e ->
                _uiState.update { it.copy(importExportMessage = "CSV import failed: ${e.message}") }
            }
        }
    }
}

private data class CombinedState(
    val profiles: List<Profile>,
    val activeId: Long?,
    val stays: List<Stay>,
    val plannedTrips: List<PlannedTrip>
)
