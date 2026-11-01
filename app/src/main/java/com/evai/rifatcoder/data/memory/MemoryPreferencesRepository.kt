package com.evai.rifatcoder.data.memory

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user preferences for the Personal Memory System.
 * Persists the master ON/OFF toggle securely on-device.
 */
class MemoryPreferencesRepository(
    context: Context,
    prefsName: String = PREFS_NAME,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _isMemoryEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED)
    )
    val isMemoryEnabledFlow: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    fun isMemoryEnabled(): Boolean {
        return prefs.getBoolean(KEY_MEMORY_ENABLED, DEFAULT_MEMORY_ENABLED)
    }

    fun setMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
        _isMemoryEnabled.value = enabled
    }

    companion object {
        const val PREFS_NAME = "eva_memory_prefs"
        const val KEY_MEMORY_ENABLED = "personal_memory_enabled"
        const val DEFAULT_MEMORY_ENABLED = true
    }
}
