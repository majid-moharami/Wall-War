package com.wallwar.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wallwar.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val prefs: SharedPreferences = context.getSharedPreferences("wallwar_audio_prefs", Context.MODE_PRIVATE)

    private val _isSoundEnabled = mutableStateOf(prefs.getBoolean("key_sound_enabled", true))
    var isSoundEnabled: Boolean
        get() = _isSoundEnabled.value
        set(value) {
            _isSoundEnabled.value = value
            prefs.edit().putBoolean("key_sound_enabled", value).apply()
        }

    private val _isVibrationEnabled = mutableStateOf(prefs.getBoolean("key_vibration_enabled", true))
    var isVibrationEnabled: Boolean
        get() = _isVibrationEnabled.value
        set(value) {
            _isVibrationEnabled.value = value
            prefs.edit().putBoolean("key_vibration_enabled", value).apply()
        }

    fun toggleSound(): Boolean {
        isSoundEnabled = !isSoundEnabled
        return isSoundEnabled
    }

    fun toggleVibration(): Boolean {
        isVibrationEnabled = !isVibrationEnabled
        return isVibrationEnabled
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val sampleRate = 22050

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    /**
     * Plays an audio resource using Android MediaPlayer.
     */
    private fun playRawResource(rawResId: Int, fallback: (() -> Unit)? = null) {
        if (!isSoundEnabled) return
        scope.launch(Dispatchers.IO) {
            try {
                val mp = MediaPlayer.create(context, rawResId)
                if (mp != null) {
                    mp.setOnCompletionListener { player ->
                        try {
                            player.release()
                        } catch (_: Throwable) {}
                    }
                    mp.start()
                } else {
                    fallback?.invoke()
                }
            } catch (e: Exception) {
                Log.w("SoundManager", "Failed to play raw audio resource $rawResId", e)
                fallback?.invoke()
            }
        }
    }

    /**
     * Plays the custom winner sound (winner_sound.mp3) with victory vibration.
     */
    fun playWinnerSound() {
        if (!isSoundEnabled) return
        playRawResource(R.raw.winner_sound) {
            playVictoryFanfare()
        }
        vibrateSuccess()
    }

    /**
     * Plays the custom loser sound (loser_sound.wav) for match defeat.
     */
    fun playLoserSound() {
        if (!isSoundEnabled) return
        playRawResource(R.raw.loser_sound) {
            playErrorSound()
        }
        vibrateShort()
    }

    /**
     * Plays the custom spinner sound (spinner_sound.wav) when spinning the lucky wheel.
     */
    fun playSpinnerSound() {
        if (!isSoundEnabled) return
        playRawResource(R.raw.spinner_sound) {
            playButtonClick()
        }
        vibrateShort()
    }

    /**
     * Plays the custom coin claim sound (coins_sound.wav) when claiming coins, daily rewards, daily quests, etc.
     */
    fun playCoinSound() {
        if (!isSoundEnabled) return
        playRawResource(R.raw.coins_sound) {
            playSynthesizedCoinSound()
        }
        vibrateCoinReward()
    }

    fun playRewardSound() {
        playCoinSound()
    }

    fun playVictory() {
        playWinnerSound()
    }

    /**
     * Synthesizes PCM Audio in real-time.
     * mine = true -> higher frequency pitch tick
     * wall = true -> wooden thud sound
     */
    fun playMoveSound(isMine: Boolean, isWall: Boolean) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val durationMs = if (isWall) 120 else 80
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)

                val startFreq = if (isWall) (if (isMine) 340.0 else 270.0) else (if (isMine) 660.0 else 500.0)
                val endFreq = if (isWall) (if (isMine) 180.0 else 140.0) else startFreq

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * progress

                    // Envelope: fast attack, exponential decay
                    val envelope = kotlin.math.exp(-progress * 5.0)

                    val wave = if (isWall) {
                        sin(2.0 * Math.PI * currentFreq * t)
                    } else {
                        // Triangle wave for crisp pawn step tick
                        val phase = (t * currentFreq) % 1.0
                        if (phase < 0.5) (4.0 * phase - 1.0) else (3.0 - 4.0 * phase)
                    }

                    buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.35).toInt().toShort()
                }

                playPcmBuffer(buffer, durationMs)
            } catch (_: Throwable) {
                // Graceful fallback
            }
        }
    }

    fun playButtonClick() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val durationMs = 40
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val envelope = kotlin.math.exp(-progress * 8.0)
                    val wave = sin(2.0 * Math.PI * 880.0 * t)
                    buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.2).toInt().toShort()
                }
                playPcmBuffer(buffer, durationMs)
            } catch (_: Throwable) {}
        }
    }

    fun playInvalidMove() {
        playErrorSound()
        vibrateShort()
    }

    fun playPawnMove() {
        playMoveSound(isMine = true, isWall = false)
        vibrateShort()
    }

    fun playWallPlaced() {
        playMoveSound(isMine = true, isWall = true)
        vibrateShort()
    }

    fun playErrorSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val durationMs = 150
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val envelope = kotlin.math.exp(-progress * 4.0)
                    val wave = sin(2.0 * Math.PI * 180.0 * t) + sin(2.0 * Math.PI * 140.0 * t)
                    buffer[i] = (wave * 0.5 * envelope * Short.MAX_VALUE * 0.3).toInt().toShort()
                }

                playPcmBuffer(buffer, durationMs)
            } catch (_: Throwable) {}
        }
    }

    fun playVictoryFanfare() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
            for (freq in notes) {
                try {
                    val durationMs = 120
                    val numSamples = (sampleRate * durationMs) / 1000
                    val buffer = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val progress = i.toDouble() / numSamples
                        val envelope = kotlin.math.exp(-progress * 3.0)
                        val wave = sin(2.0 * Math.PI * freq * t)
                        buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.4).toInt().toShort()
                    }

                    playPcmBuffer(buffer, durationMs)
                    kotlinx.coroutines.delay(100)
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Synthesizes a bright, crisp cascading coin claim chime sound effect (arcade style coin clink).
     * Plays a sparkling ascending arpeggio with rich harmonics and metallic bell ring as fallback.
     */
    private fun playSynthesizedCoinSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                // High-pitch sparkling coin tones: B5 (987.77 Hz), E6 (1318.51 Hz), G#6 (1661.22 Hz), B6 (1975.53 Hz)
                val notes = listOf(987.77, 1318.51, 1661.22, 1975.53)
                for (freq in notes) {
                    val durationMs = 80
                    val numSamples = (sampleRate * durationMs) / 1000
                    val buffer = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val progress = i.toDouble() / numSamples
                        // Fast attack, exponential metallic decay
                        val envelope = kotlin.math.exp(-progress * 5.5)

                        // Fundamental sine + secondary harmonic for sparkling coin chime
                        val wave = 0.7 * sin(2.0 * Math.PI * freq * t) +
                                   0.3 * sin(2.0 * Math.PI * (freq * 2.0) * t)

                        buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.42).toInt().toShort()
                    }

                    playPcmBuffer(buffer, durationMs)
                    kotlinx.coroutines.delay(50)
                }
            } catch (_: Throwable) {}
        }
    }

    fun vibrateCoinReward() {
        if (!isVibrationEnabled || vibrator == null) return
        try {
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 35, 30, 35, 40), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
        } catch (_: Exception) {}
    }

    fun playEmoteSound(emoteId: String = "") {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val durationMs = 100
                val numSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(numSamples)
                val baseFreq = when {
                    emoteId.contains("fire") -> 700.0
                    emoteId.contains("cool") -> 520.0
                    emoteId.contains("smirk") -> 440.0
                    emoteId.contains("greedy") -> 880.0
                    else -> 600.0
                }
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val envelope = kotlin.math.exp(-progress * 5.0)
                    val wave = sin(2.0 * Math.PI * (baseFreq + (progress * 200.0)) * t)
                    buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.3).toInt().toShort()
                }
                playPcmBuffer(buffer, durationMs)
            } catch (_: Throwable) {}
        }
    }

    private suspend fun playPcmBuffer(buffer: ShortArray, durationMs: Int) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            kotlinx.coroutines.delay(durationMs.toLong() + 30)
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {}
        }
    }

    fun vibrateShort() {
        if (!isVibrationEnabled || vibrator == null) return
        try {
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun vibrateSuccess() {
        if (!isVibrationEnabled || vibrator == null) return
        try {
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 80), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (_: Exception) {}
    }
}
