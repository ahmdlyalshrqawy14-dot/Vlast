package com.example.core.model

/**
 * Identifies the currently active network transport monitored by Vlast.
 */
enum class NetworkType {
    WIFI,
    MOBILE,
    NONE;

    fun getLabel(simSlot: Int = 0): String {
        return when (this) {
            WIFI -> "الواي فاي"
            MOBILE -> if (simSlot == 1) "الشريحة 2" else "الشريحة 1"
            NONE -> "الشبكة"
        }
    }
}
