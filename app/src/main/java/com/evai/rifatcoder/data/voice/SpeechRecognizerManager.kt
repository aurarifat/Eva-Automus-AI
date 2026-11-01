package com.evai.rifatcoder.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

interface SpeechRecognitionCallback {
    fun onReady()
    fun onListeningStarted()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResult(partialText: String)
    fun onFinalResult(finalText: String)
    fun onError(errorMessage: String, errorCode: Int)
}

/**
 * Robust wrapper around Android's SpeechRecognizer.
 * Dispatches callbacks safely to the Main Looper and handles device availability.
 */
open class SpeechRecognizerManager(
    private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: SpeechRecognitionCallback? = null
    private var isListening: Boolean = false

    open fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    open fun setCallback(callback: SpeechRecognitionCallback?) {
        this.callback = callback
    }

    open fun startListening(languagePreference: VoiceLanguagePreference) {
        startListening(languagePreference.toVoiceLanguage())
    }

    open fun startListening(language: VoiceLanguage) {
        if (!isRecognitionAvailable()) {
            callback?.onError("Speech recognition service is not available on this device.", -1)
            return
        }

        mainHandler.post {
            try {
                // Cancel any previous session
                cancel()

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                    speechRecognizer = it
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        callback?.onReady()
                    }

                    override fun onBeginningOfSpeech() {
                        callback?.onListeningStarted()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        callback?.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val message = translateErrorCode(error)
                        callback?.onError(message, error)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty().trim()
                        callback?.onFinalResult(text)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty().trim()
                        if (text.isNotEmpty()) {
                            callback?.onPartialResult(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.localeTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, language.localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                recognizer.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                callback?.onError("Failed to start speech recognition: ${e.localizedMessage ?: "Unknown error"}", -1)
            }
        }
    }

    open fun stopListening() {
        mainHandler.post {
            try {
                if (isListening) {
                    speechRecognizer?.stopListening()
                }
            } catch (_: Exception) {}
        }
    }

    open fun cancel() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {}
        }
    }

    open fun destroy() {
        cancel()
        callback = null
    }

    private fun translateErrorCode(code: Int): String {
        return when (code) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Client error in speech service."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout while processing speech."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait."
            SpeechRecognizer.ERROR_SERVER -> "Speech server encountered an error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected within time limit."
            else -> "Speech recognition error (Code $code)"
        }
    }
}
