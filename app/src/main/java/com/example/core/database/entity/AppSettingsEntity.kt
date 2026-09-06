package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted state and configuration for Vlast:
 * - Recurring limits (preserved across days)
 * - Kill switch active flag
 * - Hotspot alert preferences
 * - Monitoring service intent flag
 * - Dual SIM independent limit configuration (Item 13)
 * - Settings security lock PIN / biometric (Item 16)
 * - First run walkthrough completed flag (Item 5 & 14)
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "kill_switch_active")
    val killSwitchActive: Boolean = false,

    @ColumnInfo(name = "wifi_recurring_limit_bytes")
    val wifiRecurringLimitBytes: Long = 0L,

    @ColumnInfo(name = "wifi_recurring_enabled")
    val wifiRecurringEnabled: Boolean = false,

    @ColumnInfo(name = "mobile_recurring_limit_bytes")
    val mobileRecurringLimitBytes: Long = 0L,

    @ColumnInfo(name = "mobile_recurring_enabled")
    val mobileRecurringEnabled: Boolean = false,

    // Dual SIM support (Item 13)
    @ColumnInfo(name = "sim2_recurring_limit_bytes")
    val sim2RecurringLimitBytes: Long = 0L,

    @ColumnInfo(name = "sim2_recurring_enabled")
    val sim2RecurringEnabled: Boolean = false,

    @ColumnInfo(name = "active_sim_slot")
    val activeSimSlot: Int = 0, // 0 for SIM 1, 1 for SIM 2

    @ColumnInfo(name = "hotspot_alert_threshold_bytes")
    val hotspotAlertThresholdBytes: Long? = null,

    @ColumnInfo(name = "hotspot_alert_enabled")
    val hotspotAlertEnabled: Boolean = false,

    @ColumnInfo(name = "is_monitoring_active")
    val isMonitoringActive: Boolean = false,

    @ColumnInfo(name = "last_calibration_timestamp")
    val lastCalibrationTimestamp: Long = 0L,

    // Settings Security Lock (Item 16)
    @ColumnInfo(name = "settings_lock_enabled")
    val settingsLockEnabled: Boolean = false,

    @ColumnInfo(name = "settings_lock_pin")
    val settingsLockPin: String? = null,

    // First Run Walkthrough state (Items 5 & 14)
    @ColumnInfo(name = "first_run_completed")
    val firstRunCompleted: Boolean = false,

    // Cutoff sound alert (Section 2)
    @ColumnInfo(name = "sound_alert_enabled")
    val soundAlertEnabled: Boolean = true,

    // Color-blind support mode (Section 8)
    @ColumnInfo(name = "color_blind_mode_enabled")
    val colorBlindModeEnabled: Boolean = false
)
