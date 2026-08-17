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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN principalName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN principalMobile TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN villageName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN schoolType TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [School::class, Visit::class, Task::class, UserEntity::class, AppNotification::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun visitDao(): VisitDao
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
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
