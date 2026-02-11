package com.schengen.tracker.data

import android.content.SharedPreferences
import com.schengen.tracker.domain.PlannedTrip
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.domain.Stay
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

class StayRepository(
    private val dao: StayDao,
    private val prefs: SharedPreferences
) {
    private val activeProfileIdFlow = MutableStateFlow(readActiveProfileId())

    fun observeProfiles(): Flow<List<Profile>> = dao.observeProfiles().map { profiles ->
        profiles.map { Profile(it.id, it.name, it.passportNumber) }
    }

    fun observeActiveProfileId() = activeProfileIdFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeStaysForActiveProfile(): Flow<List<Stay>> {
        return activeProfileIdFlow
            .filterNotNull()
            .flatMapLatest { profileId -> dao.observeAll(profileId) }
            .map { entities -> entities.map { it.toDomain() } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlannedTripsForActiveProfile(): Flow<List<PlannedTrip>> {
        return activeProfileIdFlow
            .filterNotNull()
            .flatMapLatest { profileId -> dao.observePlannedTrips(profileId) }
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

    suspend fun addManualEntry(entryDate: LocalDate) {
        val profileId = requireActiveProfileId() ?: return
        dao.insert(
            StayEntity(
                profileId = profileId,
                entryDate = entryDate.toString(),
                source = EntrySource.MANUAL
            )
        )
    }

    suspend fun addManualExit(exitDate: LocalDate): Boolean {
        val profileId = requireActiveProfileId() ?: return false
        val open = dao.getLatestOpenStay(profileId) ?: return false
        if (LocalDate.parse(open.entryDate).isAfter(exitDate)) return false
        dao.update(open.copy(exitDate = exitDate.toString()))
        return true
    }

    suspend fun addPlannedTrip(entryDate: LocalDate, exitDate: LocalDate, note: String): Boolean {
        if (exitDate.isBefore(entryDate)) return false
        val profileId = requireActiveProfileId() ?: return false
        dao.insertPlannedTrip(
            PlannedTripEntity(
                profileId = profileId,
                entryDate = entryDate.toString(),
                exitDate = exitDate.toString(),
                note = note.trim()
            )
        )
        return true
    }

    suspend fun addAutoState(inSchengen: Boolean, date: LocalDate) {
        val profileId = requireActiveProfileId() ?: return
        val open = dao.getLatestOpenStay(profileId)
        if (inSchengen && open == null) {
            dao.insert(
                StayEntity(
                    profileId = profileId,
                    entryDate = date.toString(),
                    source = EntrySource.AUTO
                )
            )
            return
        }

        if (!inSchengen && open != null) {
            val entry = LocalDate.parse(open.entryDate)
            if (!entry.isAfter(date)) {
                dao.update(open.copy(exitDate = date.toString()))
            }
        }
    }

    suspend fun deleteStayById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deletePlannedTripById(id: Long) {
        dao.deletePlannedTripById(id)
    }

    suspend fun exportCsv(writer: BufferedWriter) {
        val profiles = dao.getAllProfiles()
        writer.appendLine("type,profile_name,passport_number,entry_date,exit_date,source,note")

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
                        ""
                    )
                )
            )

            dao.getAllStays(profile.id).forEach { stay ->
                writer.appendLine(
                    encodeRow(
                        listOf(
                            "STAY",
                            profile.name,
                            profile.passportNumber,
                            stay.entryDate,
                            stay.exitDate ?: "",
                            stay.source.name,
                            ""
                        )
                    )
                )
            }

            dao.getAllPlannedTrips(profile.id).forEach { trip ->
                writer.appendLine(
                    encodeRow(
                        listOf(
                            "PLANNED",
                            profile.name,
                            profile.passportNumber,
                            trip.entryDate,
                            trip.exitDate,
                            "",
                            trip.note
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

                "STAY" -> {
                    val entry = fields.getOrElse(3) { "" }
                    if (entry.isBlank()) return@forEach
                    val exit = fields.getOrElse(4) { "" }.ifBlank { null }
                    val source = runCatching { EntrySource.valueOf(fields.getOrElse(5) { "MANUAL" }) }
                        .getOrDefault(EntrySource.MANUAL)
                    dao.insert(
                        StayEntity(
                            profileId = resolvedProfileId,
                            entryDate = entry,
                            exitDate = exit,
                            source = source
                        )
                    )
                    importedRows += 1
                }

                "PLANNED" -> {
                    val entry = fields.getOrElse(3) { "" }
                    val exit = fields.getOrElse(4) { "" }
                    if (entry.isBlank() || exit.isBlank()) return@forEach
                    dao.insertPlannedTrip(
                        PlannedTripEntity(
                            profileId = resolvedProfileId,
                            entryDate = entry,
                            exitDate = exit,
                            note = fields.getOrElse(6) { "" }
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

    suspend fun getSnapshotForActiveProfile(): Pair<List<Stay>, List<PlannedTrip>> {
        val profileId = requireActiveProfileId() ?: return emptyList<Stay>() to emptyList()
        val stays = dao.getAllStays(profileId).map { it.toDomain() }
        val planned = dao.getAllPlannedTrips(profileId).map { it.toDomain() }
        return stays to planned
    }

    private suspend fun requireActiveProfileId(): Long? {
        ensureDefaultProfile()
        return activeProfileIdFlow.value
    }

    private fun readActiveProfileId(): Long? {
        val value = prefs.getLong(KEY_ACTIVE_PROFILE_ID, -1L)
        return value.takeIf { it > 0 }
    }

    private fun StayEntity.toDomain(): Stay {
        return Stay(
            id = id,
            profileId = profileId,
            entryDate = LocalDate.parse(entryDate),
            exitDate = exitDate?.let(LocalDate::parse),
            source = source
        )
    }

    private fun PlannedTripEntity.toDomain(): PlannedTrip {
        return PlannedTrip(
            id = id,
            profileId = profileId,
            entryDate = LocalDate.parse(entryDate),
            exitDate = LocalDate.parse(exitDate),
            note = note
        )
    }

    companion object {
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

        private fun encodeRow(values: List<String>): String =
            values.joinToString(",") { value ->
                val escaped = value.replace("\"", "\"\"")
                "\"$escaped\""
            }

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
