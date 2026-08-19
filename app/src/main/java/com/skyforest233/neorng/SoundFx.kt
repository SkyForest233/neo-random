package com.skyforest233.neorng

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * 音效引擎：网页版使用 WebAudio API 实时合成（tick / ding / swoosh / tear / siren），
 * 这里在加载时用完全相同的公式离线合成 PCM，再通过 AudioTrack 播放。
 */
object SoundFx {

    private const val SR = 44100

    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val tracks = HashMap<String, AudioTrack>()
    private val buffers = HashMap<String, ShortArray>()

    fun preload() {
        executor.execute {
            for (name in arrayOf("tick", "ding", "swoosh", "tear", "siren")) {
                buffers.getOrPut(name) { synth(name) }
            }
        }
    }

    fun play(name: String) {
        executor.execute {
            val pcm = buffers.getOrPut(name) { synth(name) }
            try {
                val track = tracks.getOrPut(name) { buildTrack(pcm.size) }
                track.write(pcm, 0, pcm.size)
                track.stop()
                track.reloadStaticData()
                track.play()
            } catch (_: Throwable) {
                // 音频设备异常时静默忽略，不影响功能
            }
        }
    }

    fun release() {
        executor.execute {
            tracks.values.forEach { runCatching { it.release() } }
            tracks.clear()
        }
    }

    private fun buildTrack(sizeInShorts: Int): AudioTrack {
        val bytes = sizeInShorts * 2
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bytes)
            .build()
    }

    private fun toShort(v: Float): Short = (v * 32767f).toInt().coerceIn(-32768, 32767).toShort()

    // ============ 与网页版 playSound() 逐条对应的合成公式 ============

    private fun synth(name: String): ShortArray = when (name) {
        "tick" -> tick()
        "ding" -> ding()
        "swoosh" -> swoosh()
        "tear" -> tear()
        "siren" -> siren()
        else -> ShortArray(0)
    }

    /** tick: triangle 1000Hz -> 100Hz (0.05s), gain 0.2 exp 衰减到 0.001 */
    private fun tick(): ShortArray {
        val dur = 0.05
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = 1000.0 * (100.0 / 1000.0).pow(t / dur)
            phase += f / SR
            val tri = 4.0 * kotlin.math.abs(phase % 1.0 - 0.5) - 1.0
            val gain = 0.2 * (0.001 / 0.2).pow(t / dur)
            out[i] = toShort((tri * gain).toFloat())
        }
        return out
    }

    /** ding: sine 1400Hz gain0.6 衰减0.8s + sine 2800Hz gain0.2 衰减0.3s */
    private fun ding(): ShortArray {
        val dur = 0.8
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val g1 = 0.6 * (0.001 / 0.6).pow(min(t / 0.8, 1.0))
            val s1 = sin(2.0 * PI * 1400.0 * t) * g1
            var s2 = 0.0
            if (t < 0.3) {
                val g2 = 0.2 * (0.001 / 0.2).pow(t / 0.3)
                s2 = sin(2.0 * PI * 2800.0 * t) * g2
            }
            out[i] = toShort((s1 + s2).toFloat())
        }
        return out
    }

    /** swoosh: triangle 300Hz -> 50Hz (0.15s), gain 0 -> 0.1 -> 0 线性包络 */
    private fun swoosh(): ShortArray {
        val dur = 0.15
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = 300.0 * (50.0 / 300.0).pow(t / dur)
            phase += f / SR
            val tri = 4.0 * kotlin.math.abs(phase % 1.0 - 0.5) - 1.0
            val gain = if (t < 0.05) t / 0.05 * 0.1 else (1.0 - (t - 0.05) / 0.10) * 0.1
            out[i] = toShort((tri * gain).toFloat())
        }
        return out
    }

    /** tear: 白噪声 -> 1200Hz bandpass(Q=1), gain 0.8 exp 衰减到 0.01 (0.3s) */
    private fun tear(): ShortArray {
        val dur = 0.3
        val n = (SR * dur).toInt()
        val noise = ShortArray(n)
        val rnd = java.util.Random(42)
        for (i in 0 until n) noise[i] = toShort((rnd.nextDouble() * 2 - 1).toFloat())

        // RBJ biquad bandpass
        val f0 = 1200.0
        val q = 1.0
        val w = 2.0 * PI * f0 / SR
        val alpha = sin(w) / (2.0 * q)
        val b0 = alpha
        val b2 = -alpha
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cos(w)
        val a2 = 1.0 - alpha
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0

        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val x0 = noise[i] / 32767.0
            val y0 = (b0 / a0) * x0 + (b2 / a0) * x2 - (a1 / a0) * y1 - (a2 / a0) * y2
            x2 = x1; x1 = x0; y2 = y1; y1 = y0
            val gain = 0.8 * (0.01 / 0.8).pow(t / dur)
            out[i] = toShort((y0 * gain).toFloat())
        }
        return out
    }

    /** siren: sawtooth 400<->800Hz 往返 1.6s, gain 0.3 线性衰减到 0 */
    private fun siren(): ShortArray {
        val dur = 1.6
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = when {
                t < 0.4 -> 400.0 + (800.0 - 400.0) * (t / 0.4)
                t < 0.8 -> 800.0 - (800.0 - 400.0) * ((t - 0.4) / 0.4)
                t < 1.2 -> 400.0 + (800.0 - 400.0) * ((t - 0.8) / 0.4)
                else -> 800.0 - (800.0 - 400.0) * ((t - 1.2) / 0.4)
            }
            phase += f / SR
            val saw = 2.0 * (phase % 1.0) - 1.0
            val gain = 0.3 * (1.0 - t / dur)
            out[i] = toShort((saw * gain).toFloat())
        }
        return out
    }
}
