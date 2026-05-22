package com.schengen.tracker.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schengen.tracker.SchengenApp
import com.schengen.tracker.alerts.AlertScheduler
import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.data.TripRepository
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.SchengenCalculator
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.location.AutoLocationCheckResult
import com.schengen.tracker.location.LocationAutoTracker
import com.schengen.tracker.location.LocationTrackingScheduler
import com.schengen.tracker.location.SchengenCountryCatalog
import com.schengen.tracker.sync.DriveBackupService
import com.schengen.tracker.sync.GoogleSignInManager
import com.schengen.tracker.sync.SignInResult
import com.schengen.tracker.sync.SyncResult
import com.schengen.tracker.ui.theme.AppearanceMode
import com.schengen.tracker.ui.theme.ThemePreferences
import com.schengen.tracker.ui.theme.ThemeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long? = null,
    val trips: List<Trip> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val locationTrackingEnabled: Boolean = false,
    val locationStatusMessage: String? = null,
    val locationStatusIsError: Boolean = false,
    val overstayAlertsEnabled: Boolean = true,
    val transientMessage: String? = null,
    val syncAccountEmail: String? = null,
    val syncAccountName: String? = null,
    val lastSyncAt: Long = 0L,
    val syncInProgress: Boolean = false,
    val syncErrorMessage: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SchengenApp
    val repository: TripRepository = app.repository
    val themePreferences: ThemePreferences = app.themePreferences
    val signInManager: GoogleSignInManager = app.googleSignInManager
    val driveBackupService: DriveBackupService = app.driveBackupService
    val calculator = SchengenCalculator()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val themeState: StateFlow<ThemeState> = themePreferences.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, themePreferences.current)

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
            initializeLocationTrackingState()
            initializeAlerts()
            initializeSync()
        }
        observe()
        observeSignIn()
    }

    private suspend fun initializeSync() {
        signInManager.refresh()
        _uiState.update { it.copy(lastSyncAt = driveBackupService.lastSyncAt()) }
    }

    private fun observeSignIn() {
        viewModelScope.launch {
            signInManager.account.collect { account ->
                _uiState.update {
                    it.copy(
                        syncAccountEmail = account?.email,
                        syncAccountName = account?.displayName
                    )
                }
            }
        }
    }

    private fun observe() {
        viewModelScope.launch {
            combine(
                repository.observeProfiles(),
                repository.observeActiveProfileId(),
                repository.observeTripsForActiveProfile()
            ) { profiles, activeId, trips ->
                Triple(profiles, activeId, trips)
            }.collect { (profiles, activeId, trips) ->
                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        activeProfileId = activeId,
                        trips = trips,
                        today = LocalDate.now()
                    )
                }
            }
        }
    }

    private suspend fun initializeLocationTrackingState() {
        val context = app
        val prefEnabled = if (repository.hasLocationTrackingPreference()) {
            repository.isLocationTrackingEnabled()
        } else false
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

    private fun initializeAlerts() {
        val enabled = repository.isOverstayAlertsEnabled()
        _uiState.update { it.copy(overstayAlertsEnabled = enabled) }
        if (enabled) {
            AlertScheduler.schedule(app)
        } else {
            AlertScheduler.cancel(app)
        }
    }

    fun addTrip(
        entryDate: LocalDate,
        exitDate: LocalDate?,
        source: EntrySource = EntrySource.MANUAL,
        note: String = "",
        countries: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val ok = repository.addTrip(entryDate, exitDate, source, note, countries)
            if (!ok) reportMessage("Trip has invalid dates.")
        }
    }

    fun updateTrip(
        id: Long,
        entryDate: LocalDate,
        exitDate: LocalDate?,
        source: EntrySource,
        note: String,
        countries: List<String>
    ) {
        viewModelScope.launch {
            val ok = repository.updateTrip(id, entryDate, exitDate, source, note, countries)
            if (!ok) reportMessage("Trip has invalid dates.")
        }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { repository.deleteTripById(id) }
    }

    fun addProfile(name: String, passportNumber: String) {
        if (name.isBlank()) {
            reportMessage("Profile name is required.")
            return
        }
        viewModelScope.launch {
            val id = repository.addProfile(name, passportNumber)
            repository.setActiveProfile(id)
        }
    }

    fun selectProfile(id: Long) {
        viewModelScope.launch { repository.setActiveProfile(id) }
    }

    fun updateProfile(id: Long, name: String, passportNumber: String) {
        if (name.isBlank()) {
            reportMessage("Profile name is required.")
            return
        }
        viewModelScope.launch { repository.updateProfile(id, name, passportNumber) }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch { repository.deleteProfileById(id) }
    }

    fun deleteAllTrips(allProfiles: Boolean) {
        viewModelScope.launch {
            if (allProfiles) repository.deleteAllTripsAllProfiles()
            else repository.deleteAllTripsForActiveProfile()
            reportMessage("All trips deleted.")
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
        if (enabled) LocationTrackingScheduler.schedule(app) else LocationTrackingScheduler.cancel(app)
    }

    fun setOverstayAlertsEnabled(enabled: Boolean) {
        repository.setOverstayAlertsEnabled(enabled)
        _uiState.update { it.copy(overstayAlertsEnabled = enabled) }
        if (enabled) AlertScheduler.schedule(app) else AlertScheduler.cancel(app)
    }

    fun runLocationCheckNow() {
        viewModelScope.launch {
            val tracker = LocationAutoTracker(app, repository)
            val (message, isError) = when (val result = tracker.runCheck()) {
                AutoLocationCheckResult.MissingPermission -> Pair(
                    "Location permission missing. Enable location permission and try again.",
                    true
                )

                AutoLocationCheckResult.LocationUnavailable -> Pair(
                    "Could not get a fresh location. Try again in a moment.",
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
                it.copy(locationStatusMessage = message, locationStatusIsError = isError)
            }
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        themePreferences.setAppearanceMode(mode)
    }

    fun setColorTheme(id: String) {
        themePreferences.setColorTheme(id)
    }

    fun setStartWeekOnSunday(enabled: Boolean) {
        themePreferences.setStartWeekOnSunday(enabled)
    }

    fun clearTransientMessage() {
        _uiState.update { it.copy(transientMessage = null) }
    }

    private fun reportMessage(message: String) {
        _uiState.update { it.copy(transientMessage = message) }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val resolver = app.contentResolver
                resolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    repository.exportCsv(writer)
                } ?: error("Could not open backup destination")
            }.onSuccess { reportMessage("Backup saved.") }
                .onFailure { reportMessage("Backup failed: ${it.message}") }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val resolver = app.contentResolver
                resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    repository.importCsv(reader)
                } ?: error("Could not open backup source")
            }.onSuccess { count -> reportMessage("Imported $count rows from backup.") }
                .onFailure { reportMessage("Restore failed: ${it.message}") }
        }
    }

    fun signInIntent(): Intent = signInManager.signInIntent()

    fun handleSignInResult(data: Intent?) {
        when (val result = signInManager.handleSignInResult(data)) {
            is SignInResult.Success -> {
                _uiState.update { it.copy(syncErrorMessage = null) }
            }

            SignInResult.Cancelled -> {
                _uiState.update { it.copy(syncErrorMessage = "Sign-in cancelled.") }
            }

            is SignInResult.Failure -> {
                _uiState.update {
                    it.copy(syncErrorMessage = signInErrorHint(result.statusCode, result.message))
                }
            }
        }
    }

    private fun signInErrorHint(statusCode: Int, message: String): String {
        // See com.google.android.gms.common.api.CommonStatusCodes /
        // GoogleSignInStatusCodes for the constants referenced below.
        val hint = when (statusCode) {
            10 -> "DEVELOPER_ERROR (10): the SHA-1 of the signing key for this APK isn't registered against an Android OAuth client for package com.schengen.tracker in Google Cloud Console, or the Google Drive API isn't enabled in that Cloud project."
            7 -> "NETWORK_ERROR (7): no network connection. Check Wi-Fi / data and try again."
            8 -> "INTERNAL_ERROR (8): Google Play Services hit an internal error. Try again, or update Play Services."
            4 -> "SIGN_IN_REQUIRED (4): no eligible Google account on this device, or the previous session expired."
            12500 -> "SIGN_IN_FAILED (12500): generic Google Sign-In failure. Most often this means the SHA-1 / package / OAuth client setup in Google Cloud Console doesn't match this APK."
            12502 -> "SIGN_IN_CURRENTLY_IN_PROGRESS (12502): another sign-in attempt is already running. Wait a moment and retry."
            else -> null
        }
        return if (hint != null) "Sign-in failed: $hint" else "Sign-in failed (code $statusCode): $message"
    }

    fun signOut() {
        signInManager.signOut {
            _uiState.update {
                it.copy(syncAccountEmail = null, syncAccountName = null, syncErrorMessage = null)
            }
        }
    }

    fun syncNow() {
        val account = signInManager.account.value
        if (account == null) {
            _uiState.update { it.copy(syncErrorMessage = "Sign in to Google before syncing.") }
            return
        }
        if (!signInManager.hasDriveScope(account)) {
            _uiState.update { it.copy(syncErrorMessage = "Drive permission missing. Sign in again to grant Drive AppData access.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(syncInProgress = true, syncErrorMessage = null) }
            val result: SyncResult = driveBackupService.backup(account)
            handleSyncResult(result, "Backup uploaded to Drive.")
        }
    }

    fun restoreFromDrive() {
        val account = signInManager.account.value ?: run {
            _uiState.update { it.copy(syncErrorMessage = "Sign in to Google before restoring.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(syncInProgress = true, syncErrorMessage = null) }
            val result = driveBackupService.restore(account)
            handleSyncResult(result, "Restore from Drive complete.")
        }
    }

    private fun handleSyncResult(result: SyncResult, successMessage: String) {
        when (result) {
            is SyncResult.Success -> {
                _uiState.update {
                    it.copy(
                        syncInProgress = false,
                        lastSyncAt = driveBackupService.lastSyncAt(),
                        syncErrorMessage = null,
                        transientMessage = successMessage
                    )
                }
            }

            is SyncResult.Failure -> {
                _uiState.update {
                    it.copy(
                        syncInProgress = false,
                        syncErrorMessage = result.message
                    )
                }
            }
        }
    }
}
