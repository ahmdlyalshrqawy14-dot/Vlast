package com.example.core.model

import com.example.core.database.entity.ManagedAppRuleEntity

/**
 * Domain model representing per-app data control rules.
 */
data class ManagedAppRule(
    val packageName: String,
    val appDisplayName: String,
    val isFullyBlocked: Boolean = false,
    val lastResetDate: String = "",
    // Wi-Fi daily limit
    val wifiDailyLimitBytes: Long? = null,
    val wifiDailyLimitEnabled: Boolean = false,
    val wifiUsedBytesToday: Long = 0L,
    // SIM 1 daily limit
    val sim1DailyLimitBytes: Long? = null,
    val sim1DailyLimitEnabled: Boolean = false,
    val sim1UsedBytesToday: Long = 0L,
    // SIM 2 daily limit
    val sim2DailyLimitBytes: Long? = null,
    val sim2DailyLimitEnabled: Boolean = false,
    val sim2UsedBytesToday: Long = 0L,
    // Target flags (compatibility)
    val targetWifi: Boolean = true,
    val targetSim1: Boolean = true,
    val targetSim2: Boolean = true,
    // Legacy fields (optional fallback)
    val dailyLimitBytes: Long? = null,
    val dailyLimitEnabled: Boolean = false,
    val usedBytesToday: Long = 0L
) {
    fun appliesToNetwork(networkType: NetworkType, simSlot: Int = 0): Boolean {
        return when (networkType) {
            NetworkType.WIFI -> targetWifi
            NetworkType.MOBILE -> if (simSlot == 1) targetSim2 else targetSim1
            NetworkType.NONE -> false
        }
    }

    fun getLimitForNetwork(networkType: NetworkType, simSlot: Int = 0): Long? {
        return when (networkType) {
            NetworkType.WIFI -> if (wifiDailyLimitEnabled) wifiDailyLimitBytes else if (dailyLimitEnabled && targetWifi) dailyLimitBytes else null
            NetworkType.MOBILE -> if (simSlot == 1) {
                if (sim2DailyLimitEnabled) sim2DailyLimitBytes else if (dailyLimitEnabled && targetSim2) dailyLimitBytes else null
            } else {
                if (sim1DailyLimitEnabled) sim1DailyLimitBytes else if (dailyLimitEnabled && targetSim1) dailyLimitBytes else null
            }
            NetworkType.NONE -> null
        }
    }

    fun getUsedBytesForNetwork(networkType: NetworkType, simSlot: Int = 0): Long {
        return when (networkType) {
            NetworkType.WIFI -> wifiUsedBytesToday
            NetworkType.MOBILE -> if (simSlot == 1) sim2UsedBytesToday else sim1UsedBytesToday
            NetworkType.NONE -> 0L
        }
    }

    fun isLimitReachedForNetwork(networkType: NetworkType, simSlot: Int = 0): Boolean {
        if (isFullyBlocked) return true
        val limit = getLimitForNetwork(networkType, simSlot) ?: return false
        val used = getUsedBytesForNetwork(networkType, simSlot)
        return limit > 0L && used >= limit
    }

    fun getRemainingBytesForNetwork(networkType: NetworkType, simSlot: Int = 0): Long? {
        val limit = getLimitForNetwork(networkType, simSlot) ?: return null
        val used = getUsedBytesForNetwork(networkType, simSlot)
        return (limit - used).coerceAtLeast(0L)
    }

    val isLimitReached: Boolean
        get() = isFullyBlocked || isLimitReachedForNetwork(NetworkType.WIFI, 0) || isLimitReachedForNetwork(NetworkType.MOBILE, 0) || isLimitReachedForNetwork(NetworkType.MOBILE, 1)

    val hasActiveRule: Boolean
        get() = isFullyBlocked || wifiDailyLimitEnabled || sim1DailyLimitEnabled || sim2DailyLimitEnabled || (dailyLimitEnabled && dailyLimitBytes != null && dailyLimitBytes > 0L)

    fun toEntity(): ManagedAppRuleEntity {
        return ManagedAppRuleEntity(
            packageName = packageName,
            appDisplayName = appDisplayName,
            isFullyBlocked = isFullyBlocked,
            dailyLimitBytes = dailyLimitBytes,
            dailyLimitEnabled = dailyLimitEnabled,
            usedBytesToday = usedBytesToday,
            lastResetDate = lastResetDate,
            targetWifi = targetWifi,
            targetSim1 = targetSim1,
            targetSim2 = targetSim2,
            wifiDailyLimitBytes = wifiDailyLimitBytes,
            wifiDailyLimitEnabled = wifiDailyLimitEnabled,
            wifiUsedBytesToday = wifiUsedBytesToday,
            sim1DailyLimitBytes = sim1DailyLimitBytes,
            sim1DailyLimitEnabled = sim1DailyLimitEnabled,
            sim1UsedBytesToday = sim1UsedBytesToday,
            sim2DailyLimitBytes = sim2DailyLimitBytes,
            sim2DailyLimitEnabled = sim2DailyLimitEnabled,
            sim2UsedBytesToday = sim2UsedBytesToday
        )
    }

    companion object {
        fun fromEntity(entity: ManagedAppRuleEntity): ManagedAppRule {
            return ManagedAppRule(
                packageName = entity.packageName,
                appDisplayName = entity.appDisplayName,
                isFullyBlocked = entity.isFullyBlocked,
                dailyLimitBytes = entity.dailyLimitBytes,
                dailyLimitEnabled = entity.dailyLimitEnabled,
                usedBytesToday = entity.usedBytesToday,
                lastResetDate = entity.lastResetDate,
                targetWifi = entity.targetWifi,
                targetSim1 = entity.targetSim1,
                targetSim2 = entity.targetSim2,
                wifiDailyLimitBytes = entity.wifiDailyLimitBytes,
                wifiDailyLimitEnabled = entity.wifiDailyLimitEnabled,
                wifiUsedBytesToday = entity.wifiUsedBytesToday,
                sim1DailyLimitBytes = entity.sim1DailyLimitBytes,
                sim1DailyLimitEnabled = entity.sim1DailyLimitEnabled,
                sim1UsedBytesToday = entity.sim1UsedBytesToday,
                sim2DailyLimitBytes = entity.sim2DailyLimitBytes,
                sim2DailyLimitEnabled = entity.sim2DailyLimitEnabled,
                sim2UsedBytesToday = entity.sim2UsedBytesToday
            )
        }
    }
}
