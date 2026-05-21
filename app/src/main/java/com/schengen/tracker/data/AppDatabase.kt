package com.schengen.tracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TripEntity::class, ProfileEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trips` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `entryDate` TEXT NOT NULL,
                        `exitDate` TEXT,
                        `source` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `countries` TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trips_profileId` ON `trips`(`profileId`)")

                db.execSQL(
                    """
                    INSERT INTO `trips` (`profileId`, `entryDate`, `exitDate`, `source`, `note`, `countries`)
                    SELECT `profileId`, `entryDate`, `exitDate`, `source`, `note`, `countries` FROM `stays`
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `trips` (`profileId`, `entryDate`, `exitDate`, `source`, `note`, `countries`)
                    SELECT `profileId`, `entryDate`, `exitDate`, 'MANUAL', `note`, `countries` FROM `planned_trips`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE IF EXISTS `stays`")
                db.execSQL("DROP TABLE IF EXISTS `planned_trips`")
            }
        }
    }
}
