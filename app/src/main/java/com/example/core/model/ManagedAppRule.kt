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
    val lastResetDate: String = ""
) {
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
            lastResetDate = lastResetDate
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
                lastResetDate = entity.lastResetDate
            )
        }
    }
}
