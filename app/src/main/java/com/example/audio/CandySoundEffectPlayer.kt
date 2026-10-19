package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class CandySoundEffectPlayer {

    var soundEnabled: Boolean = true
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playSwap() {
        if (!soundEnabled) return
        scope.launch {
            playTone(freq = 440f, endFreq = 660f, durationMs = 60, volume = 0.4f)
        }
    }

    fun playMatch(combo: Int = 1) {
        if (!soundEnabled) return
        scope.launch {
            val baseFreq = 523.25f * (1f + (combo.coerceAtMost(6) - 1) * 0.15f) // C5 modulated by combo
            playTone(freq = baseFreq, endFreq = baseFreq * 1.25f, durationMs = 120, volume = 0.6f)
        }
    }

    fun playSpecialExplosion() {
        if (!soundEnabled) return
        scope.launch {
            playTone(freq = 280f, endFreq = 120f, durationMs = 180, volume = 0.7f)
        }
    }

    fun playColorBomb() {
        if (!soundEnabled) return
        scope.launch {
            // Arpeggio
            val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
            for (freq in notes) {
                playTone(freq = freq, endFreq = freq, durationMs = 60, volume = 0.5f)
            }
        }
    }

    fun playLevelComplete() {
        if (!soundEnabled) return
        scope.launch {
            // Fanfare: C E G C
            val fanfare = listOf(523.25f to 100, 659.25f to 100, 783.99f to 120, 1046.50f to 300)
            for ((freq, dur) in fanfare) {
                playTone(freq = freq, endFreq = freq, durationMs = dur, volume = 0.7f)
            }
        }
    }

    fun playLevelFailed() {
        if (!soundEnabled) return
        scope.launch {
            playTone(freq = 400f, endFreq = 220f, durationMs = 300, volume = 0.5f)
        }
    }

    fun playButtonClick() {
        if (!soundEnabled) return
        scope.launch {
            playTone(freq = 800f, endFreq = 950f, durationMs = 40, volume = 0.3f)
        }
    }

    private fun playTone(freq: Float, endFreq: Float, durationMs: Int, volume: Float) {
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            if (numSamples <= 0) return

            val generatedSnd = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toFloat() / numSamples
                val currentFreq = freq + (endFreq - freq) * progress

                // Envelope attack and decay
                val envelope = when {
                    progress < 0.15f -> progress / 0.15f
                    else -> (1f - progress).coerceAtLeast(0f)
                }

                val sample = sin(2.0 * PI * currentFreq * t) * envelope * volume * 32767.0
                generatedSnd[i] = sample.toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Audio error non-fatal
        }
    }
}
