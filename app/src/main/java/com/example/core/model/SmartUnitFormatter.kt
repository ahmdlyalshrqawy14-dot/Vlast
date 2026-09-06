package com.example.core.model

import java.util.Locale

/**
 * Format bytes automatically into B, KB, MB, or GB with smart rounding (Item 6).
 * Also parses input strings flexibly into bytes.
 */
object SmartUnitFormatter {

    private const val ONE_KB = 1024L
    private const val ONE_MB = 1024L * 1024L
    private const val ONE_GB = 1024L * 1024L * 1024L

    fun format(bytes: Long): FormattedValue {
        val safeBytes = bytes.coerceAtLeast(0L)
        return when {
            safeBytes >= ONE_GB -> {
                val value = safeBytes.toDouble() / ONE_GB
                FormattedValue(
                    amount = String.format(Locale.US, if (value >= 10.0) "%.1f" else "%.2f", value),
                    unit = "GB",
                    rawBytes = safeBytes
                )
            }
            safeBytes >= ONE_MB -> {
                val value = safeBytes.toDouble() / ONE_MB
                FormattedValue(
                    amount = String.format(Locale.US, if (value >= 100.0) "%.0f" else "%.1f", value),
                    unit = "MB",
                    rawBytes = safeBytes
                )
            }
            safeBytes >= ONE_KB -> {
                val value = safeBytes.toDouble() / ONE_KB
                FormattedValue(
                    amount = String.format(Locale.US, "%.1f", value),
                    unit = "KB",
                    rawBytes = safeBytes
                )
            }
            else -> {
                FormattedValue(
                    amount = safeBytes.toString(),
                    unit = "B",
                    rawBytes = safeBytes
                )
            }
        }
    }

    fun formatDisplay(bytes: Long): String {
        val formatted = format(bytes)
        return "${formatted.amount} ${formatted.unit}"
    }

    /**
     * Parses user numeric input given an active unit (MB or GB).
     */
    fun parseInputToBytes(rawText: String, defaultUnit: String = "MB"): Long? {
        val clean = rawText.trim().replace(',', '.')
        if (clean.isEmpty()) return null

        val number = clean.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return null

        val upper = clean.uppercase(Locale.US)
        return when {
            upper.contains("GB") -> (number * ONE_GB).toLong()
            upper.contains("MB") -> (number * ONE_MB).toLong()
            upper.contains("KB") -> (number * ONE_KB).toLong()
            defaultUnit.equals("GB", ignoreCase = true) -> (number * ONE_GB).toLong()
            else -> (number * ONE_MB).toLong()
        }
    }

    data class FormattedValue(
        val amount: String,
        val unit: String,
        val rawBytes: Long
    ) {
        val fullString: String get() = "$amount $unit"
    }
}
