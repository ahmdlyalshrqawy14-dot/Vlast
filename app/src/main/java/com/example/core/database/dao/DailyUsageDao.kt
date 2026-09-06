package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyUsageDao {

    @Query("SELECT * FROM daily_usage_records WHERE date = :date LIMIT 1")
    fun getDailyRecord(date: String): Flow<DailyUsageEntity?>

    @Query("SELECT * FROM daily_usage_records WHERE date = :date LIMIT 1")
    suspend fun getDailyRecordSync(date: String): DailyUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DailyUsageEntity)

    @Query("SELECT * FROM daily_usage_records ORDER BY date DESC")
    fun getAllHistoricalRecords(): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage_records ORDER BY date DESC LIMIT :limitDays")
    fun getRecentRecords(limitDays: Int): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<DailyUsageEntity>>
}
