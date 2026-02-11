package com.schengen.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StayDao {
    @Query("SELECT * FROM stays WHERE profileId = :profileId ORDER BY entryDate DESC")
    fun observeAll(profileId: Long): Flow<List<StayEntity>>

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM planned_trips WHERE profileId = :profileId ORDER BY entryDate ASC")
    fun observePlannedTrips(profileId: Long): Flow<List<PlannedTripEntity>>

    @Insert
    suspend fun insert(stay: StayEntity): Long

    @Insert
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Insert
    suspend fun insertPlannedTrip(trip: PlannedTripEntity): Long

    @Update
    suspend fun update(stay: StayEntity)

    @Delete
    suspend fun delete(stay: StayEntity)

    @Query("DELETE FROM stays WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM planned_trips WHERE id = :id")
    suspend fun deletePlannedTripById(id: Long)

    @Query("SELECT * FROM stays WHERE profileId = :profileId AND exitDate IS NULL ORDER BY entryDate DESC LIMIT 1")
    suspend fun getLatestOpenStay(profileId: Long): StayEntity?

    @Query("SELECT * FROM stays WHERE profileId = :profileId ORDER BY entryDate DESC")
    suspend fun getAllStays(profileId: Long): List<StayEntity>

    @Query("SELECT * FROM planned_trips WHERE profileId = :profileId ORDER BY entryDate ASC")
    suspend fun getAllPlannedTrips(profileId: Long): List<PlannedTripEntity>

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ProfileEntity?
}
