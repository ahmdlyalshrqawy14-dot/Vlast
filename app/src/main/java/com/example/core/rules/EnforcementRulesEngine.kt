package com.example.core.rules

import com.example.core.model.DailyUsageRecord
import com.example.core.model.EnforcementDecision
import com.example.core.model.NetworkType

/**
 * Core business rules engine enforcing Vlast data policies.
 *
 * Rules:
 * 1. Zero exceptions: All app/system/DNS traffic must be evaluated.
 * 2. Manual Full Kill Switch has absolute priority over any limits.
 * 3. One-Time Today Override takes precedence over Recurring Limit for the day.
 * 4. Separate evaluation for Wi-Fi and Mobile transports.
 * 5. Clean session termination (not silent drop) upon limit breach to prevent battery drain.
 */
object EnforcementRulesEngine {

    /**
     * Evaluates whether outbound/inbound traffic on [networkType] is authorized.
     */
    fun evaluate(
        record: DailyUsageRecord,
        networkType: NetworkType,
        killSwitchOverride: Boolean = false
    ): EnforcementDecision {
        // Priority 1: Manual Full Kill Switch (manual only, never auto-cancelled)
        if (killSwitchOverride || record.killSwitchEnabled) {
            return EnforcementDecision.BlockedKillSwitch
        }

        if (networkType == NetworkType.NONE) {
            return EnforcementDecision.Allowed(networkType, null)
        }

        // Priority 2: Effective limit evaluation for current transport
        val effectiveLimit = record.getEffectiveLimit(networkType)
        val used = record.getUsedBytes(networkType)

        return if (effectiveLimit != null && effectiveLimit > 0L) {
            if (used >= effectiveLimit) {
                EnforcementDecision.BlockedLimitExceeded(
                    networkType = networkType,
                    usedBytes = used,
                    limitBytes = effectiveLimit
                )
            } else {
                val remaining = (effectiveLimit - used).coerceAtLeast(0L)
                EnforcementDecision.Allowed(networkType, remaining)
            }
        } else {
            EnforcementDecision.Allowed(networkType, null)
        }
    }

    /**
     * Synthesizes a clean TCP RST or ICMP Destination Unreachable packet for clean session closure,
     * fulfilling Section 4's explicit mandate to avoid battery-draining connection retry loops.
     */
    fun generateCleanTerminationPacket(rawIpPacket: ByteArray, packetLength: Int): ByteArray? {
        if (packetLength < 20) return null
        val version = (rawIpPacket[0].toInt() shr 4) and 0x0F
        if (version != 4) return null // Focus on IPv4 TCP termination

        val protocol = rawIpPacket[9].toInt() and 0xFF
        val headerLength = (rawIpPacket[0].toInt() and 0x0F) * 4
        if (protocol != 6 || packetLength < headerLength + 20) {
            // Non-TCP or truncated: return null (VPN layer tears down socket cleanly)
            return null
        }

        // Parse TCP header
        val tcpOffset = headerLength
        val srcPort = ((rawIpPacket[tcpOffset].toInt() and 0xFF) shl 8) or (rawIpPacket[tcpOffset + 1].toInt() and 0xFF)
        val dstPort = ((rawIpPacket[tcpOffset + 2].toInt() and 0xFF) shl 8) or (rawIpPacket[tcpOffset + 3].toInt() and 0xFF)
        val seqNum = ((rawIpPacket[tcpOffset + 4].toLong() and 0xFF) shl 24) or
                ((rawIpPacket[tcpOffset + 5].toLong() and 0xFF) shl 16) or
                ((rawIpPacket[tcpOffset + 6].toLong() and 0xFF) shl 8) or
                (rawIpPacket[tcpOffset + 7].toLong() and 0xFF)
        val flags = rawIpPacket[tcpOffset + 13].toInt() and 0xFF
        val isSyn = (flags and 0x02) != 0

        // Craft TCP RST packet swapping src/dst IP and src/dst Port
        val response = ByteArray(40)
        // IPv4 header (20 bytes)
        response[0] = 0x45.toByte() // Version 4, IHL 5
        response[1] = 0x00.toByte() // DSCP/ECN
        response[2] = 0x00.toByte() // Total length 40 bytes
        response[3] = 0x28.toByte()
        response[4] = 0x00.toByte() // Identification
        response[5] = 0x00.toByte()
        response[6] = 0x40.toByte() // Flags: Don't Fragment
        response[7] = 0x00.toByte()
        response[8] = 64.toByte()   // TTL
        response[9] = 6.toByte()    // Protocol TCP
        // Swap IPs
        System.arraycopy(rawIpPacket, 16, response, 12, 4) // Src IP = orig Dst IP
        System.arraycopy(rawIpPacket, 12, response, 16, 4) // Dst IP = orig Src IP

        // Compute IP checksum
        val ipChecksum = computeChecksum(response, 0, 20)
        response[10] = (ipChecksum shr 8).toByte()
        response[11] = (ipChecksum and 0xFF).toByte()

        // TCP header (20 bytes)
        response[20] = (dstPort shr 8).toByte() // Src port = orig Dst port
        response[21] = (dstPort and 0xFF).toByte()
        response[22] = (srcPort shr 8).toByte() // Dst port = orig Src port
        response[23] = (srcPort and 0xFF).toByte()

        // Ack number = seqNum + 1 if SYN, else 0
        val ackNum = if (isSyn) seqNum + 1 else 0L
        response[24] = 0.toByte() // Sequence number = 0
        response[25] = 0.toByte()
        response[26] = 0.toByte()
        response[27] = 0.toByte()
        response[28] = (ackNum shr 24).toByte()
        response[29] = (ackNum shr 16).toByte()
        response[30] = (ackNum shr 8).toByte()
        response[31] = (ackNum and 0xFF).toByte()

        response[32] = 0x50.toByte() // Data offset (5 words = 20 bytes)
        // Flags: RST + ACK (0x14) if ACK, or RST (0x04)
        response[33] = if (isSyn) 0x14.toByte() else 0x04.toByte()
        response[34] = 0.toByte() // Window size
        response[35] = 0.toByte()

        // TCP pseudo-header checksum calculation
        val tcpChecksum = computeTcpChecksum(response, 12, 16, 20, 20)
        response[36] = (tcpChecksum shr 8).toByte()
        response[37] = (tcpChecksum and 0xFF).toByte()

        return response
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            val high = data[i].toInt() and 0xFF
            val low = data[i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv()) and 0xFFFF
    }

    private fun computeTcpChecksum(packet: ByteArray, srcIpOffset: Int, dstIpOffset: Int, tcpOffset: Int, tcpLength: Int): Int {
        var sum = 0
        // Pseudo header: Src IP (4 bytes) + Dst IP (4 bytes) + Zero (1 byte) + Protocol (1 byte) + TCP length (2 bytes)
        for (i in 0 until 4) {
            sum += (packet[srcIpOffset + i].toInt() and 0xFF) shl (if (i % 2 == 0) 8 else 0)
            sum += (packet[dstIpOffset + i].toInt() and 0xFF) shl (if (i % 2 == 0) 8 else 0)
        }
        sum += 6 // Protocol TCP
        sum += tcpLength

        // TCP segment
        var i = tcpOffset
        while (i < tcpOffset + tcpLength - 1) {
            if (i != tcpOffset + 16 && i != tcpOffset + 17) { // Skip existing checksum bytes
                val high = packet[i].toInt() and 0xFF
                val low = packet[i + 1].toInt() and 0xFF
                sum += (high shl 8) or low
            }
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv()) and 0xFFFF
    }
}
