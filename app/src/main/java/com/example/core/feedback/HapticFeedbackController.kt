package com.example.core.feedback

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Haptic and vibration feedback for critical alerts (Item 15):
 * - Gentle pulse at 90% threshold warning
 * - Distinct double pulse at actual connection cut / kill switch
 */
class HapticFeedbackController(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Gentle haptic warning when reaching 90% quota.
     */
    fun triggerWarning90Percent() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120L)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Distinct dual haptic pulse upon actual cutoff or kill switch.
     */
    fun triggerCutoffHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 80, 200)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 150, 80, 200)
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }
}
