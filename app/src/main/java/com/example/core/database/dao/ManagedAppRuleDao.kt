package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.ManagedAppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppRuleDao {

    @Query("SELECT * FROM managed_app_rules ORDER BY isFullyBlocked DESC, dailyLimitEnabled DESC, appDisplayName ASC")
    fun getAllRules(): Flow<List<ManagedAppRuleEntity>>

    @Query("SELECT * FROM managed_app_rules")
    suspend fun getAllRulesSync(): List<ManagedAppRuleEntity>

    @Query("SELECT * FROM managed_app_rules WHERE packageName = :packageName LIMIT 1")
    fun getRule(packageName: String): Flow<ManagedAppRuleEntity?>

    @Query("SELECT * FROM managed_app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRuleSync(packageName: String): ManagedAppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: ManagedAppRuleEntity)

    @Query("DELETE FROM managed_app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)

    @Query("UPDATE managed_app_rules SET usedBytesToday = 0, lastResetDate = :todayDate WHERE lastResetDate != :todayDate")
    suspend fun resetDailyBytesIfNeeded(todayDate: String)

    @Query("UPDATE managed_app_rules SET usedBytesToday = CASE WHEN lastResetDate = :todayDate THEN usedBytesToday + :bytes ELSE :bytes END, lastResetDate = :todayDate WHERE packageName = :packageName")
    suspend fun addAppUsageBytes(packageName: String, bytes: Long, todayDate: String)

    @Query("UPDATE managed_app_rules SET isFullyBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockedStatus(packageName: String, isBlocked: Boolean)

    @Query("UPDATE managed_app_rules SET dailyLimitBytes = :limitBytes, dailyLimitEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateLimit(packageName: String, limitBytes: Long?, enabled: Boolean)
}
