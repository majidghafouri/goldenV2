package com.goldenv2.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.goldenv2.core.domain.model.Server
import com.goldenv2.core.domain.model.Subscription

@Database(
    entities = [Server::class, Subscription::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Adds the pinning column without destroying existing servers or subscriptions.
         */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE subscriptions ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "goldenv2.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}