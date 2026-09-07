package com.example.core.repository

import com.example.core.database.dao.ActivityLogDao
import com.example.core.database.dao.AppSettingsDao
import com.example.core.database.dao.DailyUsageDao
import com.example.core.database.dao.ManagedAppRuleDao
import com.example.core.database.entity.ActivityLogEntity
import com.example.core.database.entity.AppSettingsEntity
import com.example.core.database.entity.DailyUsageEntity
import com.example.core.database.entity.ManagedAppRuleEntity
import com.example.core.model.ActivityLogEntry
import com.example.core.model.DailyUsageRecord
import com.example.core.model.ManagedAppRule
import com.example.core.model.NetworkType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UsageRepository(
    private val dailyUsageDao: DailyUsageDao,
    private val appSettingsDao: AppSettingsDao,
    private val activityLogDao: ActivityLogDao? = null,
    private val managedAppRuleDao: ManagedAppRuleDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val mutex = Mutex()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Debounce helper for tunnel rapid switching (Item 18).
     */
    private var lastTunnelToggleTimestamp: Long = 0L

    fun getTodayDateString(): String = LocalDate.now().format(dateFormatter)

    fun observeTodayRecord(): Flow<DailyUsageRecord> {
        val today = getTodayDateString()
        return dailyUsageDao.getDailyRecord(today).map { entity ->
            entity?.toDomain() ?: createInitialRecordForDate(today)
        }.distinctUntilChanged().flowOn(ioDispatcher)
    }

    fun observeSettings(): Flow<AppSettingsEntity> {
        return appSettingsDao.getSettings().map { settings ->
            settings ?: AppSettingsEntity()
        }.distinctUntilChanged().flowOn(ioDispatcher)
    }

    fun observeRecentLogs(limit: Int = 100): Flow<List<ActivityLogEntry>> {
        return (activityLogDao?.getRecentLogs(limit) ?: kotlinx.coroutines.flow.flowOf(emptyList())).map { list ->
            list.map { it.toDomain() }
        }.distinctUntilChanged().flowOn(ioDispatcher)
    }

    fun observeAllHistory(): Flow<List<DailyUsageRecord>> {
        return dailyUsageDao.getAllHistoricalRecords().map { list ->
            list.map { it.toDomain() }
        }.distinctUntilChanged().flowOn(ioDispatcher)
    }

    fun observeRecentHistory(days: Int): Flow<List<DailyUsageRecord>> {
        return dailyUsageDao.getRecentRecords(days).map { list ->
            list.map { it.toDomain() }
        }.distinctUntilChanged().flowOn(ioDispatcher)
    }

    suspend fun getOrCreateTodayRecord(): DailyUsageRecord = withContext(ioDispatcher) {
        mutex.withLock {
            val today = getTodayDateString()
            val existing = dailyUsageDao.getDailyRecordSync(today)
            if (existing != null) {
                return@withContext existing.toDomain()
            }

            val settings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            val initial = DailyUsageEntity(
                date = today,
                wifiRecurringLimitBytes = settings.wifiRecurringLimitBytes,
                wifiRecurringEnabled = settings.wifiRecurringEnabled,
                wifiTodayOverrideLimitBytes = null, // Cleared at midnight!
                wifiUsedBytes = 0L,
                mobileRecurringLimitBytes = settings.mobileRecurringLimitBytes,
                mobileRecurringEnabled = settings.mobileRecurringEnabled,
                mobileTodayOverrideLimitBytes = null, // Cleared at midnight!
                mobileUsedBytes = 0L,
                sim2RecurringLimitBytes = settings.sim2RecurringLimitBytes,
                sim2RecurringEnabled = settings.sim2RecurringEnabled,
                sim2TodayOverrideLimitBytes = null,
                sim2UsedBytes = 0L,
                killSwitchEnabled = settings.killSwitchActive,
                hotspotUsedBytes = 0L,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            dailyUsageDao.insertOrUpdate(initial)
            initial.toDomain()
        }
    }

    suspend fun recordDeviceBytes(networkType: NetworkType, bytesToAdd: Long, simSlot: Int = 0) = withContext(ioDispatcher) {
        if (bytesToAdd <= 0L || networkType == NetworkType.NONE) return@withContext
        mutex.withLock {
            val today = getTodayDateString()
            val current = dailyUsageDao.getDailyRecordSync(today)
                ?: DailyUsageEntity.fromDomain(createInitialRecordForDate(today))

            val updated = when (networkType) {
                NetworkType.WIFI -> current.copy(
                    wifiUsedBytes = current.wifiUsedBytes + bytesToAdd,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                NetworkType.MOBILE -> {
                    if (simSlot == 1) {
                        current.copy(
                            sim2UsedBytes = current.sim2UsedBytes + bytesToAdd,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    } else {
                        current.copy(
                            mobileUsedBytes = current.mobileUsedBytes + bytesToAdd,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                NetworkType.NONE -> current
            }
            dailyUsageDao.insertOrUpdate(updated)
        }
    }

    suspend fun updateHotspotBytes(hotspotTotalBytes: Long) = withContext(ioDispatcher) {
        if (hotspotTotalBytes < 0L) return@withContext
        mutex.withLock {
            val today = getTodayDateString()
            val current = dailyUsageDao.getDailyRecordSync(today)
                ?: DailyUsageEntity.fromDomain(createInitialRecordForDate(today))

            val updated = current.copy(
                hotspotUsedBytes = hotspotTotalBytes,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            dailyUsageDao.insertOrUpdate(updated)
        }
    }

    suspend fun logActivity(
        eventType: ActivityLogEntry.EventType,
        reason: ActivityLogEntry.SpecificCutReason,
        description: String,
        details: String? = null
    ) = withContext(ioDispatcher) {
        if (activityLogDao == null) return@withContext
        val entry = ActivityLogEntity(
            timestamp = System.currentTimeMillis(),
            eventType = eventType.name,
            specificReason = reason.name,
            description = description,
            details = details
        )
        activityLogDao.insertLog(entry)
    }

    suspend fun setRecurringLimit(
        networkType: NetworkType,
        limitBytes: Long,
        enabled: Boolean,
        simSlot: Int = 0
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            val updatedSettings = when (networkType) {
                NetworkType.WIFI -> currentSettings.copy(
                    wifiRecurringLimitBytes = limitBytes,
                    wifiRecurringEnabled = enabled
                )
                NetworkType.MOBILE -> {
                    if (simSlot == 1) {
                        currentSettings.copy(
                            sim2RecurringLimitBytes = limitBytes,
                            sim2RecurringEnabled = enabled
                        )
                    } else {
                        currentSettings.copy(
                            mobileRecurringLimitBytes = limitBytes,
                            mobileRecurringEnabled = enabled
                        )
                    }
                }
                NetworkType.NONE -> currentSettings
            }
            appSettingsDao.saveSettings(updatedSettings)

            val today = getTodayDateString()
            val currentDay = dailyUsageDao.getDailyRecordSync(today)
            if (currentDay != null) {
                val updatedDay = when (networkType) {
                    NetworkType.WIFI -> currentDay.copy(
                        wifiRecurringLimitBytes = limitBytes,
                        wifiRecurringEnabled = enabled,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                    NetworkType.MOBILE -> {
                        if (simSlot == 1) {
                            currentDay.copy(
                                sim2RecurringLimitBytes = limitBytes,
                                sim2RecurringEnabled = enabled,
                                lastUpdatedTimestamp = System.currentTimeMillis()
                            )
                        } else {
                            currentDay.copy(
                                mobileRecurringLimitBytes = limitBytes,
                                mobileRecurringEnabled = enabled,
                                lastUpdatedTimestamp = System.currentTimeMillis()
                            )
                        }
                    }
                    NetworkType.NONE -> currentDay
                }
                dailyUsageDao.insertOrUpdate(updatedDay)
            }
        }
    }

    suspend fun setTodayOverrideLimit(
        networkType: NetworkType,
        overrideBytes: Long?,
        simSlot: Int = 0
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val today = getTodayDateString()
            val current = dailyUsageDao.getDailyRecordSync(today)
                ?: DailyUsageEntity.fromDomain(createInitialRecordForDate(today))

            val updated = when (networkType) {
                NetworkType.WIFI -> current.copy(
                    wifiTodayOverrideLimitBytes = overrideBytes,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                NetworkType.MOBILE -> {
                    if (simSlot == 1) {
                        current.copy(
                            sim2TodayOverrideLimitBytes = overrideBytes,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    } else {
                        current.copy(
                            mobileTodayOverrideLimitBytes = overrideBytes,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    }
                }
                NetworkType.NONE -> current
            }
            dailyUsageDao.insertOrUpdate(updated)
        }
    }

    suspend fun setKillSwitch(active: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(killSwitchActive = active))

            val today = getTodayDateString()
            val current = dailyUsageDao.getDailyRecordSync(today)
            if (current != null) {
                dailyUsageDao.insertOrUpdate(
                    current.copy(
                        killSwitchEnabled = active,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                )
            }

            // Log event
            if (active) {
                logActivity(
                    eventType = ActivityLogEntry.EventType.DISCONNECTED,
                    reason = ActivityLogEntry.SpecificCutReason.MANUAL_KILL_SWITCH,
                    description = "تم تفعيل القطع الكامل اليدوي لحظر كافة حركات البيانات."
                )
            } else {
                logActivity(
                    eventType = ActivityLogEntry.EventType.RESTORED,
                    reason = ActivityLogEntry.SpecificCutReason.CONNECTION_RESTORED,
                    description = "تم إيقاف القطع الكامل واستعادة حركة البيانات."
                )
            }
        }
    }

    /**
     * Debounced rapid toggle protection (Item 18).
     * Prevents database corruption or packet race conditions when toggled within 1 second.
     */
    suspend fun canToggleTunnel(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTunnelToggleTimestamp < 1000L) {
            return false
        }
        lastTunnelToggleTimestamp = now
        return true
    }

    suspend fun setActiveSimSlot(slot: Int) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(activeSimSlot = slot))
        }
    }

    suspend fun setSettingsLock(enabled: Boolean, pin: String?) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(
                currentSettings.copy(
                    settingsLockEnabled = enabled,
                    settingsLockPin = pin
                )
            )
        }
    }

    suspend fun setFirstRunCompleted(completed: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(firstRunCompleted = completed))
        }
    }

    suspend fun calibrateUsage(
        wifiBaselineBytes: Long,
        mobileBaselineBytes: Long
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val today = getTodayDateString()
            val current = dailyUsageDao.getDailyRecordSync(today) ?: return@withContext

            val calibratedWifi = maxOf(current.wifiUsedBytes, wifiBaselineBytes)
            val calibratedMobile = maxOf(current.mobileUsedBytes, mobileBaselineBytes)

            val updated = current.copy(
                wifiUsedBytes = calibratedWifi,
                mobileUsedBytes = calibratedMobile,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            dailyUsageDao.insertOrUpdate(updated)

            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(lastCalibrationTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun setHotspotAlertThreshold(thresholdBytes: Long?, enabled: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(
                currentSettings.copy(
                    hotspotAlertThresholdBytes = thresholdBytes,
                    hotspotAlertEnabled = enabled
                )
            )
        }
    }

    suspend fun setSoundAlertEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(soundAlertEnabled = enabled))
        }
    }

    suspend fun setColorBlindModeEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(colorBlindModeEnabled = enabled))
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(hapticFeedbackEnabled = enabled))
        }
    }

    suspend fun setMonitoringActive(active: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSettings = appSettingsDao.getSettingsSync() ?: AppSettingsEntity()
            appSettingsDao.saveSettings(currentSettings.copy(isMonitoringActive = active))
        }
    }

    // =========================================================================
    // Phase 4: Per-App Control Methods (Sections 2 & 5)
    // =========================================================================

    fun observeAllAppRules(): Flow<List<ManagedAppRule>> {
        val today = getTodayDateString()
        return (managedAppRuleDao?.getAllRules() ?: flowOf(emptyList())).map { list ->
            list.map { entity ->
                val rule = ManagedAppRule.fromEntity(entity)
                if (rule.lastResetDate != today) rule.copy(usedBytesToday = 0L, lastResetDate = today) else rule
            }
        }.distinctUntilChanged().flowOn(ioDispatcher)
    }

    suspend fun getAllAppRulesSync(): List<ManagedAppRule> = withContext(ioDispatcher) {
        val today = getTodayDateString()
        managedAppRuleDao?.resetDailyBytesIfNeeded(today)
        managedAppRuleDao?.getAllRulesSync()?.map { ManagedAppRule.fromEntity(it) } ?: emptyList()
    }

    suspend fun getAppRuleSync(packageName: String): ManagedAppRule? = withContext(ioDispatcher) {
        val today = getTodayDateString()
        val entity = managedAppRuleDao?.getRuleSync(packageName) ?: return@withContext null
        val rule = ManagedAppRule.fromEntity(entity)
        if (rule.lastResetDate != today) {
            rule.copy(usedBytesToday = 0L, lastResetDate = today)
        } else {
            rule
        }
    }

    suspend fun setAppFullyBlocked(packageName: String, appDisplayName: String, isBlocked: Boolean) = withContext(ioDispatcher) {
        mutex.withLock {
            val today = getTodayDateString()
            val existing = managedAppRuleDao?.getRuleSync(packageName)
            if (existing != null) {
                managedAppRuleDao.updateBlockedStatus(packageName, isBlocked)
            } else {
                managedAppRuleDao?.insertOrUpdate(
                    ManagedAppRuleEntity(
                        packageName = packageName,
                        appDisplayName = appDisplayName,
                        isFullyBlocked = isBlocked,
                        lastResetDate = today
                    )
                )
            }

            // Section 5: Log to Activity Log
            if (isBlocked) {
                logActivity(
                    eventType = ActivityLogEntry.EventType.DISCONNECTED,
                    reason = ActivityLogEntry.SpecificCutReason.APP_FULLY_BLOCKED,
                    description = "تم حظر $appDisplayName بالكامل من الإنترنت.",
                    details = packageName
                )
            } else {
                logActivity(
                    eventType = ActivityLogEntry.EventType.RESTORED,
                    reason = ActivityLogEntry.SpecificCutReason.APP_UNBLOCKED,
                    description = "تم إلغاء حظر $appDisplayName واستعادة اتصاله بالإنترنت.",
                    details = packageName
                )
            }
        }
    }

    suspend fun setAppNetworkLimit(
        packageName: String,
        appDisplayName: String,
        networkType: NetworkType,
        simSlot: Int,
        limitBytes: Long?,
        enabled: Boolean
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val today = getTodayDateString()
            val existing = managedAppRuleDao?.getRuleSync(packageName)
            if (existing != null) {
                when (networkType) {
                    NetworkType.WIFI -> managedAppRuleDao.updateWifiLimit(packageName, limitBytes, enabled)
                    NetworkType.MOBILE -> if (simSlot == 1) {
                        managedAppRuleDao.updateSim2Limit(packageName, limitBytes, enabled)
                    } else {
                        managedAppRuleDao.updateSim1Limit(packageName, limitBytes, enabled)
                    }
                    NetworkType.NONE -> {}
                }
            } else {
                val entity = when (networkType) {
                    NetworkType.WIFI -> ManagedAppRuleEntity(
                        packageName = packageName,
                        appDisplayName = appDisplayName,
                        wifiDailyLimitBytes = limitBytes,
                        wifiDailyLimitEnabled = enabled,
                        lastResetDate = today
                    )
                    NetworkType.MOBILE -> if (simSlot == 1) {
                        ManagedAppRuleEntity(
                            packageName = packageName,
                            appDisplayName = appDisplayName,
                            sim2DailyLimitBytes = limitBytes,
                            sim2DailyLimitEnabled = enabled,
                            lastResetDate = today
                        )
                    } else {
                        ManagedAppRuleEntity(
                            packageName = packageName,
                            appDisplayName = appDisplayName,
                            sim1DailyLimitBytes = limitBytes,
                            sim1DailyLimitEnabled = enabled,
                            lastResetDate = today
                        )
                    }
                    NetworkType.NONE -> ManagedAppRuleEntity(
                        packageName = packageName,
                        appDisplayName = appDisplayName,
                        lastResetDate = today
                    )
                }
                managedAppRuleDao?.insertOrUpdate(entity)
            }

            val netLabel = when (networkType) {
                NetworkType.WIFI -> "الواي فاي"
                NetworkType.MOBILE -> if (simSlot == 1) "الشريحة 2" else "الشريحة 1"
                NetworkType.NONE -> "الشبكة"
            }
            val limitStr = if (limitBytes != null) com.example.core.model.SmartUnitFormatter.formatDisplay(limitBytes) else "بدون حد"
            logActivity(
                eventType = ActivityLogEntry.EventType.CONFIG_CHANGED,
                reason = ActivityLogEntry.SpecificCutReason.LIMIT_EXPANDED,
                description = if (enabled) "تم تفعيل حد يومي ($limitStr) لشبكة $netLabel لتطبيق $appDisplayName." else "تم تعطيل الحد اليومي لشبكة $netLabel لتطبيق $appDisplayName.",
                details = packageName
            )
        }
    }

    suspend fun setAppDailyLimit(
        packageName: String,
        appDisplayName: String,
        limitBytes: Long?,
        enabled: Boolean
    ) = withContext(ioDispatcher) {
        setAppNetworkLimit(packageName, appDisplayName, NetworkType.WIFI, 0, limitBytes, enabled)
    }

    suspend fun setAppNetworkTargets(
        packageName: String,
        appDisplayName: String,
        targetWifi: Boolean,
        targetSim1: Boolean,
        targetSim2: Boolean
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val today = getTodayDateString()
            val existing = managedAppRuleDao?.getRuleSync(packageName)
            if (existing != null) {
                managedAppRuleDao.updateNetworkTargets(packageName, targetWifi, targetSim1, targetSim2)
            } else {
                managedAppRuleDao?.insertOrUpdate(
                    ManagedAppRuleEntity(
                        packageName = packageName,
                        appDisplayName = appDisplayName,
                        targetWifi = targetWifi,
                        targetSim1 = targetSim1,
                        targetSim2 = targetSim2,
                        lastResetDate = today
                    )
                )
            }
        }
    }

    suspend fun deleteAppRule(packageName: String) = withContext(ioDispatcher) {
        mutex.withLock {
            managedAppRuleDao?.deleteRule(packageName)
        }
    }

    suspend fun recordAppBytes(
        packageName: String,
        bytes: Long,
        networkType: NetworkType = NetworkType.WIFI,
        simSlot: Int = 0
    ): ManagedAppRule? = withContext(ioDispatcher) {
        val today = getTodayDateString()
        when (networkType) {
            NetworkType.WIFI -> managedAppRuleDao?.addWifiAppUsageBytes(packageName, bytes, today)
            NetworkType.MOBILE -> if (simSlot == 1) {
                managedAppRuleDao?.addSim2AppUsageBytes(packageName, bytes, today)
            } else {
                managedAppRuleDao?.addSim1AppUsageBytes(packageName, bytes, today)
            }
            NetworkType.NONE -> managedAppRuleDao?.addAppUsageBytes(packageName, bytes, today)
        }
        managedAppRuleDao?.getRuleSync(packageName)?.let { ManagedAppRule.fromEntity(it) }
    }

    suspend fun factoryResetApp() = withContext(ioDispatcher) {
        mutex.withLock {
            dailyUsageDao.clearAllDailyRecords()
            managedAppRuleDao?.clearAllRules()
            activityLogDao?.clearLogs()
            appSettingsDao.saveSettings(AppSettingsEntity())
            val today = getTodayDateString()
            dailyUsageDao.insertOrUpdate(DailyUsageEntity.fromDomain(createInitialRecordForDate(today)))
        }
    }

    private fun createInitialRecordForDate(date: String): DailyUsageRecord {
        return DailyUsageRecord(
            date = date,
            wifiRecurringLimitBytes = 0L,
            wifiRecurringEnabled = false,
            wifiTodayOverrideLimitBytes = null,
            wifiUsedBytes = 0L,
            mobileRecurringLimitBytes = 0L,
            mobileRecurringEnabled = false,
            mobileTodayOverrideLimitBytes = null,
            mobileUsedBytes = 0L,
            sim2RecurringLimitBytes = 0L,
            sim2RecurringEnabled = false,
            sim2TodayOverrideLimitBytes = null,
            sim2UsedBytes = 0L,
            killSwitchEnabled = false,
            hotspotUsedBytes = 0L,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
