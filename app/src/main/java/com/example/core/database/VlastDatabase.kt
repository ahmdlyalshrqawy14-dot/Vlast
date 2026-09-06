package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.ActivityLogDao
import com.example.core.database.dao.AppSettingsDao
import com.example.core.database.dao.DailyUsageDao
import com.example.core.database.entity.ActivityLogEntity
import com.example.core.database.entity.AppSettingsEntity
import com.example.core.database.entity.DailyUsageEntity

@Database(
    entities = [DailyUsageEntity::class, AppSettingsEntity::class, ActivityLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VlastDatabase : RoomDatabase() {

    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: VlastDatabase? = null

        fun getInstance(context: Context): VlastDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VlastDatabase::class.java,
                    "vlast_database.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
