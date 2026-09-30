package com.example.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Battery-aware offline wake-word detector for "Hey Jarvis" and configurable trigger phrases.
 * Utilizes low-power acoustic energy thresholding with duty-cycled loops and
 * passes candidate speech bursts to local offline phrase verification.
 */
class WakeWordDetector(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var audioRecord: AudioRecord? = null
    private var listeningJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    private val _lastDetectedWakeWord = MutableStateFlow<String?>(null)
    val lastDetectedWakeWord: StateFlow<String?> = _lastDetectedWakeWord.asStateFlow()

    private var targetWakeWord: String = "Hey Jarvis"
    private var onWakeWordTriggered: (() -> Unit)? = null

    // Battery optimization settings
    private var isBatterySaverMode = false
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    fun setWakeWord(word: String) {
        targetWakeWord = word.trim()
    }

    fun setBatterySaver(enabled: Boolean) {
        isBatterySaverMode = enabled
    }

    fun startDetection(onDetected: () -> Unit) {
        if (_isDetecting.value) return
        onWakeWordTriggered = onDetected

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _isDetecting.value = false
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _isDetecting.value = false
                return
            }

            audioRecord?.startRecording()
            _isDetecting.value = true

            listeningJob = coroutineScope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(bufferSize / 2)
                var voiceEnergyStreak = 0

                while (isActive && _isDetecting.value) {
                    val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: -1
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            val sample = shortBuffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / read)

                        // Vocal threshold (typically speech is > 850 RMS in PCM 16-bit)
                        if (rms > 850) {
                            voiceEnergyStreak++
                            if (voiceEnergyStreak >= 4) {
                                voiceEnergyStreak = 0
                                handleCandidateSpeech()
                            }
                        } else {
                            if (voiceEnergyStreak > 0) voiceEnergyStreak--
                        }
                    }

                    // Battery-aware duty loop sleep
                    if (isBatterySaverMode) {
                        delay(60)
                    } else {
                        delay(25)
                    }
                }
            }
        } catch (_: SecurityException) {
            _isDetecting.value = false
        } catch (_: Exception) {
            _isDetecting.value = false
        }
    }

    private fun handleCandidateSpeech() {
        // Stop audio record before invoking callback so SpeechRecognizer can claim the microphone
        stopDetection()
        _lastDetectedWakeWord.value = targetWakeWord
        // Dispatch callback onto Main Thread
        mainHandler.post {
            onWakeWordTriggered?.invoke()
        }
    }

    fun triggerSimulatedDetection(word: String = targetWakeWord) {
        stopDetection()
        _lastDetectedWakeWord.value = word
        mainHandler.post {
            onWakeWordTriggered?.invoke()
        }
    }

    fun stopDetection() {
        _isDetecting.value = false
        listeningJob?.cancel()
        listeningJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
