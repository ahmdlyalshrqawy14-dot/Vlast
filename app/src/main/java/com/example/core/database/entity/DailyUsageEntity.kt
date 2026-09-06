package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.DailyUsageRecord

/**
 * Historical daily record stored permanently in Room (Section 3).
 * Represents one calendar day (date YYYY-MM-DD).
 */
@Entity(tableName = "daily_usage_records")
data class DailyUsageEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "wifi_recurring_limit_bytes")
    val wifiRecurringLimitBytes: Long = 0L,

    @ColumnInfo(name = "wifi_recurring_enabled")
    val wifiRecurringEnabled: Boolean = false,

    @ColumnInfo(name = "wifi_today_override_limit_bytes")
    val wifiTodayOverrideLimitBytes: Long? = null,

    @ColumnInfo(name = "wifi_used_bytes")
    val wifiUsedBytes: Long = 0L,

    @ColumnInfo(name = "mobile_recurring_limit_bytes")
    val mobileRecurringLimitBytes: Long = 0L,

    @ColumnInfo(name = "mobile_recurring_enabled")
    val mobileRecurringEnabled: Boolean = false,

    @ColumnInfo(name = "mobile_today_override_limit_bytes")
    val mobileTodayOverrideLimitBytes: Long? = null,

    @ColumnInfo(name = "mobile_used_bytes")
    val mobileUsedBytes: Long = 0L,

    // Dual SIM usage (Item 13)
    @ColumnInfo(name = "sim2_recurring_limit_bytes")
    val sim2RecurringLimitBytes: Long = 0L,

    @ColumnInfo(name = "sim2_recurring_enabled")
    val sim2RecurringEnabled: Boolean = false,

    @ColumnInfo(name = "sim2_today_override_limit_bytes")
    val sim2TodayOverrideLimitBytes: Long? = null,

    @ColumnInfo(name = "sim2_used_bytes")
    val sim2UsedBytes: Long = 0L,

    @ColumnInfo(name = "kill_switch_enabled")
    val killSwitchEnabled: Boolean = false,

    @ColumnInfo(name = "hotspot_used_bytes")
    val hotspotUsedBytes: Long = 0L,

    @ColumnInfo(name = "last_updated_timestamp")
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): DailyUsageRecord {
        return DailyUsageRecord(
            date = date,
            wifiRecurringLimitBytes = wifiRecurringLimitBytes,
            wifiRecurringEnabled = wifiRecurringEnabled,
            wifiTodayOverrideLimitBytes = wifiTodayOverrideLimitBytes,
            wifiUsedBytes = wifiUsedBytes,
            mobileRecurringLimitBytes = mobileRecurringLimitBytes,
            mobileRecurringEnabled = mobileRecurringEnabled,
            mobileTodayOverrideLimitBytes = mobileTodayOverrideLimitBytes,
            mobileUsedBytes = mobileUsedBytes,
            sim2RecurringLimitBytes = sim2RecurringLimitBytes,
            sim2RecurringEnabled = sim2RecurringEnabled,
            sim2TodayOverrideLimitBytes = sim2TodayOverrideLimitBytes,
            sim2UsedBytes = sim2UsedBytes,
            killSwitchEnabled = killSwitchEnabled,
            hotspotUsedBytes = hotspotUsedBytes,
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
    }

    companion object {
        fun fromDomain(domain: DailyUsageRecord): DailyUsageEntity {
            return DailyUsageEntity(
                date = domain.date,
                wifiRecurringLimitBytes = domain.wifiRecurringLimitBytes,
                wifiRecurringEnabled = domain.wifiRecurringEnabled,
                wifiTodayOverrideLimitBytes = domain.wifiTodayOverrideLimitBytes,
                wifiUsedBytes = domain.wifiUsedBytes,
                mobileRecurringLimitBytes = domain.mobileRecurringLimitBytes,
                mobileRecurringEnabled = domain.mobileRecurringEnabled,
                mobileTodayOverrideLimitBytes = domain.mobileTodayOverrideLimitBytes,
                mobileUsedBytes = domain.mobileUsedBytes,
                sim2RecurringLimitBytes = domain.sim2RecurringLimitBytes,
                sim2RecurringEnabled = domain.sim2RecurringEnabled,
                sim2TodayOverrideLimitBytes = domain.sim2TodayOverrideLimitBytes,
                sim2UsedBytes = domain.sim2UsedBytes,
                killSwitchEnabled = domain.killSwitchEnabled,
                hotspotUsedBytes = domain.hotspotUsedBytes,
                lastUpdatedTimestamp = domain.lastUpdatedTimestamp
            )
        }
    }
}
