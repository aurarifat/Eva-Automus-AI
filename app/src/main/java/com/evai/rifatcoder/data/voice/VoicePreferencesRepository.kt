package com.evai.rifatcoder.data.voice

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository responsible for persistent storage and reactive delivery of Voice & Speech settings.
 */
open class VoicePreferencesRepository(
    private val context: Context,
    prefsName: String = PREFS_NAME,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(readFromStorage())
    val preferences: StateFlow<VoicePreferenceState> = _preferences.asStateFlow()

    fun getPreferences(): VoicePreferenceState {
        return _preferences.value
    }

    fun setSttLanguage(language: VoiceLanguagePreference) {
        prefs.edit().putString(KEY_STT_LANGUAGE, language.id).apply()
        updateFlow()
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
        updateFlow()
    }

    fun setTtsLanguage(language: VoiceLanguagePreference) {
        prefs.edit().putString(KEY_TTS_LANGUAGE, language.id).apply()
        updateFlow()
    }

    fun setTtsVoiceId(voiceId: String?) {
        if (voiceId != null) {
            prefs.edit().putString(KEY_TTS_VOICE_ID, voiceId).apply()
        } else {
            prefs.edit().remove(KEY_TTS_VOICE_ID).apply()
        }
        updateFlow()
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(
            VoicePreferenceState.MIN_SPEECH_RATE,
            VoicePreferenceState.MAX_SPEECH_RATE
        )
        prefs.edit().putFloat(KEY_SPEECH_RATE, clamped).apply()
        updateFlow()
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(
            VoicePreferenceState.MIN_SPEECH_PITCH,
            VoicePreferenceState.MAX_SPEECH_PITCH
        )
        prefs.edit().putFloat(KEY_SPEECH_PITCH, clamped).apply()
        updateFlow()
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        updateFlow()
    }

    fun saveAll(state: VoicePreferenceState) {
        prefs.edit()
            .putString(KEY_STT_LANGUAGE, state.sttLanguage.id)
            .putBoolean(KEY_TTS_ENABLED, state.ttsEnabled)
            .putString(KEY_TTS_LANGUAGE, state.ttsLanguage.id)
            .apply {
                if (state.ttsVoiceId != null) {
                    putString(KEY_TTS_VOICE_ID, state.ttsVoiceId)
                } else {
                    remove(KEY_TTS_VOICE_ID)
                }
            }
            .putFloat(
                KEY_SPEECH_RATE,
                state.speechRate.coerceIn(
                    VoicePreferenceState.MIN_SPEECH_RATE,
                    VoicePreferenceState.MAX_SPEECH_RATE
                )
            )
            .putFloat(
                KEY_SPEECH_PITCH,
                state.speechPitch.coerceIn(
                    VoicePreferenceState.MIN_SPEECH_PITCH,
                    VoicePreferenceState.MAX_SPEECH_PITCH
                )
            )
            .putBoolean(KEY_WAKE_WORD_ENABLED, state.wakeWordEnabled)
            .apply()
        updateFlow()
    }

    fun resetToDefaults(): VoicePreferenceState {
        val defaults = VoicePreferenceState()
        saveAll(defaults)
        return defaults
    }

    private fun readFromStorage(): VoicePreferenceState {
        val sttLangId = prefs.getString(KEY_STT_LANGUAGE, VoiceLanguagePreference.SYSTEM_DEFAULT.id)
        val ttsEnabled = prefs.getBoolean(KEY_TTS_ENABLED, true)
        val ttsLangId = prefs.getString(KEY_TTS_LANGUAGE, VoiceLanguagePreference.ENGLISH.id)
        val ttsVoiceId = prefs.getString(KEY_TTS_VOICE_ID, null)
        val rate = prefs.getFloat(KEY_SPEECH_RATE, VoicePreferenceState.DEFAULT_SPEECH_RATE)
        val pitch = prefs.getFloat(KEY_SPEECH_PITCH, VoicePreferenceState.DEFAULT_SPEECH_PITCH)
        val wakeWord = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)

        return VoicePreferenceState(
            sttLanguage = VoiceLanguagePreference.fromId(sttLangId),
            ttsEnabled = ttsEnabled,
            ttsLanguage = VoiceLanguagePreference.fromId(ttsLangId),
            ttsVoiceId = ttsVoiceId,
            speechRate = rate.coerceIn(
                VoicePreferenceState.MIN_SPEECH_RATE,
                VoicePreferenceState.MAX_SPEECH_RATE
            ),
            speechPitch = pitch.coerceIn(
                VoicePreferenceState.MIN_SPEECH_PITCH,
                VoicePreferenceState.MAX_SPEECH_PITCH
            ),
            wakeWordEnabled = wakeWord,
        )
    }

    private fun updateFlow() {
        _preferences.value = readFromStorage()
    }

    companion object {
        const val PREFS_NAME = "eva_ai_voice_preferences"

        const val KEY_STT_LANGUAGE = "key_voice_stt_language"
        const val KEY_TTS_ENABLED = "key_voice_tts_enabled"
        const val KEY_TTS_LANGUAGE = "key_voice_tts_language"
        const val KEY_TTS_VOICE_ID = "key_voice_tts_voice_id"
        const val KEY_SPEECH_RATE = "key_voice_speech_rate"
        const val KEY_SPEECH_PITCH = "key_voice_speech_pitch"
        const val KEY_WAKE_WORD_ENABLED = "key_voice_wake_word_enabled"
    }
}
