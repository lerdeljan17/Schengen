package com.schengen.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stays",
    indices = [Index("profileId")]
)
data class StayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val entryDate: String,
    val exitDate: String? = null,
    val source: EntrySource = EntrySource.MANUAL,
    val note: String = "",
    val countries: List<String> = emptyList()
)
