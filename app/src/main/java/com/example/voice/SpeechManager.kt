package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var isContinuous = false
    private var isDestroyed = false

    fun startListening(continuous: Boolean = false, onResult: (String) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening(continuous, onResult) }
            return
        }

        onResultCallback = onResult
        isContinuous = continuous
        _statusMessage.value = null

        // 1. Permission verification
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _statusMessage.value = "Microphone permission required for voice recognition."
            _isListening.value = false
            return
        }

        // 2. Hardware/Service availability
        val isStandardAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val isOnDeviceAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else false

        if (!isStandardAvailable && !isOnDeviceAvailable) {
            _statusMessage.value = "Speech recognition service unavailable on this device."
            _isListening.value = false
            return
        }

        // Clean up any stale recognizer instance
        cleanupRecognizer()

        try {
            val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isOnDeviceAvailable) {
                try {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } catch (_: Exception) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialText.value = ""
                    _statusMessage.value = "Listening..."
                }

                override fun onBeginningOfSpeech() {
                    _statusMessage.value = "Voice detected..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = when {
                        rmsdB <= 0f -> 0f
                        rmsdB >= 12f -> 1f
                        else -> rmsdB / 12f
                    }
                    _rmsDb.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _rmsDb.value = 0f
                    _statusMessage.value = "Processing speech..."
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsDb.value = 0f
                    val description = getErrorDescription(error)
                    _statusMessage.value = description
                    cleanupRecognizer()

                    // In continuous listening mode, restart gracefully after brief pause for non-fatal states
                    if (isContinuous && !isDestroyed) {
                        val shouldRestart = error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_CLIENT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY

                        if (shouldRestart) {
                            mainHandler.postDelayed({
                                if (isContinuous && !isDestroyed) {
                                    startListening(continuous = true, onResult = onResult)
                                }
                            }, 600)
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: ""
                    _isListening.value = false
                    _rmsDb.value = 0f
                    _partialText.value = ""
                    _statusMessage.value = null
                    cleanupRecognizer()

                    if (spoken.isNotBlank()) {
                        onResultCallback?.invoke(spoken)
                    }

                    if (isContinuous && !isDestroyed) {
                        mainHandler.postDelayed({
                            if (isContinuous && !isDestroyed) {
                                startListening(continuous = true, onResult = onResult)
                            }
                        }, 400)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let {
                        _partialText.value = it
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val languageTag = Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _statusMessage.value = "Voice recognizer error: ${e.message}"
            cleanupRecognizer()
        }
    }

    fun stopListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }

        isContinuous = false
        cleanupRecognizer()
        _isListening.value = false
        _rmsDb.value = 0f
        _partialText.value = ""
        _statusMessage.value = null
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun destroy() {
        isDestroyed = true
        stopListening()
    }

    private fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Voice client error. Please try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for voice recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap orb to try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer was busy. Ready to listen."
            SpeechRecognizer.ERROR_SERVER -> "Voice server unavailable. Please try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap orb to speak."
            else -> "Speech recognition error ($errorCode). Tap orb to retry."
        }
    }
}
