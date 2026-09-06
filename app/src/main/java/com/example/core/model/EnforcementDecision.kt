package com.example.core.model

/**
 * Result of evaluating active traffic against Vlast's 3 independent control mechanisms:
 * 1. Manual Full Kill Switch (Highest priority)
 * 2. Active Daily Limit (Today Override or Recurring Limit)
 * 3. Traffic Allowed
 */
sealed class EnforcementDecision {
    /**
     * Manual Full Kill Switch is active: unconditionally blocks all data (Wi-Fi & Mobile).
     */
    data object BlockedKillSwitch : EnforcementDecision()

    /**
     * Effective limit reached: demands clean session termination.
     */
    data class BlockedLimitExceeded(
        val networkType: NetworkType,
        val usedBytes: Long,
        val limitBytes: Long
    ) : EnforcementDecision()

    /**
     * Section 3 (Priority 2): Application is individually fully blocked.
     */
    data class BlockedAppFull(
        val packageName: String,
        val appName: String
    ) : EnforcementDecision()

    /**
     * Section 3 (Priority 4): Application exceeded its dedicated daily quota.
     */
    data class BlockedAppLimitExceeded(
        val packageName: String,
        val appName: String,
        val usedBytes: Long,
        val limitBytes: Long
    ) : EnforcementDecision()

    /**
     * Data passage authorized.
     */
    data class Allowed(
        val networkType: NetworkType,
        val remainingBytes: Long?
    ) : EnforcementDecision()
}
