package com.skyforest233.neorng

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * 触感引擎：对应网页版 navigator.vibrate()。
 * 全局静音（isMuted）时由调用方跳过，与网页行为一致。
 */
object Haptics {

    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        if (vibrator == null) {
            vibrator = context.getSystemService(Vibrator::class.java)
        }
    }

    fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        runCatching {
            if (v.hasVibrator()) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        }
    }

    fun vibrate(ms: Long) {
        if (ms <= 0) return
        vibrate(longArrayOf(0, ms))
    }

    /** 轻触感：低振幅（0-255），适合高频 tick 反馈 */
    fun vibrateLight(ms: Long, amplitude: Int = 64) {
        val v = vibrator ?: return
        runCatching {
            if (v.hasVibrator()) {
                v.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, ms),
                        intArrayOf(0, amplitude.coerceIn(1, 255)),
                        -1
                    )
                )
            }
        }
    }
}
