package com.evai.rifatcoder.data.glow

import android.content.Context
import android.content.SharedPreferences
import com.evai.rifatcoder.domain.glow.EdgeGlowColorOption
import com.evai.rifatcoder.domain.glow.EdgeGlowIntensity
import com.evai.rifatcoder.domain.glow.EdgeGlowPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent storage and reactive delivery of Edge Glow overlay settings.
 */
class EdgeGlowPreferencesRepository(
    context: Context,
    prefsName: String = PREFS_NAME,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(readFromStorage())
    val preferences: StateFlow<EdgeGlowPreferences> = _preferences.asStateFlow()

    fun getPreferences(): EdgeGlowPreferences {
        return _preferences.value
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        updateFlow()
    }

    fun setIntensity(intensity: EdgeGlowIntensity) {
        prefs.edit().putString(KEY_INTENSITY, intensity.id).apply()
        updateFlow()
    }

    fun setColorOption(colorOption: EdgeGlowColorOption) {
        prefs.edit().putString(KEY_COLOR_OPTION, colorOption.id).apply()
        updateFlow()
    }

    fun setShowDuringListening(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LISTENING, show).apply()
        updateFlow()
    }

    fun setShowDuringSpeaking(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SPEAKING, show).apply()
        updateFlow()
    }

    fun setShowDuringThinking(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_THINKING, show).apply()
        updateFlow()
    }

    fun setShowDuringExecuting(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_EXECUTING, show).apply()
        updateFlow()
    }

    fun setSystemOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM_OVERLAY_ENABLED, enabled).apply()
        updateFlow()
    }

    fun resetToDefaults(): EdgeGlowPreferences {
        val defaults = EdgeGlowPreferences()
        prefs.edit()
            .putBoolean(KEY_ENABLED, defaults.isEnabled)
            .putString(KEY_INTENSITY, defaults.intensity.id)
            .putString(KEY_COLOR_OPTION, defaults.colorOption.id)
            .putBoolean(KEY_SHOW_LISTENING, defaults.showDuringListening)
            .putBoolean(KEY_SHOW_SPEAKING, defaults.showDuringSpeaking)
            .putBoolean(KEY_SHOW_THINKING, defaults.showDuringThinking)
            .putBoolean(KEY_SHOW_EXECUTING, defaults.showDuringExecuting)
            .putBoolean(KEY_SYSTEM_OVERLAY_ENABLED, defaults.isSystemOverlayEnabled)
            .apply()
        updateFlow()
        return defaults
    }

    private fun readFromStorage(): EdgeGlowPreferences {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val intensityId = prefs.getString(KEY_INTENSITY, EdgeGlowIntensity.MEDIUM.id)
        val colorId = prefs.getString(KEY_COLOR_OPTION, EdgeGlowColorOption.AUTO.id)
        val showListening = prefs.getBoolean(KEY_SHOW_LISTENING, true)
        val showSpeaking = prefs.getBoolean(KEY_SHOW_SPEAKING, true)
        val showThinking = prefs.getBoolean(KEY_SHOW_THINKING, true)
        val showExecuting = prefs.getBoolean(KEY_SHOW_EXECUTING, true)
        val systemOverlay = prefs.getBoolean(KEY_SYSTEM_OVERLAY_ENABLED, false)

        return EdgeGlowPreferences(
            isEnabled = enabled,
            intensity = EdgeGlowIntensity.fromId(intensityId),
            colorOption = EdgeGlowColorOption.fromId(colorId),
            showDuringListening = showListening,
            showDuringSpeaking = showSpeaking,
            showDuringThinking = showThinking,
            showDuringExecuting = showExecuting,
            isSystemOverlayEnabled = systemOverlay,
        )
    }

    private fun updateFlow() {
        _preferences.value = readFromStorage()
    }

    companion object {
        const val PREFS_NAME = "eva_ai_edge_glow_preferences"

        const val KEY_ENABLED = "key_edge_glow_enabled"
        const val KEY_INTENSITY = "key_edge_glow_intensity"
        const val KEY_COLOR_OPTION = "key_edge_glow_color_option"
        const val KEY_SHOW_LISTENING = "key_edge_glow_show_listening"
        const val KEY_SHOW_SPEAKING = "key_edge_glow_show_speaking"
        const val KEY_SHOW_THINKING = "key_edge_glow_show_thinking"
        const val KEY_SHOW_EXECUTING = "key_edge_glow_show_executing"
        const val KEY_SYSTEM_OVERLAY_ENABLED = "key_edge_glow_system_overlay_enabled"
    }
}
