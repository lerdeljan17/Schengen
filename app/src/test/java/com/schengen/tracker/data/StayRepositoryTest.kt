package com.schengen.tracker.data

import com.schengen.tracker.date
import java.io.StringReader
import java.io.StringWriter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StayRepositoryTest {
    @Test
    fun ensureDefaultProfile_createsAndPersistsActiveProfileWhenEmpty() = runTest {
        val dao = FakeStayDao()
        val prefs = FakeSharedPreferences()
        val repository = StayRepository(dao, prefs)

        repository.ensureDefaultProfile()

        val profiles = dao.getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals("Primary passport", profiles.single().name)
        assertEquals(profiles.single().id, repository.observeActiveProfileId().first())
        assertEquals(profiles.single().id, prefs.getLong("active_profile_id", -1L))
    }

    @Test
    fun addUpdateAndDeleteProfiles_trimValuesCascadeDataAndChooseFallbackProfile() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val primaryId = repository.observeActiveProfileId().first()!!
        val secondaryId = repository.addProfile(" Secondary ", " P2 ")
        repository.setActiveProfile(secondaryId)
        repository.addManualEntry(date("2024-03-01"), " stay ", listOf("DE"))
        assertTrue(repository.addPlannedTrip(date("2024-04-01"), date("2024-04-03"), " plan ", listOf("FR")))

        assertTrue(repository.updateProfile(secondaryId, " Updated ", " NEW "))
        assertFalse(repository.updateProfile(secondaryId, "   ", "ignored"))
        assertFalse(repository.updateProfile(99L, "Missing", "ignored"))
        assertTrue(repository.deleteProfileById(secondaryId))

        assertEquals(listOf(primaryId), dao.getAllProfiles().map { it.id })
        assertEquals(primaryId, repository.observeActiveProfileId().first())
        assertEquals(emptyList<StayEntity>(), dao.getAllStays(secondaryId))
        assertEquals(emptyList<PlannedTripEntity>(), dao.getAllPlannedTrips(secondaryId))
    }

    @Test
    fun deletingLastProfileRecreatesDefaultProfile() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val onlyProfileId = repository.observeActiveProfileId().first()!!

        assertTrue(repository.deleteProfileById(onlyProfileId))

        val profiles = dao.getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals("Primary passport", profiles.single().name)
        assertEquals(profiles.single().id, repository.observeActiveProfileId().first())
    }

    @Test
    fun staysAndPlannedTripsAreScopedToActiveProfile() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val primaryId = repository.observeActiveProfileId().first()!!
        val secondaryId = repository.addProfile("Secondary", "P2")

        repository.addManualEntry(date("2024-01-01"), "primary", listOf("DE"))
        repository.setActiveProfile(secondaryId)
        repository.addManualEntry(date("2024-02-01"), "secondary", listOf("FR"))
        assertTrue(repository.addPlannedTrip(date("2024-03-01"), date("2024-03-02"), "secondary plan", listOf("ES")))

        val secondarySnapshot = repository.getSnapshotForActiveProfile()
        assertEquals(listOf(secondaryId), secondarySnapshot.first.map { it.profileId }.distinct())
        assertEquals(listOf(secondaryId), secondarySnapshot.second.map { it.profileId }.distinct())
        repository.setActiveProfile(primaryId)
        val primarySnapshot = repository.getSnapshotForActiveProfile()
        assertEquals(listOf("primary"), primarySnapshot.first.map { it.note })
        assertEquals(emptyList<String>(), primarySnapshot.second.map { it.note })
    }

    @Test
    fun manualEntryExitAndStayUpdateValidateDatesAndNormalizeFields() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()

        repository.addManualEntry(date("2024-03-10"), "  hello  ", listOf("Germany", "DE", "unknown"))
        assertFalse(repository.addManualExit(date("2024-03-09")))
        assertTrue(repository.addManualExit(date("2024-03-12")))

        val profileId = repository.observeActiveProfileId().first()!!
        val stay = dao.getAllStays(profileId).single()
        assertEquals("hello", stay.note)
        assertEquals(listOf("DE"), stay.countries)
        assertEquals("2024-03-12", stay.exitDate)

        assertFalse(
            repository.updateStay(
                id = stay.id,
                entryDate = date("2024-03-15"),
                exitDate = date("2024-03-14"),
                source = EntrySource.MANUAL,
                note = "ignored",
                countries = emptyList()
            )
        )
        assertTrue(
            repository.updateStay(
                id = stay.id,
                entryDate = date("2024-03-08"),
                exitDate = null,
                source = EntrySource.AUTO,
                note = " updated ",
                countries = listOf("France", "FR")
            )
        )

        val updated = dao.getStayById(stay.id)!!
        assertEquals("2024-03-08", updated.entryDate)
        assertNull(updated.exitDate)
        assertEquals(EntrySource.AUTO, updated.source)
        assertEquals("updated", updated.note)
        assertEquals(listOf("FR"), updated.countries)
    }

    @Test
    fun plannedTripsValidateUpdateAndConfirmIntoStay() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()

        assertFalse(repository.addPlannedTrip(date("2024-04-10"), date("2024-04-09"), "bad", emptyList()))
        assertTrue(repository.addPlannedTrip(date("2024-04-10"), date("2024-04-12"), " plan ", listOf("Spain")))

        val profileId = repository.observeActiveProfileId().first()!!
        val trip = dao.getAllPlannedTrips(profileId).single()
        assertFalse(repository.updatePlannedTrip(trip.id, date("2024-04-15"), date("2024-04-14"), "bad", emptyList()))
        assertTrue(repository.updatePlannedTrip(trip.id, date("2024-04-11"), date("2024-04-13"), " updated ", listOf("ES")))

        assertTrue(repository.confirmPlannedTripById(trip.id, note = " confirmed ", countries = listOf("France")))

        assertEquals(emptyList<PlannedTripEntity>(), dao.getAllPlannedTrips(profileId))
        val confirmedStay = dao.getAllStays(profileId).single()
        assertEquals("2024-04-11", confirmedStay.entryDate)
        assertEquals("2024-04-13", confirmedStay.exitDate)
        assertEquals(EntrySource.MANUAL, confirmedStay.source)
        assertEquals("confirmed", confirmedStay.note)
        assertEquals(listOf("FR"), confirmedStay.countries)
    }

    @Test
    fun autoStateOpensUpdatesAndClosesAutomaticStay() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val profileId = repository.observeActiveProfileId().first()!!

        repository.addAutoState(inSchengen = true, date = date("2024-05-01"), countryCode = "de")
        repository.addAutoState(inSchengen = true, date = date("2024-05-02"), countryCode = "France")
        repository.addAutoState(inSchengen = true, date = date("2024-05-03"), countryCode = "DE")
        repository.addAutoState(inSchengen = false, date = date("2024-04-30"), countryCode = "US")
        assertNull(dao.getLatestOpenStay(profileId)!!.exitDate)
        repository.addAutoState(inSchengen = false, date = date("2024-05-04"), countryCode = "US")

        val stay = dao.getAllStays(profileId).single()
        assertEquals(EntrySource.AUTO, stay.source)
        assertEquals("2024-05-01", stay.entryDate)
        assertEquals("2024-05-04", stay.exitDate)
        assertEquals(listOf("DE", "FR"), stay.countries)
    }

    @Test
    fun csvExportAndImportRoundTripProfilesStaysPlansAndQuotedFields() = runTest {
        val sourceDao = FakeStayDao()
        val sourceRepository = StayRepository(sourceDao, FakeSharedPreferences())
        val profileId = sourceRepository.addProfile("""Alex "A", B""", "P,1")
        sourceRepository.setActiveProfile(profileId)
        sourceRepository.addManualEntry(date("2024-06-01"), """note, "quoted"""", listOf("Germany", "France"))
        sourceRepository.addManualExit(date("2024-06-03"))
        assertTrue(sourceRepository.addPlannedTrip(date("2024-07-01"), date("2024-07-02"), "future, trip", listOf("Spain")))

        val csv = StringWriter().also { writer ->
            sourceRepository.exportCsv(writer.buffered())
        }.toString()
        assertTrue(csv.contains("Alex \"\"A\"\", B"))
        assertTrue(csv.contains("note, \"\"quoted\"\""))

        val targetDao = FakeStayDao()
        val targetRepository = StayRepository(targetDao, FakeSharedPreferences())
        val importedRows = targetRepository.importCsv(StringReader("$csv\n\n").buffered())

        assertEquals(3, importedRows)
        val importedProfile = targetDao.getAllProfiles().single()
        assertEquals("""Alex "A", B""", importedProfile.name)
        assertEquals("P,1", importedProfile.passportNumber)
        val importedStay = targetDao.getAllStays(importedProfile.id).single()
        assertEquals("2024-06-01", importedStay.entryDate)
        assertEquals("2024-06-03", importedStay.exitDate)
        assertEquals(EntrySource.MANUAL, importedStay.source)
        assertEquals("""note, "quoted"""", importedStay.note)
        assertEquals(listOf("DE", "FR"), importedStay.countries)
        val importedTrip = targetDao.getAllPlannedTrips(importedProfile.id).single()
        assertEquals("future, trip", importedTrip.note)
        assertEquals(listOf("ES"), importedTrip.countries)
        assertEquals(importedProfile.id, targetRepository.observeActiveProfileId().first())
    }

    @Test
    fun csvImportSkipsMissingDatesAndFallsBackToManualSource() = runTest {
        val dao = FakeStayDao()
        val repository = StayRepository(dao, FakeSharedPreferences())
        val csv = """
            type,profile_name,passport_number,entry_date,exit_date,source,note,countries
            STAY,Traveler,P1,2024-01-01,2024-01-02,NOT_A_SOURCE,note,Germany
            STAY,Traveler,P1,,2024-01-02,MANUAL,missing entry,Germany
            PLANNED,Traveler,P1,2024-02-01,,ignored,missing exit,France
        """.trimIndent()

        val importedRows = repository.importCsv(StringReader(csv).buffered())

        assertEquals(1, importedRows)
        val profile = dao.getAllProfiles().single()
        val stay = dao.getAllStays(profile.id).single()
        assertEquals(EntrySource.MANUAL, stay.source)
        assertEquals("note", stay.note)
        assertEquals(listOf("DE"), stay.countries)
        assertEquals(emptyList<PlannedTripEntity>(), dao.getAllPlannedTrips(profile.id))
    }

    @Test
    fun locationTrackingPreferenceIsPersisted() {
        val repository = StayRepository(FakeStayDao(), FakeSharedPreferences())

        assertFalse(repository.hasLocationTrackingPreference())
        assertFalse(repository.isLocationTrackingEnabled())

        repository.setLocationTrackingEnabled(true)

        assertTrue(repository.hasLocationTrackingPreference())
        assertTrue(repository.isLocationTrackingEnabled())
    }
}
