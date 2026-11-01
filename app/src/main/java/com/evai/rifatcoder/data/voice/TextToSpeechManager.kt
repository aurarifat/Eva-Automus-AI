package com.evai.rifatcoder.data.voice

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

interface TtsCallback {
    fun onInitSuccess()
    fun onInitError(message: String)
    fun onSpeechStarted(utteranceId: String)
    fun onSpeechFinished(utteranceId: String)
    fun onSpeechError(utteranceId: String, errorCode: Int)
}

/**
 * Manages native Android Text-To-Speech engine.
 * Ensures proper initialization, language fallback, speech rate/pitch modulation,
 * installed voice querying, and utterance progress tracking.
 */
open class TextToSpeechManager(
    private val context: Context,
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var callback: TtsCallback? = null
    private var isInitialized: Boolean = false

    private var currentSpeechRate: Float = VoicePreferenceState.DEFAULT_SPEECH_RATE
    private var currentSpeechPitch: Float = VoicePreferenceState.DEFAULT_SPEECH_PITCH
    private var selectedVoiceId: String? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    open fun setCallback(callback: TtsCallback?) {
        this.callback = callback
    }

    fun isTtsReady(): Boolean = isInitialized

    open fun isSpeaking(): Boolean {
        return try {
            tts?.isSpeaking == true
        } catch (_: Exception) {
            false
        }
    }

    override fun onInit(status: Int) {
        mainHandler.post {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupUtteranceListener()
                applyRateAndPitch()
                callback?.onInitSuccess()
            } else {
                isInitialized = false
                callback?.onInitError("Text-To-Speech engine failed to initialize (status $status).")
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post {
                    callback?.onSpeechStarted(utteranceId.orEmpty())
                }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    callback?.onSpeechFinished(utteranceId.orEmpty())
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    callback?.onSpeechError(utteranceId.orEmpty(), -1)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    callback?.onSpeechError(utteranceId.orEmpty(), errorCode)
                }
            }
        })
    }

    open fun setSpeechRate(rate: Float) {
        currentSpeechRate = rate.coerceIn(
            VoicePreferenceState.MIN_SPEECH_RATE,
            VoicePreferenceState.MAX_SPEECH_RATE
        )
        try {
            tts?.setSpeechRate(currentSpeechRate)
        } catch (_: Exception) {}
    }

    open fun setPitch(pitch: Float) {
        currentSpeechPitch = pitch.coerceIn(
            VoicePreferenceState.MIN_SPEECH_PITCH,
            VoicePreferenceState.MAX_SPEECH_PITCH
        )
        try {
            tts?.setPitch(currentSpeechPitch)
        } catch (_: Exception) {}
    }

    open fun setVoice(voiceId: String?) {
        selectedVoiceId = voiceId
        if (voiceId.isNullOrBlank()) return

        try {
            val engine = tts ?: return
            val voices = engine.voices ?: return
            val matchedVoice = voices.firstOrNull { it.name == voiceId }
            if (matchedVoice != null) {
                engine.voice = matchedVoice
            }
        } catch (_: Exception) {}
    }

    open fun getAvailableVoices(localeTag: String? = null): List<VoiceInfo> {
        val engine = tts ?: return emptyList()
        return try {
            val voices = engine.voices ?: return emptyList()
            voices.filter { voice ->
                if (localeTag.isNullOrBlank()) true
                else voice.locale.toLanguageTag().equals(localeTag, ignoreCase = true) ||
                        voice.locale.language.equals(localeTag, ignoreCase = true)
            }.map { voice ->
                VoiceInfo(
                    id = voice.name,
                    name = voice.name,
                    localeTag = voice.locale.toLanguageTag(),
                    isNetworkConnectionRequired = voice.isNetworkConnectionRequired,
                    quality = when (voice.quality) {
                        Voice.QUALITY_VERY_HIGH -> "Very High"
                        Voice.QUALITY_HIGH -> "High"
                        Voice.QUALITY_NORMAL -> "Normal"
                        Voice.QUALITY_LOW -> "Low"
                        Voice.QUALITY_VERY_LOW -> "Very Low"
                        else -> "Standard"
                    }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun applyRateAndPitch() {
        try {
            tts?.setSpeechRate(currentSpeechRate)
            tts?.setPitch(currentSpeechPitch)
        } catch (_: Exception) {}
    }

    /**
     * Backward-compatible speak method for Phase 7.
     */
    open fun speak(text: String, utteranceId: String, language: VoiceLanguage): Boolean {
        return speak(
            text = text,
            utteranceId = utteranceId,
            language = language,
            rate = currentSpeechRate,
            pitch = currentSpeechPitch,
            voiceId = selectedVoiceId
        )
    }

    /**
     * Full speak method allowing explicit rate, pitch, and voice parameters.
     */
    open fun speak(
        text: String,
        utteranceId: String,
        language: VoiceLanguage,
        rate: Float,
        pitch: Float,
        voiceId: String? = null,
    ): Boolean {
        val engine = tts
        if (!isInitialized || engine == null || text.isBlank()) {
            return false
        }

        val targetLocale = when (language) {
            VoiceLanguage.ENGLISH -> Locale.US
            VoiceLanguage.BENGALI -> Locale.forLanguageTag("bn-BD")
        }

        try {
            val langResult = engine.isLanguageAvailable(targetLocale)
            if (langResult >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = targetLocale
            } else {
                engine.language = Locale.US
            }

            // Apply rate and pitch
            val effectiveRate = rate.coerceIn(
                VoicePreferenceState.MIN_SPEECH_RATE,
                VoicePreferenceState.MAX_SPEECH_RATE
            )
            val effectivePitch = pitch.coerceIn(
                VoicePreferenceState.MIN_SPEECH_PITCH,
                VoicePreferenceState.MAX_SPEECH_PITCH
            )
            engine.setSpeechRate(effectiveRate)
            engine.setPitch(effectivePitch)

            // Apply voice if specified
            val targetVoiceId = voiceId ?: selectedVoiceId
            if (!targetVoiceId.isNullOrBlank()) {
                val matched = engine.voices?.firstOrNull { it.name == targetVoiceId }
                if (matched != null) {
                    engine.voice = matched
                }
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            return result == TextToSpeech.SUCCESS
        } catch (_: Exception) {
            return false
        }
    }

    open fun stop() {
        mainHandler.post {
            try {
                tts?.stop()
            } catch (_: Exception) {}
        }
    }

    open fun shutdown() {
        mainHandler.post {
            try {
                tts?.stop()
                tts?.shutdown()
                tts = null
                isInitialized = false
            } catch (_: Exception) {}
        }
        callback = null
    }
}
