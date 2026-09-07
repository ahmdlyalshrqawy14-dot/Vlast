package com.example.core.model

import com.example.core.database.entity.ManagedAppRuleEntity

/**
 * Domain model representing per-app data control rules.
 */
data class ManagedAppRule(
    val packageName: String,
    val appDisplayName: String,
    val isFullyBlocked: Boolean = false,
    val dailyLimitBytes: Long? = null,
    val dailyLimitEnabled: Boolean = false,
    val usedBytesToday: Long = 0L,
    val lastResetDate: String = "",
    val targetWifi: Boolean = true,
    val targetSim1: Boolean = true,
    val targetSim2: Boolean = true
) {
    fun appliesToNetwork(networkType: NetworkType, simSlot: Int = 0): Boolean {
        return when (networkType) {
            NetworkType.WIFI -> targetWifi
            NetworkType.MOBILE -> if (simSlot == 1) targetSim2 else targetSim1
            NetworkType.NONE -> false
        }
    }

    val networkTargetSummary: String
        get() {
            return when {
                targetWifi && targetSim1 && targetSim2 -> "جميع الشبكات"
                targetWifi && !targetSim1 && !targetSim2 -> "الواي فاي فقط"
                !targetWifi && targetSim1 && !targetSim2 -> "الشريحة 1 فقط"
                !targetWifi && !targetSim1 && targetSim2 -> "الشريحة 2 فقط"
                targetWifi && targetSim1 && !targetSim2 -> "الواي فاي والشريحة 1"
                targetWifi && !targetSim1 && targetSim2 -> "الواي فاي والشريحة 2"
                !targetWifi && targetSim1 && targetSim2 -> "الشريحة 1 والشريحة 2"
                else -> "لا ينطبق على أي شبكة"
            }
        }

    val isLimitReached: Boolean
        get() = dailyLimitEnabled && dailyLimitBytes != null && dailyLimitBytes > 0L && usedBytesToday >= dailyLimitBytes

    val remainingBytes: Long?
        get() = if (dailyLimitEnabled && dailyLimitBytes != null) {
            (dailyLimitBytes - usedBytesToday).coerceAtLeast(0L)
        } else null

    val hasActiveRule: Boolean
        get() = isFullyBlocked || (dailyLimitEnabled && dailyLimitBytes != null && dailyLimitBytes > 0L)

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
            targetSim2 = targetSim2
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
                targetSim2 = entity.targetSim2
            )
        }
    }
}
