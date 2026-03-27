package com.schengen.tracker.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schengen.tracker.SchengenApp
import com.schengen.tracker.alerts.AlertScheduler
import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.data.StayRepository
import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.SchengenCalculator
import com.schengen.tracker.domain.Stay
import com.schengen.tracker.location.AutoLocationCheckResult
import com.schengen.tracker.location.LocationAutoTracker
import com.schengen.tracker.location.SchengenCountryCatalog
import com.schengen.tracker.location.LocationTrackingScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class SchengenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StayRepository = (application as SchengenApp).repository
    private val calculator = SchengenCalculator()

    private val _uiState = MutableStateFlow(SchengenUiState())
    val uiState: StateFlow<SchengenUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
            AlertScheduler.schedule(getApplication())
            initializeLocationTrackingState()
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
                val currentState = _uiState.value
                val month = currentState.selectedMonth
                val targetDate = currentState.targetDate
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
                val unlockedDaysByDate = calculator.unlockedDaysInMonth(month, result.stays, result.plannedTrips)
                val availableDaysOnTargetDate = targetDate?.let {
                    calculator.availableDaysOn(it, result.stays)
                }
                val availableDaysOnTargetDateWithPlanned = if (hasPlannedTrips) {
                    targetDate?.let {
                        calculator.availableDaysOn(it, result.stays, result.plannedTrips)
                    }
                } else {
                    null
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
                        unlockedHighlightedDays = unlockedDaysByDate.keys,
                        unlockedDaysByDate = unlockedDaysByDate,
                        firstPlannedOverstayDate = calculator.firstPlannedOverstayDate(
                            today,
                            result.stays,
                            result.plannedTrips
                        ),
                        availableDaysOnTargetDate = availableDaysOnTargetDate,
                        availableDaysOnTargetDateWithPlanned = availableDaysOnTargetDateWithPlanned
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

    fun addManualEntry(date: LocalDate, note: String, countries: List<String>) {
        viewModelScope.launch {
            repository.addManualEntry(date, note, countries)
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

    fun addPlannedTrip(entry: LocalDate, exit: LocalDate, note: String, countries: List<String>) {
        viewModelScope.launch {
            val ok = repository.addPlannedTrip(entry, exit, note, countries)
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

    fun confirmPlannedTrip(
        id: Long,
        entryDate: LocalDate? = null,
        exitDate: LocalDate? = null,
        note: String? = null,
        countries: List<String>? = null
    ) {
        viewModelScope.launch {
            val ok = repository.confirmPlannedTripById(id, entryDate, exitDate, note, countries)
            _uiState.update {
                it.copy(validationMessage = if (ok) null else "Planned trip could not be confirmed.")
            }
        }
    }

    fun updateStay(
        id: Long,
        entryDate: LocalDate,
        exitDate: LocalDate?,
        source: EntrySource,
        note: String,
        countries: List<String>
    ) {
        viewModelScope.launch {
            val ok = repository.updateStay(id, entryDate, exitDate, source, note, countries)
            _uiState.update {
                it.copy(validationMessage = if (ok) null else "Stay has invalid dates.")
            }
        }
    }

    fun updatePlannedTrip(
        id: Long,
        entryDate: LocalDate,
        exitDate: LocalDate,
        note: String,
        countries: List<String>
    ) {
        viewModelScope.launch {
            val ok = repository.updatePlannedTrip(id, entryDate, exitDate, note, countries)
            _uiState.update {
                it.copy(validationMessage = if (ok) null else "Planned trip has invalid dates.")
            }
        }
    }

    fun updateProfile(id: Long, name: String, passportNumber: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(validationMessage = "Profile name is required.") }
            return
        }
        viewModelScope.launch {
            val ok = repository.updateProfile(id, name, passportNumber)
            _uiState.update {
                it.copy(validationMessage = if (ok) null else "Profile could not be updated.")
            }
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfileById(id)
            _uiState.update { it.copy(validationMessage = null) }
        }
    }

    fun previousMonth() {
        val month = _uiState.value.selectedMonth.minusMonths(1)
        _uiState.update {
            val unlockedDaysByDate = calculator.unlockedDaysInMonth(month, it.stays, it.plannedTrips)
            it.copy(
                selectedMonth = month,
                highlightedDays = calculator.occupiedDaysInMonth(month, it.stays),
                plannedHighlightedDays = calculator.plannedDaysInMonth(month, it.plannedTrips),
                unlockedHighlightedDays = unlockedDaysByDate.keys,
                unlockedDaysByDate = unlockedDaysByDate
            )
        }
    }

    fun nextMonth() {
        val month = _uiState.value.selectedMonth.plusMonths(1)
        _uiState.update {
            val unlockedDaysByDate = calculator.unlockedDaysInMonth(month, it.stays, it.plannedTrips)
            it.copy(
                selectedMonth = month,
                highlightedDays = calculator.occupiedDaysInMonth(month, it.stays),
                plannedHighlightedDays = calculator.plannedDaysInMonth(month, it.plannedTrips),
                unlockedHighlightedDays = unlockedDaysByDate.keys,
                unlockedDaysByDate = unlockedDaysByDate
            )
        }
    }

    fun goToCurrentMonth() {
        val month = YearMonth.now()
        _uiState.update {
            val unlockedDaysByDate = calculator.unlockedDaysInMonth(month, it.stays, it.plannedTrips)
            it.copy(
                selectedMonth = month,
                highlightedDays = calculator.occupiedDaysInMonth(month, it.stays),
                plannedHighlightedDays = calculator.plannedDaysInMonth(month, it.plannedTrips),
                unlockedHighlightedDays = unlockedDaysByDate.keys,
                unlockedDaysByDate = unlockedDaysByDate
            )
        }
    }

    fun setTargetDate(date: LocalDate?) {
        _uiState.update {
            it.copy(
                targetDate = date,
                availableDaysOnTargetDate = date?.let { selected ->
                    calculator.availableDaysOn(selected, it.stays)
                },
                availableDaysOnTargetDateWithPlanned = if (it.plannedTrips.isNotEmpty()) {
                    date?.let { selected ->
                        calculator.availableDaysOn(selected, it.stays, it.plannedTrips)
                    }
                } else {
                    null
                }
            )
        }
    }

    fun setLocationTracking(enabled: Boolean) {
        repository.setLocationTrackingEnabled(enabled)
        _uiState.update {
            it.copy(
                locationTrackingEnabled = enabled,
                locationStatusMessage = if (enabled) it.locationStatusMessage else null,
                locationStatusIsError = if (enabled) it.locationStatusIsError else false
            )
        }
        if (enabled) {
            LocationTrackingScheduler.schedule(getApplication())
        } else {
            LocationTrackingScheduler.cancel(getApplication())
        }
    }

    fun runLocationCheckNow() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val tracker = LocationAutoTracker(app, repository)
            val (message, isError) = when (val result = tracker.runCheck()) {
                AutoLocationCheckResult.MissingPermission -> Pair(
                    "Location permission missing. Enable location permission and try again.",
                    true
                )

                AutoLocationCheckResult.LocationUnavailable -> Pair(
                    "Could not get a fresh location. Set emulator location and press Check now again.",
                    true
                )

                AutoLocationCheckResult.CountryUnavailable -> Pair(
                    "Could not resolve country from current location.",
                    true
                )

                is AutoLocationCheckResult.Updated -> {
                    val countryName = SchengenCountryCatalog.nameForCode(result.countryCode)
                    val region = if (result.inSchengen) "Schengen" else "non-Schengen"
                    Pair("Location check complete: $countryName (${result.countryCode}, $region).", false)
                }
            }
            _uiState.update {
                it.copy(
                    locationStatusMessage = message,
                    locationStatusIsError = isError,
                    validationMessage = null
                )
            }
        }
    }

    private suspend fun initializeLocationTrackingState() {
        val context = getApplication<Application>()
        val prefEnabled = if (repository.hasLocationTrackingPreference()) {
            repository.isLocationTrackingEnabled()
        } else {
            false
        }
        val scheduledEnabled = LocationTrackingScheduler.isScheduled(context)
        val enabled = prefEnabled || scheduledEnabled
        repository.setLocationTrackingEnabled(enabled)
        _uiState.update { it.copy(locationTrackingEnabled = enabled) }
        if (enabled) {
            LocationTrackingScheduler.schedule(context)
        } else {
            LocationTrackingScheduler.cancel(context)
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
