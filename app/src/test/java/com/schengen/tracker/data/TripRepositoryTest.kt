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

class TripRepositoryTest {
    @Test
    fun ensureDefaultProfile_createsAndPersistsActiveProfileWhenEmpty() = runTest {
        val dao = FakeTripDao()
        val prefs = FakeSharedPreferences()
        val repository = TripRepository(dao, prefs)

        repository.ensureDefaultProfile()

        val profiles = dao.getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals("Primary passport", profiles.single().name)
        assertEquals(profiles.single().id, repository.observeActiveProfileId().first())
        assertEquals(profiles.single().id, prefs.getLong("active_profile_id", -1L))
    }

    @Test
    fun deletingLastProfileRecreatesDefaultProfile() = runTest {
        val dao = FakeTripDao()
        val repository = TripRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val onlyProfileId = repository.observeActiveProfileId().first()!!

        assertTrue(repository.deleteProfileById(onlyProfileId))

        val profiles = dao.getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals("Primary passport", profiles.single().name)
        assertEquals(profiles.single().id, repository.observeActiveProfileId().first())
    }

    @Test
    fun tripsAreScopedToActiveProfile() = runTest {
        val dao = FakeTripDao()
        val repository = TripRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val primaryId = repository.observeActiveProfileId().first()!!
        val secondaryId = repository.addProfile("Secondary", "P2")

        repository.addTrip(date("2024-01-01"), date("2024-01-05"), note = "primary", countries = listOf("DE"))
        repository.setActiveProfile(secondaryId)
        repository.addTrip(date("2024-02-01"), date("2024-02-05"), note = "secondary", countries = listOf("FR"))
        repository.addTrip(date("2024-03-01"), date("2024-03-02"), note = "secondary plan", countries = listOf("ES"))

        val secondaryTrips = repository.getTripsSnapshotForActiveProfile()
        assertEquals(listOf(secondaryId), secondaryTrips.map { it.profileId }.distinct())
        repository.setActiveProfile(primaryId)
        val primaryTrips = repository.getTripsSnapshotForActiveProfile()
        assertEquals(listOf("primary"), primaryTrips.map { it.note })
    }

    @Test
    fun addAndUpdateTripValidateDatesAndNormalizeFields() = runTest {
        val dao = FakeTripDao()
        val repository = TripRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()

        assertTrue(repository.addTrip(date("2024-03-10"), null, note = "  open  ", countries = listOf("Germany", "DE", "unknown")))
        assertFalse(repository.addManualExit(date("2024-03-09")))
        assertTrue(repository.addManualExit(date("2024-03-12")))

        val profileId = repository.observeActiveProfileId().first()!!
        val trip = dao.getAllTrips(profileId).single()
        assertEquals("open", trip.note)
        assertEquals(listOf("DE"), trip.countries)
        assertEquals("2024-03-12", trip.exitDate)

        assertFalse(
            repository.updateTrip(
                id = trip.id,
                entryDate = date("2024-03-15"),
                exitDate = date("2024-03-14"),
                source = EntrySource.MANUAL,
                note = "ignored",
                countries = emptyList()
            )
        )
        assertTrue(
            repository.updateTrip(
                id = trip.id,
                entryDate = date("2024-03-08"),
                exitDate = null,
                source = EntrySource.AUTO,
                note = " updated ",
                countries = listOf("France", "FR")
            )
        )

        val updated = dao.getTripById(trip.id)!!
        assertEquals("2024-03-08", updated.entryDate)
        assertNull(updated.exitDate)
        assertEquals(EntrySource.AUTO, updated.source)
        assertEquals("updated", updated.note)
        assertEquals(listOf("FR"), updated.countries)
    }

    @Test
    fun autoStateOpensUpdatesAndClosesAutomaticTrip() = runTest {
        val dao = FakeTripDao()
        val repository = TripRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val profileId = repository.observeActiveProfileId().first()!!

        repository.addAutoState(inSchengen = true, date = date("2024-05-01"), countryCode = "de")
        repository.addAutoState(inSchengen = true, date = date("2024-05-02"), countryCode = "France")
        repository.addAutoState(inSchengen = true, date = date("2024-05-03"), countryCode = "DE")
        repository.addAutoState(inSchengen = false, date = date("2024-04-30"), countryCode = "US")
        assertNull(dao.getLatestOpenTrip(profileId)!!.exitDate)
        repository.addAutoState(inSchengen = false, date = date("2024-05-04"), countryCode = "US")

        val trip = dao.getAllTrips(profileId).single()
        assertEquals(EntrySource.AUTO, trip.source)
        assertEquals("2024-05-01", trip.entryDate)
        assertEquals("2024-05-04", trip.exitDate)
        assertEquals(listOf("DE", "FR"), trip.countries)
    }

    @Test
    fun csvExportAndImportRoundTripProfilesAndTrips() = runTest {
        val sourceDao = FakeTripDao()
        val sourceRepository = TripRepository(sourceDao, FakeSharedPreferences())
        val profileId = sourceRepository.addProfile("""Alex "A", B""", "P,1")
        sourceRepository.setActiveProfile(profileId)
        sourceRepository.addTrip(
            date("2024-06-01"),
            date("2024-06-03"),
            note = """note, "quoted"""",
            countries = listOf("Germany", "France")
        )
        sourceRepository.addTrip(
            date("2024-07-01"),
            date("2024-07-02"),
            note = "future, trip",
            countries = listOf("Spain")
        )

        val csv = StringWriter().also { writer ->
            sourceRepository.exportCsv(writer.buffered())
        }.toString()
        assertTrue(csv.contains("Alex \"\"A\"\", B"))
        assertTrue(csv.contains("note, \"\"quoted\"\""))

        val targetDao = FakeTripDao()
        val targetRepository = TripRepository(targetDao, FakeSharedPreferences())
        val importedRows = targetRepository.importCsv(StringReader("$csv\n\n").buffered())

        assertEquals(3, importedRows)
        val importedProfile = targetDao.getAllProfiles().single()
        assertEquals("""Alex "A", B""", importedProfile.name)
        assertEquals("P,1", importedProfile.passportNumber)
        val importedTrips = targetDao.getAllTrips(importedProfile.id)
        assertEquals(2, importedTrips.size)
        val byNote = importedTrips.associateBy { it.note }
        val firstTrip = byNote.getValue("""note, "quoted"""")
        assertEquals("2024-06-01", firstTrip.entryDate)
        assertEquals("2024-06-03", firstTrip.exitDate)
        assertEquals(EntrySource.MANUAL, firstTrip.source)
        assertEquals(listOf("DE", "FR"), firstTrip.countries)
        val secondTrip = byNote.getValue("future, trip")
        assertEquals(listOf("ES"), secondTrip.countries)
        assertEquals(importedProfile.id, targetRepository.observeActiveProfileId().first())
    }

    @Test
    fun csvImportSkipsMissingDatesAndFallsBackToManualSource() = runTest {
        val dao = FakeTripDao()
        val repository = TripRepository(dao, FakeSharedPreferences())
        val csv = """
            type,profile_name,passport_number,entry_date,exit_date,source,note,countries
            TRIP,Traveler,P1,2024-01-01,2024-01-02,NOT_A_SOURCE,note,Germany
            TRIP,Traveler,P1,,2024-01-02,MANUAL,missing entry,Germany
            PLANNED,Traveler,P1,2024-02-01,,ignored,missing exit,France
        """.trimIndent()

        val importedRows = repository.importCsv(StringReader(csv).buffered())

        assertEquals(1, importedRows)
        val profile = dao.getAllProfiles().single()
        val trip = dao.getAllTrips(profile.id).single()
        assertEquals(EntrySource.MANUAL, trip.source)
        assertEquals("note", trip.note)
        assertEquals(listOf("DE"), trip.countries)
    }

    @Test
    fun locationTrackingPreferenceIsPersisted() {
        val repository = TripRepository(FakeTripDao(), FakeSharedPreferences())

        assertFalse(repository.hasLocationTrackingPreference())
        assertFalse(repository.isLocationTrackingEnabled())

        repository.setLocationTrackingEnabled(true)

        assertTrue(repository.hasLocationTrackingPreference())
        assertTrue(repository.isLocationTrackingEnabled())
    }

    @Test
    fun overstayAlertsPreferenceDefaultsToEnabled() {
        val repository = TripRepository(FakeTripDao(), FakeSharedPreferences())
        assertTrue(repository.isOverstayAlertsEnabled())
        repository.setOverstayAlertsEnabled(false)
        assertFalse(repository.isOverstayAlertsEnabled())
    }

    @Test
    fun deleteAllTripsForActiveProfileOnlyRemovesActiveScope() = runTest {
        val dao = FakeTripDao()
        val repository = TripRepository(dao, FakeSharedPreferences())
        repository.ensureDefaultProfile()
        val primaryId = repository.observeActiveProfileId().first()!!
        val secondaryId = repository.addProfile("Secondary", "P2")

        repository.addTrip(date("2024-01-01"), date("2024-01-02"))
        repository.setActiveProfile(secondaryId)
        repository.addTrip(date("2024-02-01"), date("2024-02-02"))

        repository.deleteAllTripsForActiveProfile()

        assertEquals(emptyList<TripEntity>(), dao.getAllTrips(secondaryId))
        assertEquals(1, dao.getAllTrips(primaryId).size)
    }
}
