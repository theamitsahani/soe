package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppNotification
import com.example.data.model.School
import com.example.data.model.Task
import com.example.data.model.Visit
import com.example.data.model.VisitEvent

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN principalName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN principalMobile TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN villageName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN schoolType TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op migration for gap bridge
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE visits ADD COLUMN taskId TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE visits ADD COLUMN villageName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN schoolType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN udiseCode TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN principalName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN principalMobile TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN startedAt INTEGER")
        db.execSQL("ALTER TABLE visits ADD COLUMN completedAt INTEGER")
        db.execSQL("ALTER TABLE visits ADD COLUMN submittedAt INTEGER")
        db.execSQL("ALTER TABLE visits ADD COLUMN reviewedAt INTEGER")
        db.execSQL("ALTER TABLE visits ADD COLUMN reviewedBy TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN reviewNotes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN rejectionReason TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE visits ADD COLUMN latitude REAL")
        db.execSQL("ALTER TABLE visits ADD COLUMN longitude REAL")
        db.execSQL("ALTER TABLE visits ADD COLUMN appVersion TEXT NOT NULL DEFAULT '1.0.0'")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS visit_events (
                eventId TEXT NOT NULL PRIMARY KEY,
                visitId TEXT NOT NULL,
                taskId TEXT NOT NULL DEFAULT '',
                eventType TEXT NOT NULL,
                actorId TEXT NOT NULL DEFAULT '',
                actorName TEXT NOT NULL DEFAULT '',
                actorRole TEXT NOT NULL DEFAULT '',
                statusFrom TEXT NOT NULL DEFAULT '',
                statusTo TEXT NOT NULL DEFAULT '',
                details TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL,
                syncStatus TEXT NOT NULL DEFAULT 'SYNCED'
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [School::class, Visit::class, Task::class, UserEntity::class, AppNotification::class, VisitEvent::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun visitDao(): VisitDao
    abstract fun visitEventDao(): VisitEventDao
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun appNotificationDao(): AppNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soe_school_visit.db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
