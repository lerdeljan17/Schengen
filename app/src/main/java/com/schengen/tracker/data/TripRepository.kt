package com.schengen.tracker.data

import android.content.SharedPreferences
import com.schengen.tracker.data.AppTypeConverters.Companion.decodeCountryCodes
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.Trip
import com.schengen.tracker.location.SchengenCountryCatalog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.io.BufferedReader
import java.io.BufferedWriter
import java.time.LocalDate

class TripRepository(
    private val dao: TripDao,
    private val prefs: SharedPreferences
) {
    private val activeProfileIdFlow = MutableStateFlow(readActiveProfileId())

    fun observeProfiles(): Flow<List<Profile>> = dao.observeProfiles().map { profiles ->
        profiles.map { Profile(it.id, it.name, it.passportNumber) }
    }

    fun observeActiveProfileId() = activeProfileIdFlow.asStateFlow()

    fun hasLocationTrackingPreference(): Boolean = prefs.contains(KEY_LOCATION_TRACKING_ENABLED)

    fun isLocationTrackingEnabled(): Boolean = prefs.getBoolean(KEY_LOCATION_TRACKING_ENABLED, false)

    fun setLocationTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_TRACKING_ENABLED, enabled).commit()
    }

    fun isOverstayAlertsEnabled(): Boolean =
        prefs.getBoolean(KEY_OVERSTAY_ALERTS_ENABLED, true)

    fun setOverstayAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OVERSTAY_ALERTS_ENABLED, enabled).apply()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTripsForActiveProfile(): Flow<List<Trip>> {
        return activeProfileIdFlow
            .filterNotNull()
            .flatMapLatest { profileId -> dao.observeTrips(profileId) }
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun ensureDefaultProfile() {
        val profiles = dao.getAllProfiles()
        if (profiles.isEmpty()) {
            val id = dao.insertProfile(ProfileEntity(name = "Primary passport", passportNumber = ""))
            setActiveProfile(id)
        } else if (activeProfileIdFlow.value == null) {
            setActiveProfile(profiles.first().id)
        }
    }

    suspend fun addProfile(name: String, passportNumber: String): Long {
        val id = dao.insertProfile(ProfileEntity(name = name.trim(), passportNumber = passportNumber.trim()))
        if (activeProfileIdFlow.value == null) {
            setActiveProfile(id)
        }
        return id
    }

    suspend fun setActiveProfile(id: Long) {
        prefs.edit().putLong(KEY_ACTIVE_PROFILE_ID, id).apply()
        activeProfileIdFlow.value = id
    }

    suspend fun addTrip(
        entryDate: LocalDate,
        exitDate: LocalDate?,
        source: EntrySource = EntrySource.MANUAL,
        note: String = "",
        countries: List<String> = emptyList()
    ): Boolean {
        if (exitDate != null && exitDate.isBefore(entryDate)) return false
        val profileId = requireActiveProfileId() ?: return false
        dao.insertTrip(
            TripEntity(
                profileId = profileId,
                entryDate = entryDate.toString(),
                exitDate = exitDate?.toString(),
                source = source,
                note = note.trim(),
                countries = normalizeCountryCodes(countries)
            )
        )
        return true
    }

    suspend fun addManualExit(exitDate: LocalDate): Boolean {
        val profileId = requireActiveProfileId() ?: return false
        val open = dao.getLatestOpenTrip(profileId) ?: return false
        if (LocalDate.parse(open.entryDate).isAfter(exitDate)) return false
        dao.updateTrip(open.copy(exitDate = exitDate.toString()))
        return true
    }

    suspend fun addAutoState(inSchengen: Boolean, date: LocalDate, countryCode: String? = null) {
        val profileId = requireActiveProfileId() ?: return
        val open = dao.getLatestOpenTrip(profileId)
        val normalizedCountryCode = countryCode?.let(SchengenCountryCatalog::normalizeCode)
        if (inSchengen && open == null) {
            dao.insertTrip(
                TripEntity(
                    profileId = profileId,
                    entryDate = date.toString(),
                    source = EntrySource.AUTO,
                    countries = listOfNotNull(normalizedCountryCode)
                )
            )
            return
        }

        if (inSchengen && open != null) {
            val updatedCountries = normalizeCountryCodes(open.countries + listOfNotNull(normalizedCountryCode))
            if (updatedCountries != open.countries) {
                dao.updateTrip(open.copy(countries = updatedCountries))
            }
            return
        }

        if (!inSchengen && open != null) {
            val entry = LocalDate.parse(open.entryDate)
            if (!entry.isAfter(date)) {
                dao.updateTrip(open.copy(exitDate = date.toString()))
            }
        }
    }

    suspend fun updateTrip(
        id: Long,
        entryDate: LocalDate,
        exitDate: LocalDate?,
        source: EntrySource,
        note: String,
        countries: List<String>
    ): Boolean {
        if (exitDate != null && exitDate.isBefore(entryDate)) return false
        val trip = dao.getTripById(id) ?: return false
        dao.updateTrip(
            trip.copy(
                entryDate = entryDate.toString(),
                exitDate = exitDate?.toString(),
                source = source,
                note = note.trim(),
                countries = normalizeCountryCodes(countries)
            )
        )
        return true
    }

    suspend fun updateProfile(id: Long, name: String, passportNumber: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return false
        val profile = dao.getProfileById(id) ?: return false
        dao.updateProfile(
            profile.copy(
                name = trimmedName,
                passportNumber = passportNumber.trim()
            )
        )
        return true
    }

    suspend fun deleteTripById(id: Long) {
        dao.deleteTripById(id)
    }

    suspend fun deleteAllTripsForActiveProfile() {
        val profileId = requireActiveProfileId() ?: return
        dao.deleteTripsByProfileId(profileId)
    }

    suspend fun deleteAllTripsAllProfiles() {
        dao.getAllProfiles().forEach { dao.deleteTripsByProfileId(it.id) }
    }

    suspend fun deleteProfileById(id: Long): Boolean {
        val existing = dao.getProfileById(id) ?: return false
        dao.deleteTripsByProfileId(existing.id)
        dao.deleteProfileById(existing.id)

        val remaining = dao.getAllProfiles()
        when {
            remaining.isEmpty() -> ensureDefaultProfile()
            activeProfileIdFlow.value == existing.id -> setActiveProfile(remaining.first().id)
        }
        return true
    }

    suspend fun exportCsv(writer: BufferedWriter) {
        val profiles = dao.getAllProfiles()
        writer.appendLine("type,profile_name,passport_number,entry_date,exit_date,source,note,countries")

        for (profile in profiles) {
            writer.appendLine(
                encodeRow(
                    listOf(
                        "PROFILE",
                        profile.name,
                        profile.passportNumber,
                        "",
                        "",
                        "",
                        "",
                        ""
                    )
                )
            )

            dao.getAllTrips(profile.id).forEach { trip ->
                writer.appendLine(
                    encodeRow(
                        listOf(
                            "TRIP",
                            profile.name,
                            profile.passportNumber,
                            trip.entryDate,
                            trip.exitDate ?: "",
                            trip.source.name,
                            trip.note,
                            formatCountriesForCsv(trip.countries)
                        )
                    )
                )
            }
        }
        writer.flush()
    }

    suspend fun importCsv(reader: BufferedReader): Int {
        val lines = reader.readLines()
        if (lines.isEmpty()) return 0

        val profileByKey = mutableMapOf<String, Long>()
        var importedRows = 0

        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val fields = parseCsvLine(line)
            if (fields.isEmpty()) return@forEach
            val type = fields.getOrElse(0) { "" }.trim().uppercase()
            val name = fields.getOrElse(1) { "" }.trim().ifBlank { "Imported profile" }
            val passport = fields.getOrElse(2) { "" }.trim()
            val key = "$name::$passport"
            var profileId = profileByKey[key]
            if (profileId == null) {
                val existing =
                    dao.getAllProfiles().firstOrNull { it.name == name && it.passportNumber == passport }
                profileId = existing?.id ?: dao.insertProfile(
                    ProfileEntity(
                        name = name,
                        passportNumber = passport
                    )
                )
                profileByKey[key] = profileId
            }

            val resolvedProfileId = profileId

            when (type) {
                "PROFILE" -> {
                    importedRows += 1
                }

                "TRIP", "STAY" -> {
                    val entry = fields.getOrElse(3) { "" }
                    if (entry.isBlank()) return@forEach
                    val exit = fields.getOrElse(4) { "" }.ifBlank { null }
                    val source = runCatching { EntrySource.valueOf(fields.getOrElse(5) { "MANUAL" }) }
                        .getOrDefault(EntrySource.MANUAL)
                    dao.insertTrip(
                        TripEntity(
                            profileId = resolvedProfileId,
                            entryDate = entry,
                            exitDate = exit,
                            source = source,
                            note = fields.getOrElse(6) { "" },
                            countries = parseImportedCountries(fields.getOrElse(7) { "" })
                        )
                    )
                    importedRows += 1
                }

                "PLANNED" -> {
                    val entry = fields.getOrElse(3) { "" }
                    val exit = fields.getOrElse(4) { "" }
                    if (entry.isBlank() || exit.isBlank()) return@forEach
                    dao.insertTrip(
                        TripEntity(
                            profileId = resolvedProfileId,
                            entryDate = entry,
                            exitDate = exit,
                            source = EntrySource.MANUAL,
                            note = fields.getOrElse(6) { "" },
                            countries = parseImportedCountries(fields.getOrElse(7) { "" })
                        )
                    )
                    importedRows += 1
                }
            }
        }

        if (activeProfileIdFlow.value == null) {
            dao.getAllProfiles().firstOrNull()?.id?.let { setActiveProfile(it) }
        }

        return importedRows
    }

    suspend fun getCurrentProfile(): Profile? {
        val id = activeProfileIdFlow.value ?: return null
        val profile = dao.getProfileById(id) ?: return null
        return Profile(profile.id, profile.name, profile.passportNumber)
    }

    suspend fun getAllProfiles(): List<Profile> =
        dao.getAllProfiles().map { Profile(it.id, it.name, it.passportNumber) }

    suspend fun getTripsSnapshotForActiveProfile(): List<Trip> {
        val profileId = requireActiveProfileId() ?: return emptyList()
        return dao.getAllTrips(profileId).map { it.toDomain() }
    }

    suspend fun getAllTripsAllProfiles(): Map<Profile, List<Trip>> {
        val profiles = dao.getAllProfiles()
        return profiles.associate { profile ->
            Profile(profile.id, profile.name, profile.passportNumber) to
                dao.getAllTrips(profile.id).map { it.toDomain() }
        }
    }

    suspend fun replaceAllData(
        profiles: List<Profile>,
        tripsByProfileName: Map<String, List<Trip>>
    ) {
        dao.getAllProfiles().forEach { dao.deleteTripsByProfileId(it.id) }
        dao.getAllProfiles().forEach { dao.deleteProfileById(it.id) }

        profiles.forEach { profile ->
            val newId = dao.insertProfile(
                ProfileEntity(name = profile.name, passportNumber = profile.passportNumber)
            )
            tripsByProfileName[profile.name]?.forEach { trip ->
                dao.insertTrip(
                    TripEntity(
                        profileId = newId,
                        entryDate = trip.entryDate.toString(),
                        exitDate = trip.exitDate?.toString(),
                        source = trip.source,
                        note = trip.note,
                        countries = normalizeCountryCodes(trip.countries)
                    )
                )
            }
        }

        ensureDefaultProfile()
    }

    private suspend fun requireActiveProfileId(): Long? {
        ensureDefaultProfile()
        return activeProfileIdFlow.value
    }

    private fun readActiveProfileId(): Long? {
        val value = prefs.getLong(KEY_ACTIVE_PROFILE_ID, -1L)
        return value.takeIf { it > 0 }
    }

    private fun TripEntity.toDomain(): Trip {
        return Trip(
            id = id,
            profileId = profileId,
            entryDate = LocalDate.parse(entryDate),
            exitDate = exitDate?.let(LocalDate::parse),
            source = source,
            note = note,
            countries = normalizeCountryCodes(countries)
        )
    }

    companion object {
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"
        private const val KEY_OVERSTAY_ALERTS_ENABLED = "overstay_alerts_enabled"

        private fun encodeRow(values: List<String>): String =
            values.joinToString(",") { value ->
                val escaped = value.replace("\"", "\"\"")
                "\"$escaped\""
            }

        private fun normalizeCountryCodes(values: List<String>): List<String> =
            SchengenCountryCatalog.normalizeCodes(values)

        private fun parseImportedCountries(value: String): List<String> =
            normalizeCountryCodes(decodeCountryCodes(value))

        private fun formatCountriesForCsv(countryCodes: List<String>): String =
            SchengenCountryCatalog.displayNames(countryCodes).joinToString()

        private fun parseCsvLine(line: String): List<String> {
            val out = mutableListOf<String>()
            val sb = StringBuilder()
            var i = 0
            var inQuotes = false
            while (i < line.length) {
                val c = line[i]
                when {
                    c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                        sb.append('"')
                        i += 1
                    }

                    c == '"' -> inQuotes = !inQuotes
                    c == ',' && !inQuotes -> {
                        out.add(sb.toString())
                        sb.clear()
                    }

                    else -> sb.append(c)
                }
                i += 1
            }
            out.add(sb.toString())
            return out
        }
    }
}
