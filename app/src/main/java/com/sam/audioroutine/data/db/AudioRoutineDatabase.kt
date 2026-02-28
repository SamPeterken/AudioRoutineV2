package com.sam.audioroutine.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoutineEntity::class, RoutineBlockEntity::class, RoutineScheduleEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AudioRoutineDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN musicSource TEXT")
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN musicSelectionType TEXT")
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN musicSourceId TEXT")
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN musicDisplayName TEXT")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN additionalTtsEventsJson TEXT")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN recordedPromptFilePath TEXT")
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN recordedPromptDurationMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routine_blocks ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
