package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Section 2: Room Entity for per-app rules table "managed_app_rules".
 * - packageName: Unique package name (Primary Key)
 * - appDisplayName: Display name of the application
 * - isFullyBlocked: Permanent total block flag
 * - dailyLimitBytes: Dedicated daily quota for this app (nullable)
 * - dailyLimitEnabled: Whether daily limit is enabled
 * - usedBytesToday: Actual consumption today (resets at midnight when lastResetDate changes)
 * - lastResetDate: Date string (YYYY-MM-DD) of last daily reset
 */
@Entity(tableName = "managed_app_rules")
data class ManagedAppRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "packageName")
    val packageName: String,

    @ColumnInfo(name = "appDisplayName")
    val appDisplayName: String,

    @ColumnInfo(name = "isFullyBlocked")
    val isFullyBlocked: Boolean = false,

    @ColumnInfo(name = "dailyLimitBytes")
    val dailyLimitBytes: Long? = null,

    @ColumnInfo(name = "dailyLimitEnabled")
    val dailyLimitEnabled: Boolean = false,

    @ColumnInfo(name = "usedBytesToday")
    val usedBytesToday: Long = 0L,

    @ColumnInfo(name = "lastResetDate")
    val lastResetDate: String
)
