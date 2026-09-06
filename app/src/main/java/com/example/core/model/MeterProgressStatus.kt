package com.example.core.model

import androidx.compose.ui.graphics.Color

/**
 * Visual color gradation for consumption counters (Item 1):
 * - Green: < 70%
 * - Yellow/Amber: 70% - 90%
 * - Red: > 90% (or 100% / breached)
 */
enum class MeterProgressStatus(
    val colorLight: Color,
    val colorDark: Color
) {
    GREEN(
        colorLight = Color(0xFF16A34A), // Emerald 600
        colorDark = Color(0xFF22C55E)   // Emerald 500
    ),
    YELLOW(
        colorLight = Color(0xFFD97706), // Amber 600
        colorDark = Color(0xFFFBBF24)   // Amber 400
    ),
    RED(
        colorLight = Color(0xFFDC2626), // Red 600
        colorDark = Color(0xFFEF4444)   // Red 500
    );

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
