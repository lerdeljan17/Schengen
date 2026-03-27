package com.schengen.tracker.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StayEntity::class, ProfileEntity::class, PlannedTripEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stayDao(): StayDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `passportNumber` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO `profiles` (`name`, `passportNumber`) VALUES ('Primary passport', '')")

                db.execSQL("ALTER TABLE `stays` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stays_profileId` ON `stays`(`profileId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `planned_trips` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `entryDate` TEXT NOT NULL,
                        `exitDate` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_trips_profileId` ON `planned_trips`(`profileId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stays` ADD COLUMN `note` TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stays` ADD COLUMN `countries` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `planned_trips` ADD COLUMN `countries` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
