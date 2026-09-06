package com.example.core.model

import androidx.compose.ui.graphics.Color

/**
 * Visual color gradation for consumption counters (Item 1 & Section 8):
 * - Green / Sky Blue (Color-blind): < 70%
 * - Yellow / Vivid Amber (Color-blind): 70% - 90%
 * - Red / Vivid Purple (Color-blind): > 90% (or 100% / breached)
 * Supports symbols and explicit labels so reliance on color alone is eliminated.
 */
enum class MeterProgressStatus(
    val colorLight: Color,
    val colorDark: Color,
    val colorBlindColor: Color,
    val symbol: String,
    val labelAr: String,
    val labelEn: String
) {
    GREEN(
        colorLight = Color(0xFF16A34A), // Emerald 600
        colorDark = Color(0xFF22C55E),  // Emerald 500
        colorBlindColor = Color(0xFF38BDF8), // High-contrast Vivid Sky Blue
        symbol = "✓",
        labelAr = "آمن",
        labelEn = "Safe"
    ),
    YELLOW(
        colorLight = Color(0xFFD97706), // Amber 600
        colorDark = Color(0xFFFBBF24),  // Amber 400
        colorBlindColor = Color(0xFFF59E0B), // Vivid Amber
        symbol = "!",
        labelAr = "تحذير",
        labelEn = "Warning"
    ),
    RED(
        colorLight = Color(0xFFDC2626), // Red 600
        colorDark = Color(0xFFEF4444),  // Red 500
        colorBlindColor = Color(0xFFA855F7), // High-contrast Vivid Purple
        symbol = "✕",
        labelAr = "حرج",
        labelEn = "Critical"
    );

    fun getColor(isColorBlind: Boolean, isDarkMode: Boolean = true): Color {
        return if (isColorBlind) {
            colorBlindColor
        } else if (isDarkMode) {
            colorDark
        } else {
            colorLight
        }
    }

    companion object {
        fun fromRatio(ratio: Float): MeterProgressStatus {
            return when {
                ratio >= 0.90f -> RED
                ratio >= 0.70f -> YELLOW
                else -> GREEN
            }
        }

        fun fromUsage(usedBytes: Long, limitBytes: Long?): MeterProgressStatus {
            if (limitBytes == null || limitBytes <= 0L) return GREEN
            val ratio = (usedBytes.toDouble() / limitBytes.toDouble()).toFloat()
            return fromRatio(ratio)
        }
    }
}

