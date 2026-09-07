package com.example.core.network

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

/**
 * Official Android NetworkStatsManager query helper (Section 1, 6 & 8).
 * Provides:
 * 1. Parallel verification & calibration for tunnel counters (Wi-Fi & Mobile).
 * 2. Independent tethering/hotspot measurement via TYPE_TETHERING / UID_TETHERING.
 */
class NetworkStatsHelper(private val context: Context) {

    private val networkStatsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Start of today in epoch milliseconds.
     */
    fun getTodayStartEpochMs(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * End of current window (now) in epoch milliseconds.
     */
    fun getCurrentEpochMs(): Long = System.currentTimeMillis()

    /**
     * Measures total Tethering / Hotspot consumption today in bytes.
     * Strictly isolated from device counters (Section 6).
     */
    fun queryHotspotBytesToday(): Long {
        if (!hasUsageStatsPermission()) return 0L
        val nsm = networkStatsManager ?: return 0L
        val startTime = getTodayStartEpochMs()
        val endTime = getCurrentEpochMs()
        var totalBytes = 0L

        try {
            // Method 1: Query by UID_TETHERING (-5) across Mobile and Wi-Fi networks
            val uidTethering = -5 // NetworkStats.Bucket.UID_TETHERING
            listOf(ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_WIFI).forEach { netType ->
                var stats: NetworkStats? = null
                try {
                    stats = nsm.queryDetailsForUid(netType, null, startTime, endTime, uidTethering)
                    val bucket = NetworkStats.Bucket()
                    while (stats?.hasNextBucket() == true) {
                        stats.getNextBucket(bucket)
                        totalBytes += bucket.rxBytes + bucket.txBytes
                    }
                } catch (_: Exception) {
                } finally {
                    stats?.close()
                }
            }

            // Method 2: Query summary for TYPE_TETHERING (4) if UID query returned 0
            if (totalBytes == 0L) {
                val tetheringType = 4 // ConnectivityManager.TYPE_TETHERING
                var tetheringStats: NetworkStats? = null
                try {
                    tetheringStats = nsm.querySummary(tetheringType, null, startTime, endTime)
                    val bucket = NetworkStats.Bucket()
                    while (tetheringStats?.hasNextBucket() == true) {
                        tetheringStats.getNextBucket(bucket)
                        totalBytes += bucket.rxBytes + bucket.txBytes
                    }
                } catch (_: Exception) {
                } finally {
                    tetheringStats?.close()
                }
            }
        } catch (_: SecurityException) {
            // Usage stats permission not yet granted
            return 0L
        } catch (_: Exception) {
            return 0L
        }

        return totalBytes
    }

    /**
     * Queries total device Wi-Fi bytes today for calibration.
     */
    fun queryWifiDeviceBytesToday(): Long {
        if (!hasUsageStatsPermission()) return 0L
        val nsm = networkStatsManager ?: return 0L
        val startTime = getTodayStartEpochMs()
        val endTime = getCurrentEpochMs()
        var totalBytes = 0L

        try {
            val bucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            totalBytes = bucket.rxBytes + bucket.txBytes
        } catch (_: Exception) {
        }
        return totalBytes
    }

    /**
     * Queries total device Mobile bytes today for calibration.
     */
    fun queryMobileDeviceBytesToday(): Long {
        if (!hasUsageStatsPermission()) return 0L
        val nsm = networkStatsManager ?: return 0L
        val startTime = getTodayStartEpochMs()
        val endTime = getCurrentEpochMs()
        var totalBytes = 0L

        try {
            val bucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
            totalBytes = bucket.rxBytes + bucket.txBytes
        } catch (_: Exception) {
        }
        return totalBytes
    }
}
