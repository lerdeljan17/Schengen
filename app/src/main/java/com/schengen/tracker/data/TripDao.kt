package com.schengen.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE profileId = :profileId ORDER BY entryDate DESC")
    fun observeTrips(profileId: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Insert
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)

    @Query("DELETE FROM trips WHERE profileId = :profileId")
    suspend fun deleteTripsByProfileId(profileId: Long)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query(
        "SELECT * FROM trips WHERE profileId = :profileId AND exitDate IS NULL " +
            "ORDER BY entryDate DESC LIMIT 1"
    )
    suspend fun getLatestOpenTrip(profileId: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripById(id: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE profileId = :profileId ORDER BY entryDate DESC")
    suspend fun getAllTrips(profileId: Long): List<TripEntity>

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ProfileEntity?
}
