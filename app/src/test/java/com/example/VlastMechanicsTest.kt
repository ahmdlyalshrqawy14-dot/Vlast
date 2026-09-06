package com.example

import android.content.Context
import com.example.core.model.MeterProgressStatus
import com.example.core.model.NetworkType
import com.example.core.model.ServicePriorityStatus
import com.example.core.model.SmartUnitFormatter
import org.junit.Assert.*
import org.junit.Test

class VlastMechanicsTest {

    @Test
    fun testSmartUnitFormatterMbAndGb() {
        // 500 MB
        val bytes500Mb = 500L * 1024L * 1024L
        val formatted500Mb = SmartUnitFormatter.format(bytes500Mb)
        assertEquals("MB", formatted500Mb.unit)
        assertEquals("500", formatted500Mb.amount)

        // 2.5 GB
        val bytes2Point5Gb = (2.5 * 1024 * 1024 * 1024).toLong()
        val formattedGb = SmartUnitFormatter.format(bytes2Point5Gb)
        assertEquals("GB", formattedGb.unit)
        assertEquals("2.50", formattedGb.amount)

        // Input parsing
        val parsedMb = SmartUnitFormatter.parseInputToBytes("750", "MB")
        assertEquals(750L * 1024L * 1024L, parsedMb)

        val parsedGb = SmartUnitFormatter.parseInputToBytes("1.5 GB", "MB")
        assertEquals((1.5 * 1024 * 1024 * 1024).toLong(), parsedGb)
    }

    @Test
    fun testMeterProgressStatusColorGradation() {
        // Less than 70% -> GREEN
        val statusGreen = MeterProgressStatus.fromUsage(usedBytes = 600L, limitBytes = 1000L)
        assertEquals(MeterProgressStatus.GREEN, statusGreen)

        // 70% to 90% -> YELLOW
        val statusYellow = MeterProgressStatus.fromUsage(usedBytes = 750L, limitBytes = 1000L)
        assertEquals(MeterProgressStatus.YELLOW, statusYellow)

        // Greater than 90% -> RED
        val statusRed = MeterProgressStatus.fromUsage(usedBytes = 950L, limitBytes = 1000L)
        assertEquals(MeterProgressStatus.RED, statusRed)

        // Breached limit -> RED
        val statusExceeded = MeterProgressStatus.fromUsage(usedBytes = 1200L, limitBytes = 1000L)
        assertEquals(MeterProgressStatus.RED, statusExceeded)
    }

    @Test
    fun testPriorityResolutionOrdering() {
        // Verify priority levels: KillSwitch (1) > Today (2) > Recurring (3) > Unlimited (4)
        assertTrue(ServicePriorityStatus.KillSwitchInControl.priorityLevel < 2)
        val todayStatus = ServicePriorityStatus.TodayOverrideInControl("Wi-Fi", "1 GB")
        val recurringStatus = ServicePriorityStatus.RecurringLimitInControl("Wi-Fi", "500 MB")
        assertTrue(todayStatus.priorityLevel < recurringStatus.priorityLevel)
        assertTrue(recurringStatus.priorityLevel < ServicePriorityStatus.UnlimitedInControl.priorityLevel)
    }
}
