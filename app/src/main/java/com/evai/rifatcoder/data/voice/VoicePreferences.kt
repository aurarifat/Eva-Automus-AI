package com.evai.rifatcoder.data.voice

/**
 * Speech Recognition and Synthesis language options with fallback mappings.
 */
enum class VoiceLanguagePreference(
    val id: String,
    val displayName: String,
    val voiceLanguage: VoiceLanguage?,
    val localeTag: String,
) {
    SYSTEM_DEFAULT("system", "System Default", null, ""),
    ENGLISH("en", "English (US)", VoiceLanguage.ENGLISH, "en-US"),
    BENGALI("bn", "বাংলা (Bangladesh)", VoiceLanguage.BENGALI, "bn-BD");

    fun toVoiceLanguage(): VoiceLanguage {
        return voiceLanguage ?: VoiceLanguage.ENGLISH
    }

    companion object {
        fun fromId(id: String?): VoiceLanguagePreference {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM_DEFAULT
        }
    }
}

/**
 * Representation of an installed Text-To-Speech voice on the device.
 */
data class VoiceInfo(
    val id: String,
    val name: String,
    val localeTag: String,
    val isNetworkConnectionRequired: Boolean = false,
    val quality: String = "Standard",
)

/**
 * Immutable state holding persistent voice settings.
 */
data class VoicePreferenceState(
    val sttLanguage: VoiceLanguagePreference = VoiceLanguagePreference.SYSTEM_DEFAULT,
    val ttsEnabled: Boolean = true,
    val ttsLanguage: VoiceLanguagePreference = VoiceLanguagePreference.ENGLISH,
    val ttsVoiceId: String? = null,
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    val speechPitch: Float = DEFAULT_SPEECH_PITCH,
    val wakeWordEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val MIN_SPEECH_RATE = 0.75f
        const val MAX_SPEECH_RATE = 1.50f

        const val DEFAULT_SPEECH_PITCH = 1.0f
        const val MIN_SPEECH_PITCH = 0.80f
        const val MAX_SPEECH_PITCH = 1.20f
    }
}
