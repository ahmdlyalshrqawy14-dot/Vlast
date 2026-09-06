package com.example.core.model

/**
 * Encapsulates the runtime state of the smart adaptive hotspot polling engine.
 */
data class HotspotState(
    val currentHotspotBytes: Long = 0L,
    val lastCheckTimestamp: Long = 0L,
    val movingAverageRateBytesPerMin: Double = 0.0,
    val recentRates: List<Double> = emptyList(),
    val nextScheduledIntervalMs: Long = 5 * 60 * 1000L, // Default: 5 minutes
    val alertThresholdBytes: Long? = null,
    val alertTriggeredToday: Boolean = false,
    val isPending10sNotification: Boolean = false,
    val pendingNotificationScheduledAt: Long? = null
)
