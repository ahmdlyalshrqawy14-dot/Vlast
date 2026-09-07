package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.ManagedAppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppRuleDao {

    @Query("SELECT * FROM managed_app_rules ORDER BY isFullyBlocked DESC, wifiDailyLimitEnabled DESC, sim1DailyLimitEnabled DESC, sim2DailyLimitEnabled DESC, dailyLimitEnabled DESC, appDisplayName ASC")
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

    @Query("UPDATE managed_app_rules SET wifiUsedBytesToday = 0, sim1UsedBytesToday = 0, sim2UsedBytesToday = 0, usedBytesToday = 0, lastResetDate = :todayDate WHERE lastResetDate != :todayDate")
    suspend fun resetDailyBytesIfNeeded(todayDate: String)

    @Query("UPDATE managed_app_rules SET usedBytesToday = CASE WHEN lastResetDate = :todayDate THEN usedBytesToday + :bytes ELSE :bytes END, lastResetDate = :todayDate WHERE packageName = :packageName")
    suspend fun addAppUsageBytes(packageName: String, bytes: Long, todayDate: String)

    @Query("UPDATE managed_app_rules SET wifiUsedBytesToday = CASE WHEN lastResetDate = :todayDate THEN wifiUsedBytesToday + :bytes ELSE :bytes END, usedBytesToday = CASE WHEN lastResetDate = :todayDate THEN usedBytesToday + :bytes ELSE :bytes END, lastResetDate = :todayDate WHERE packageName = :packageName")
    suspend fun addWifiAppUsageBytes(packageName: String, bytes: Long, todayDate: String)

    @Query("UPDATE managed_app_rules SET sim1UsedBytesToday = CASE WHEN lastResetDate = :todayDate THEN sim1UsedBytesToday + :bytes ELSE :bytes END, usedBytesToday = CASE WHEN lastResetDate = :todayDate THEN usedBytesToday + :bytes ELSE :bytes END, lastResetDate = :todayDate WHERE packageName = :packageName")
    suspend fun addSim1AppUsageBytes(packageName: String, bytes: Long, todayDate: String)

    @Query("UPDATE managed_app_rules SET sim2UsedBytesToday = CASE WHEN lastResetDate = :todayDate THEN sim2UsedBytesToday + :bytes ELSE :bytes END, usedBytesToday = CASE WHEN lastResetDate = :todayDate THEN usedBytesToday + :bytes ELSE :bytes END, lastResetDate = :todayDate WHERE packageName = :packageName")
    suspend fun addSim2AppUsageBytes(packageName: String, bytes: Long, todayDate: String)

    @Query("UPDATE managed_app_rules SET isFullyBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockedStatus(packageName: String, isBlocked: Boolean)

    @Query("UPDATE managed_app_rules SET dailyLimitBytes = :limitBytes, dailyLimitEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateLimit(packageName: String, limitBytes: Long?, enabled: Boolean)

    @Query("UPDATE managed_app_rules SET wifiDailyLimitBytes = :limitBytes, wifiDailyLimitEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateWifiLimit(packageName: String, limitBytes: Long?, enabled: Boolean)

    @Query("UPDATE managed_app_rules SET sim1DailyLimitBytes = :limitBytes, sim1DailyLimitEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateSim1Limit(packageName: String, limitBytes: Long?, enabled: Boolean)

    @Query("UPDATE managed_app_rules SET sim2DailyLimitBytes = :limitBytes, sim2DailyLimitEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateSim2Limit(packageName: String, limitBytes: Long?, enabled: Boolean)

    @Query("UPDATE managed_app_rules SET targetWifi = :targetWifi, targetSim1 = :targetSim1, targetSim2 = :targetSim2 WHERE packageName = :packageName")
    suspend fun updateNetworkTargets(packageName: String, targetWifi: Boolean, targetSim1: Boolean, targetSim2: Boolean)

    @Query("DELETE FROM managed_app_rules")
    suspend fun clearAllRules()
}
