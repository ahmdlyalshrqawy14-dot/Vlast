package com.example

import com.example.core.model.DailyUsageRecord
import com.example.core.model.EnforcementDecision
import com.example.core.model.HotspotState
import com.example.core.model.NetworkType
import com.example.core.rules.EnforcementRulesEngine
import com.example.core.rules.HotspotAdaptiveEngine
import org.junit.Assert.*
import org.junit.Test

class VlastCoreRulesTest {

    @Test
    fun testKillSwitchHasAbsolutePriority() {
        val recordWithRemainingQuota = DailyUsageRecord(
            date = "2026-09-06",
            wifiRecurringLimitBytes = 1000L,
            wifiRecurringEnabled = true,
            wifiUsedBytes = 100L,
            killSwitchEnabled = true
        )

        val decisionWifi = EnforcementRulesEngine.evaluate(
            record = recordWithRemainingQuota,
            networkType = NetworkType.WIFI,
            killSwitchOverride = false
        )
        assertTrue("Kill switch must block Wi-Fi even with remaining quota", decisionWifi is EnforcementDecision.BlockedKillSwitch)

        val decisionMobile = EnforcementRulesEngine.evaluate(
            record = recordWithRemainingQuota,
            networkType = NetworkType.MOBILE,
            killSwitchOverride = false
        )
        assertTrue("Kill switch must block Mobile immediately", decisionMobile is EnforcementDecision.BlockedKillSwitch)
    }

    @Test
    fun testTodayOverridePrecedenceOverRecurringLimit() {
        // Recurring limit: 500MB, Today Override: 200MB, Used: 250MB
        val record = DailyUsageRecord(
            date = "2026-09-06",
            wifiRecurringLimitBytes = 500L,
            wifiRecurringEnabled = true,
            wifiTodayOverrideLimitBytes = 200L,
            wifiUsedBytes = 250L
        )

        val decision = EnforcementRulesEngine.evaluate(
            record = record,
            networkType = NetworkType.WIFI
        )

        assertTrue(decision is EnforcementDecision.BlockedLimitExceeded)
        val blocked = decision as EnforcementDecision.BlockedLimitExceeded
        assertEquals(200L, blocked.limitBytes)
        assertEquals(250L, blocked.usedBytes)
    }

    @Test
    fun testAllowedWhenUnderLimit() {
        val record = DailyUsageRecord(
            date = "2026-09-06",
            mobileRecurringLimitBytes = 1000L,
            mobileRecurringEnabled = true,
            mobileUsedBytes = 400L
        )

        val decision = EnforcementRulesEngine.evaluate(
            record = record,
            networkType = NetworkType.MOBILE
        )

        assertTrue(decision is EnforcementDecision.Allowed)
        val allowed = decision as EnforcementDecision.Allowed
        assertEquals(600L, allowed.remainingBytes)
    }

    @Test
    fun testCleanTerminationPacketGeneratesValidTcpRst() {
        // Construct a mock IPv4 TCP SYN packet from 10.0.0.2:12345 to 93.184.216.34:80
        val mockPacket = ByteArray(40)
        mockPacket[0] = 0x45.toByte() // Version 4, IHL 5 (20 bytes)
        mockPacket[9] = 6.toByte()    // Protocol TCP
        // Src IP: 10.0.0.2
        mockPacket[12] = 10.toByte(); mockPacket[13] = 0; mockPacket[14] = 0; mockPacket[15] = 2
        // Dst IP: 93.184.216.34
        mockPacket[16] = 93.toByte(); mockPacket[17] = 184.toByte(); mockPacket[18] = 216.toByte(); mockPacket[19] = 34.toByte()
        // TCP Src Port: 12345 (0x3039)
        mockPacket[20] = 0x30.toByte(); mockPacket[21] = 0x39.toByte()
        // TCP Dst Port: 80 (0x0050)
        mockPacket[22] = 0x00.toByte(); mockPacket[23] = 0x50.toByte()
        // TCP Flags: SYN (0x02) at offset 33
        mockPacket[33] = 0x02.toByte()

        val rstPacket = EnforcementRulesEngine.generateCleanTerminationPacket(mockPacket, mockPacket.size)
        assertNotNull("Must generate clean termination packet", rstPacket)
        assertEquals(40, rstPacket!!.size)

        // Verify inverted ports: Src port must be 80, Dst port must be 12345
        val rstSrcPort = ((rstPacket[20].toInt() and 0xFF) shl 8) or (rstPacket[21].toInt() and 0xFF)
        val rstDstPort = ((rstPacket[22].toInt() and 0xFF) shl 8) or (rstPacket[23].toInt() and 0xFF)
        assertEquals(80, rstSrcPort)
        assertEquals(12345, rstDstPort)

        // Verify RST flag (either 0x14 RST+ACK or 0x04 RST)
        val flags = rstPacket[33].toInt() and 0xFF
        assertTrue("RST flag must be set in clean termination packet", (flags and 0x04) != 0)
    }

    @Test
    fun testHotspotAdaptivePollingMinimumOneMinuteFloor() {
        val threshold = 1_000_000L // 1MB
        val state0 = HotspotState(
            currentHotspotBytes = 0L,
            lastCheckTimestamp = 1_000_000L
        )

        // Extreme rapid consumption: 900,000 bytes in 1 minute -> rate = 900,000 bytes/min
        val state1 = HotspotAdaptiveEngine.processPoll(
            currentState = state0,
            newHotspotBytes = 900_000L,
            checkTimestamp = 1_000_000L + 60_000L,
            thresholdBytes = threshold
        )

        // Remaining = 100,000 bytes. Expected time = 100,000 / 900,000 = ~0.11 minutes (~6.6 seconds)
        // Must clamp to mandatory floor of 1 minute (60,000 ms)!
        assertEquals("Must clamp polling interval to minimum 1 minute (60,000ms)", 60_000L, state1.nextScheduledIntervalMs)
    }

    @Test
    fun testHotspot99PercentToleranceAndDebounce() {
        val threshold = 1_000_000L
        val tolerance99Percent = 990_000L // 99% of 1,000,000

        val state0 = HotspotState(
            currentHotspotBytes = 500_000L,
            lastCheckTimestamp = 100_000L
        )

        val checkTime = 200_000L
        val state1 = HotspotAdaptiveEngine.processPoll(
            currentState = state0,
            newHotspotBytes = tolerance99Percent,
            checkTimestamp = checkTime,
            thresholdBytes = threshold
        )

        assertTrue("Tolerance (99%) must trigger pending notification", state1.isPending10sNotification)
        assertEquals("Notification scheduled with 10s debounce", checkTime + 10_000L, state1.pendingNotificationScheduledAt)
    }

    @Test
    fun testHotspotManualRefreshResetsRateBaseline() {
        val stateWithHistory = HotspotState(
            currentHotspotBytes = 500_000L,
            lastCheckTimestamp = 100_000L,
            movingAverageRateBytesPerMin = 25_000.0,
            recentRates = listOf(20_000.0, 30_000.0)
        )

        val manualTime = 120_000L
        val refreshed = HotspotAdaptiveEngine.onManualRefresh(
            currentState = stateWithHistory,
            manualBytes = 550_000L,
            timestamp = manualTime
        )

        assertEquals(550_000L, refreshed.currentHotspotBytes)
        assertEquals(manualTime, refreshed.lastCheckTimestamp)
        assertTrue("Manual refresh must reset moving average rates to avoid distortion", refreshed.recentRates.isEmpty())
        assertEquals(0.0, refreshed.movingAverageRateBytesPerMin, 0.001)
    }
}
