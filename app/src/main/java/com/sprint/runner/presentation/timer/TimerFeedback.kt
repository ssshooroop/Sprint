package com.sprint.runner.presentation.timer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.sprint.runner.domain.timer.Cue

/**
 * Maps abstract [Cue]s to real haptics + tones.
 *
 * Vibration is the primary channel (the runner feels it without looking); the
 * tone is secondary and is routed to STREAM_MUSIC so it follows Bluetooth
 * headphones when connected. Replace the tones with a real "start gun" sample
 * later — the cue contract stays the same.
 */
class TimerFeedback(context: Context) {

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Volume 100; STREAM_MUSIC so it ducks/plays through connected headphones.
    private val tone: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }.getOrNull()

    fun play(cue: Cue) {
        when (cue) {
            Cue.COUNTDOWN_TICK -> {
                vibrate(longArrayOf(0, 40))
                beep(ToneGenerator.TONE_PROP_BEEP, 90)
            }
            Cue.GO -> {
                vibrate(longArrayOf(0, 350))
                beep(ToneGenerator.TONE_CDMA_HIGH_L, 350)
            }
            Cue.WORK_END -> {
                vibrate(longArrayOf(0, 120, 90, 120))
                beep(ToneGenerator.TONE_PROP_BEEP2, 200)
            }
            Cue.FINISH -> {
                vibrate(longArrayOf(0, 200, 120, 200, 120, 400))
                beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
            }
        }
    }

    private fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    }

    private fun beep(toneType: Int, durationMs: Int) {
        runCatching { tone?.startTone(toneType, durationMs) }
    }

    fun release() {
        runCatching { tone?.release() }
    }
}
