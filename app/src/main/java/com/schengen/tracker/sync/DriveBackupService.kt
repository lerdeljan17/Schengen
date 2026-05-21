package com.schengen.tracker.sync

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.schengen.tracker.data.EntrySource
import com.schengen.tracker.data.TripRepository
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.ui.theme.AppearanceMode
import com.schengen.tracker.ui.theme.ColorThemes
import com.schengen.tracker.ui.theme.ThemePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class DriveBackupService(
    private val context: Context,
    private val repository: TripRepository,
    private val themePreferences: ThemePreferences,
    private val prefs: SharedPreferences
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun lastSyncAt(): Long = prefs.getLong(KEY_LAST_SYNC_AT, 0L)

    private fun setLastSyncAt(value: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_AT, value).apply()
    }

    private fun driveClient(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Schengen Tracker")
            .build()
    }

    suspend fun backup(account: GoogleSignInAccount): SyncResult = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload()
            val bytes = json.encodeToString(payload).toByteArray()
            val drive = driveClient(account)

            val existing = drive.files()
                .list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILENAME'")
                .setFields("files(id, modifiedTime)")
                .execute()
                .files
                .firstOrNull()

            val content = ByteArrayContent("application/json", bytes)
            if (existing != null) {
                drive.files()
                    .update(existing.id, File(), content)
                    .execute()
            } else {
                val meta = File().apply {
                    name = BACKUP_FILENAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(meta, content).execute()
            }
            setLastSyncAt(System.currentTimeMillis())
            SyncResult.Success(bytes.size.toLong())
        }.getOrElse { SyncResult.Failure(it.localizedMessage ?: it.javaClass.simpleName) }
    }

    suspend fun restore(account: GoogleSignInAccount): SyncResult = withContext(Dispatchers.IO) {
        runCatching {
            val drive = driveClient(account)
            val existing = drive.files()
                .list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILENAME'")
                .setFields("files(id, modifiedTime)")
                .execute()
                .files
                .firstOrNull() ?: return@runCatching SyncResult.Failure("No backup found on Drive.")

            val outputStream = ByteArrayOutputStream()
            drive.files().get(existing.id).executeMediaAndDownloadTo(outputStream)
            val payload = json.decodeFromString<BackupPayload>(outputStream.toString(Charsets.UTF_8.name()))

            val profiles = payload.profiles.map { Profile(0L, it.name, it.passportNumber) }
            val tripsByName = payload.profiles.associate { backupProfile ->
                backupProfile.name to backupProfile.trips.map { trip ->
                    Trip(
                        id = 0L,
                        profileId = 0L,
                        entryDate = LocalDate.parse(trip.entryDate),
                        exitDate = trip.exitDate?.let { LocalDate.parse(it) },
                        source = runCatching { EntrySource.valueOf(trip.source) }.getOrDefault(EntrySource.MANUAL),
                        note = trip.note,
                        countries = trip.countries
                    )
                }
            }
            repository.replaceAllData(profiles, tripsByName)

            themePreferences.setAppearanceMode(AppearanceMode.fromKey(payload.appearanceMode))
            themePreferences.setColorTheme(ColorThemes.byId(payload.colorThemeId).id)
            themePreferences.setStartWeekOnSunday(payload.startWeekOnSunday)

            setLastSyncAt(System.currentTimeMillis())
            SyncResult.Success(outputStream.size().toLong())
        }.getOrElse { SyncResult.Failure(it.localizedMessage ?: it.javaClass.simpleName) }
    }

    private suspend fun buildPayload(): BackupPayload {
        val themeState = themePreferences.current
        val byProfile = repository.getAllTripsAllProfiles()
        return BackupPayload(
            createdAt = System.currentTimeMillis(),
            appearanceMode = themeState.appearanceMode.key,
            colorThemeId = themeState.colorThemeId,
            startWeekOnSunday = themeState.startWeekOnSunday,
            profiles = byProfile.map { (profile, trips) ->
                BackupProfile(
                    name = profile.name,
                    passportNumber = profile.passportNumber,
                    trips = trips.map { trip ->
                        BackupTrip(
                            entryDate = trip.entryDate.toString(),
                            exitDate = trip.exitDate?.toString(),
                            source = trip.source.name,
                            note = trip.note,
                            countries = trip.countries
                        )
                    }
                )
            }
        )
    }

    companion object {
        private const val BACKUP_FILENAME = "schengen-backup.json"
        private const val KEY_LAST_SYNC_AT = "drive_last_sync_at"
    }
}

sealed interface SyncResult {
    data class Success(val sizeBytes: Long) : SyncResult
    data class Failure(val message: String) : SyncResult
}
