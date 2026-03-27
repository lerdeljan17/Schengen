package com.schengen.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_trips",
    indices = [Index("profileId")]
)
data class PlannedTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val entryDate: String,
    val exitDate: String,
    val note: String = "",
    val countries: List<String> = emptyList()
)
