package com.example.core.rules

import com.example.core.model.HotspotState

/**
 * Implements Section 6: Hotspot Adaptive Polling & Alert rules.
 *
 * Mandates:
 * - Measurement only, strictly NO cutting or blocking.
 * - Isolated metric (never merged with device used_bytes).
 * - Default polling interval: 5 minutes (300,000 ms).
 * - Minimum polling interval: 1 minute (60,000 ms) to safeguard battery.
 * - Moving average over last 2-3 rate checks.
 * - Alert threshold tolerance: 99% of target.
 * - 10-second debounce before final notification delivery.
 */
object HotspotAdaptiveEngine {

    const val DEFAULT_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    const val MINIMUM_INTERVAL_MS = 1 * 60 * 1000L // 1 minute mandatory floor
    const val TOLERANCE_PERCENT = 0.99             // 99% threshold tolerance
    const val NOTIFICATION_DEBOUNCE_MS = 10 * 1000L // 10 seconds debounce

    /**
     * Evaluates a new hotspot reading, updates moving average, and computes next interval & alert state.
     */
    fun processPoll(
        currentState: HotspotState,
        newHotspotBytes: Long,
        checkTimestamp: Long,
        thresholdBytes: Long?
    ): HotspotState {
        val lastTimestamp = currentState.lastCheckTimestamp
        val lastBytes = currentState.currentHotspotBytes

        val updatedRecentRates = currentState.recentRates.toMutableList()
        var movingAverage = currentState.movingAverageRateBytesPerMin

        if (lastTimestamp > 0L && checkTimestamp > lastTimestamp && newHotspotBytes >= lastBytes) {
            val deltaBytes = (newHotspotBytes - lastBytes).toDouble()
            val deltaMinutes = (checkTimestamp - lastTimestamp).toDouble() / (60.0 * 1000.0)

            if (deltaMinutes > 0.0) {
                val currentRate = deltaBytes / deltaMinutes
                updatedRecentRates.add(currentRate)
                // Retain only last 3 samples for moving average
                while (updatedRecentRates.size > 3) {
                    updatedRecentRates.removeAt(0)
                }
                movingAverage = updatedRecentRates.average()
            }
        }

        // Determine if 99% threshold tolerance is met
        var isToleranceReached = currentState.alertTriggeredToday
        var isPendingNotification = currentState.isPending10sNotification
        var pendingNotificationScheduledAt = currentState.pendingNotificationScheduledAt

        if (!currentState.alertTriggeredToday && thresholdBytes != null && thresholdBytes > 0L) {
            val toleranceThreshold = (thresholdBytes * TOLERANCE_PERCENT).toLong()
            if (newHotspotBytes >= toleranceThreshold) {
                isToleranceReached = true
                if (!isPendingNotification) {
                    isPendingNotification = true
                    pendingNotificationScheduledAt = checkTimestamp + NOTIFICATION_DEBOUNCE_MS
                }
            }
        }

        // Calculate next polling interval
        val nextIntervalMs: Long = if (thresholdBytes != null && thresholdBytes > 0L && movingAverage > 0.0) {
            val remainingBytes = (thresholdBytes - newHotspotBytes).coerceAtLeast(0L)
            val expectedMinutes = remainingBytes / movingAverage
            val expectedMs = (expectedMinutes * 60.0 * 1000.0).toLong()

            if (expectedMs > DEFAULT_INTERVAL_MS) {
                DEFAULT_INTERVAL_MS
            } else {
                // Adaptive acceleration clamped to minimum 1 minute
                expectedMs.coerceAtLeast(MINIMUM_INTERVAL_MS)
            }
        } else {
            DEFAULT_INTERVAL_MS
        }

        return currentState.copy(
            currentHotspotBytes = newHotspotBytes,
            lastCheckTimestamp = checkTimestamp,
            movingAverageRateBytesPerMin = movingAverage,
            recentRates = updatedRecentRates,
            nextScheduledIntervalMs = nextIntervalMs,
            alertThresholdBytes = thresholdBytes,
            alertTriggeredToday = isToleranceReached && (pendingNotificationScheduledAt != null && checkTimestamp >= pendingNotificationScheduledAt),
            isPending10sNotification = isPendingNotification,
            pendingNotificationScheduledAt = pendingNotificationScheduledAt
        )
    }

    /**
     * Resets rate baseline upon manual check without altering background schedule.
     */
    fun onManualRefresh(
        currentState: HotspotState,
        manualBytes: Long,
        timestamp: Long
    ): HotspotState {
        return currentState.copy(
            currentHotspotBytes = manualBytes,
            lastCheckTimestamp = timestamp,
            // Reset rates to avoid distortion from manual trigger
            recentRates = emptyList(),
            movingAverageRateBytesPerMin = 0.0
        )
    }
}
