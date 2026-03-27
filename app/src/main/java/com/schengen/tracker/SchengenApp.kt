package com.schengen.tracker

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.schengen.tracker.data.AppDatabase
import com.schengen.tracker.data.StayRepository

class SchengenApp : Application() {
    lateinit var repository: StayRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "schengen.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .build()
        val prefs = getSharedPreferences("schengen_prefs", Context.MODE_PRIVATE)
        repository = StayRepository(db.stayDao(), prefs)
    }
}
