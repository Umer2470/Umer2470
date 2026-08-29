package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundEffectHelper {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        } catch (_: Throwable) {
            // ToneGenerator fallback
        }
    }

    /**
     * Emits a crisp, professional POS terminal barcode scanner beep tone and haptic feedback.
     */
    fun playBeepAndVibrate(context: Context, isSuccess: Boolean = true) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            }
            if (isSuccess) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
            }
        } catch (_: Throwable) {
            // Fallback gracefully
        }

        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(if (isSuccess) 40L else 90L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(if (isSuccess) 40L else 90L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(if (isSuccess) 40L else 90L)
                }
            }
        } catch (_: Throwable) {
            // Ignore vibration errors
        }
    }
}
