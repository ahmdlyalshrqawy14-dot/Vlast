package com.example.core.sim

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

/**
 * Dual SIM manager for Item 13:
 * Allows independent tracking and limit assignments per SIM slot.
 */
class DualSimManager(private val context: Context) {

    data class SimSlotInfo(
        val slotIndex: Int,
        val subscriptionId: Int,
        val displayName: String,
        val carrierName: String
    )

    fun getAvailableSims(): List<SimSlotInfo> {
        val list = mutableListOf<SimSlotInfo>()
        try {
            val subscriptionManager =
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                    ?: return defaultSingleSim()

            @SuppressLint("MissingPermission")
            val activeSubscriptionList: List<SubscriptionInfo>? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    try {
                        subscriptionManager.activeSubscriptionInfoList
                    } catch (_: SecurityException) {
                        null
                    }
                } else {
                    null
                }

            if (!activeSubscriptionList.isNullOrEmpty()) {
                for (info in activeSubscriptionList) {
                    list.add(
                        SimSlotInfo(
                            slotIndex = info.simSlotIndex,
                            subscriptionId = info.subscriptionId,
                            displayName = info.displayName?.toString() ?: "شريحة ${info.simSlotIndex + 1}",
                            carrierName = info.carrierName?.toString() ?: "SIM ${info.simSlotIndex + 1}"
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        return if (list.isEmpty()) defaultSingleSim() else list
    }

    private fun defaultSingleSim(): List<SimSlotInfo> {
        return listOf(
            SimSlotInfo(
                slotIndex = 0,
                subscriptionId = 1,
                displayName = "الشريحة الأساسية (SIM 1)",
                carrierName = "SIM 1"
            )
        )
    }
}
