package com.example.core.model

/**
 * Immutable historical daily record (Section 3 & Phase 2).
 *
 * Stored permanently per calendar date (YYYY-MM-DD).
 * Each record preserves:
 * - recurring_limit
 * - today_override_limit (cleared at midnight for future/new day rows)
 * - used_bytes (actual device usage)
 * - kill_switch_enabled
 * - hotspot_used_bytes (strictly independent tethering metric, no limit, no warning color)
 * - Dual SIM support (SIM 1 and SIM 2)
 */
data class DailyUsageRecord(
    val date: String,
    val wifiRecurringLimitBytes: Long = 0L,
    val wifiRecurringEnabled: Boolean = false,
    val wifiTodayOverrideLimitBytes: Long? = null,
    val wifiUsedBytes: Long = 0L,

    val mobileRecurringLimitBytes: Long = 0L,
    val mobileRecurringEnabled: Boolean = false,
    val mobileTodayOverrideLimitBytes: Long? = null,
    val mobileUsedBytes: Long = 0L,

    // Dual SIM Support (Item 13)
    val sim2RecurringLimitBytes: Long = 0L,
    val sim2RecurringEnabled: Boolean = false,
    val sim2TodayOverrideLimitBytes: Long? = null,
    val sim2UsedBytes: Long = 0L,

    val killSwitchEnabled: Boolean = false,
    val hotspotUsedBytes: Long = 0L,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Determines the active ceiling for the specified network transport.
     * Rule: today_override takes absolute precedence over recurring_limit for the active day.
     * If neither override nor recurring limit is active, returns null (unlimited).
     */
    fun getEffectiveLimit(networkType: NetworkType, simSlot: Int = 0): Long? {
        return when (networkType) {
            NetworkType.WIFI -> {
                if (wifiTodayOverrideLimitBytes != null && wifiTodayOverrideLimitBytes > 0L) {
                    wifiTodayOverrideLimitBytes
                } else if (wifiRecurringEnabled && wifiRecurringLimitBytes > 0L) {
                    wifiRecurringLimitBytes
                } else {
                    null
                }
            }
            NetworkType.MOBILE -> {
                if (simSlot == 1) {
                    if (sim2TodayOverrideLimitBytes != null && sim2TodayOverrideLimitBytes > 0L) {
                        sim2TodayOverrideLimitBytes
                    } else if (sim2RecurringEnabled && sim2RecurringLimitBytes > 0L) {
                        sim2RecurringLimitBytes
                    } else {
                        null
                    }
                } else {
                    if (mobileTodayOverrideLimitBytes != null && mobileTodayOverrideLimitBytes > 0L) {
                        mobileTodayOverrideLimitBytes
                    } else if (mobileRecurringEnabled && mobileRecurringLimitBytes > 0L) {
                        mobileRecurringLimitBytes
                    } else {
                        null
                    }
                }
            }
            NetworkType.NONE -> null
        }
    }

    /**
     * Returns the consumed bytes for the specified network transport.
     */
    fun getUsedBytes(networkType: NetworkType, simSlot: Int = 0): Long {
        return when (networkType) {
            NetworkType.WIFI -> wifiUsedBytes
            NetworkType.MOBILE -> if (simSlot == 1) sim2UsedBytes else mobileUsedBytes
            NetworkType.NONE -> 0L
        }
    }

    /**
     * Evaluates remaining quota in bytes.
     * Returns null if unconstrained/unlimited.
     */
    fun getRemainingBytes(networkType: NetworkType, simSlot: Int = 0): Long? {
        val limit = getEffectiveLimit(networkType, simSlot) ?: return null
        val used = getUsedBytes(networkType, simSlot)
        return (limit - used).coerceAtLeast(0L)
    }

    /**
     * Returns whether the active limit has been hit or exceeded for this transport.
     */
    fun isLimitReached(networkType: NetworkType, simSlot: Int = 0): Boolean {
        if (killSwitchEnabled) return true
        val limit = getEffectiveLimit(networkType, simSlot) ?: return false
        return getUsedBytes(networkType, simSlot) >= limit
    }
}
