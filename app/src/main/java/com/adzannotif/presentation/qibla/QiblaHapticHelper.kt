package com.adzannotif.presentation.qibla

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

@Suppress("DEPRECATION")
class QiblaHapticHelper(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var lastVibratedAt: Long = 0L

    /**
     * Triggers a distinct, high-precision tactile vibration when aligned with Qibla (Kaaba).
     * Uses hardware Vibrator API + View Haptic Feedback with FLAG_IGNORE_GLOBAL_SETTING.
     */
    fun vibrateAligned(view: View? = null) {
        val now = System.currentTimeMillis()
        if (now - lastVibratedAt < 300) return
        lastVibratedAt = now

        // 1. Hardware Vibrator API
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val effect = VibrationEffect.createWaveform(
                            longArrayOf(0, 50, 70, 60),
                            intArrayOf(0, 220, 0, 255),
                            -1
                        )
                        vibrator.vibrate(effect)
                    } catch (_: Exception) {
                        try {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                        } catch (_: Exception) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 50, 70, 60),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 50, 70, 60), -1)
                }
            }
        } catch (_: Exception) {}

        // 2. View Haptic Feedback as companion
        try {
            view?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    it.performHapticFeedback(
                        HapticFeedbackConstants.CONFIRM,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                } else {
                    it.performHapticFeedback(
                        HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                }
            }
        } catch (_: Exception) {}
    }
}
