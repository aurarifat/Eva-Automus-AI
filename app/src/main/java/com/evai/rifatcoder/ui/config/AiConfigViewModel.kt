package com.evai.rifatcoder.ui.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.network.GeminiProviderAdapter
import com.evai.rifatcoder.data.network.OmniRouteProviderAdapter
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.domain.ai.AiProviderAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiConfigViewModel @JvmOverloads constructor(
    application: Application,
    private val storage: SecureConfigStorage = SecureConfigStorage(application),
    private val geminiAdapter: AiProviderAdapter = GeminiProviderAdapter(),
    private val omniRouteAdapter: AiProviderAdapter = OmniRouteProviderAdapter(),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AiConfigUiState(
            selectedProvider = storage.getActiveProvider(),
            geminiApiKeyInput = "",
            geminiModelIdInput = storage.getGeminiModelId(),
            hasStoredGeminiKey = storage.hasGeminiApiKey(),
            geminiStatus = storage.getProviderStatus(AiProviderType.GEMINI),

            omniRouteApiKeyInput = "",
            omniRouteModelIdInput = storage.getOmniRouteModelId(),
            omniRouteBaseUrlInput = storage.getOmniRouteBaseUrl(),
            hasStoredOmniRouteKey = storage.hasOmniRouteApiKey(),
            omniRouteStatus = storage.getProviderStatus(AiProviderType.OMNIROUTE),
        )
    )
    val uiState: StateFlow<AiConfigUiState> = _uiState.asStateFlow()

    fun selectProvider(providerType: AiProviderType) {
        storage.setActiveProvider(providerType)
        _uiState.update {
            it.copy(
                selectedProvider = providerType,
                testResultMessage = null,
                testSuccess = null,
            )
        }
    }

    fun updateApiKey(newKey: String) {
        val currentProvider = _uiState.value.selectedProvider
        // Invalidate previous test status when key is modified
        storage.setProviderStatus(currentProvider, ProviderConnectionStatus.CONFIGURATION_CHANGED)

        _uiState.update { current ->
            when (currentProvider) {
                AiProviderType.GEMINI -> current.copy(
                    geminiApiKeyInput = newKey,
                    geminiStatus = ProviderConnectionStatus.CONFIGURATION_CHANGED,
                    testResultMessage = null,
                    testSuccess = null,
                )
                AiProviderType.OMNIROUTE -> current.copy(
                    omniRouteApiKeyInput = newKey,
                    omniRouteStatus = ProviderConnectionStatus.CONFIGURATION_CHANGED,
                    testResultMessage = null,
                    testSuccess = null,
                )
            }
        }
    }

    fun updateModelId(newModelId: String) {
        val currentProvider = _uiState.value.selectedProvider
        storage.setProviderStatus(currentProvider, ProviderConnectionStatus.CONFIGURATION_CHANGED)

        _uiState.update { current ->
            when (currentProvider) {
                AiProviderType.GEMINI -> current.copy(
                    geminiModelIdInput = newModelId,
                    geminiStatus = ProviderConnectionStatus.CONFIGURATION_CHANGED,
                    testResultMessage = null,
                    testSuccess = null,
                )
                AiProviderType.OMNIROUTE -> current.copy(
                    omniRouteModelIdInput = newModelId,
                    omniRouteStatus = ProviderConnectionStatus.CONFIGURATION_CHANGED,
                    testResultMessage = null,
                    testSuccess = null,
                )
            }
        }
    }

    fun updateOmniRouteBaseUrl(newUrl: String) {
        storage.setProviderStatus(AiProviderType.OMNIROUTE, ProviderConnectionStatus.CONFIGURATION_CHANGED)
        _uiState.update { current ->
            current.copy(
                omniRouteBaseUrlInput = newUrl,
                omniRouteStatus = ProviderConnectionStatus.CONFIGURATION_CHANGED,
                testResultMessage = null,
                testSuccess = null,
            )
        }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun saveConfiguration() {
        val state = _uiState.value
        val provider = state.selectedProvider

        when (provider) {
            AiProviderType.GEMINI -> {
                if (state.geminiApiKeyInput.isNotBlank()) {
                    storage.saveGeminiApiKey(state.geminiApiKeyInput)
                }
                storage.saveGeminiModelId(state.geminiModelIdInput)
                _uiState.update {
                    it.copy(
                        hasStoredGeminiKey = storage.hasGeminiApiKey(),
                        userFeedbackMessage = "Gemini configuration saved securely.",
                    )
                }
            }
            AiProviderType.OMNIROUTE -> {
                if (state.omniRouteApiKeyInput.isNotBlank()) {
                    storage.saveOmniRouteApiKey(state.omniRouteApiKeyInput)
                }
                storage.saveOmniRouteModelId(state.omniRouteModelIdInput)
                storage.saveOmniRouteBaseUrl(state.omniRouteBaseUrlInput)
                _uiState.update {
                    it.copy(
                        hasStoredOmniRouteKey = storage.hasOmniRouteApiKey(),
                        userFeedbackMessage = "OmniRoute configuration saved securely.",
                    )
                }
            }
        }
    }

    fun testConnection() {
        val state = _uiState.value
        val provider = state.selectedProvider

        val apiKey = when (provider) {
            AiProviderType.GEMINI -> state.geminiApiKeyInput.ifBlank { storage.getGeminiApiKey().orEmpty() }
            AiProviderType.OMNIROUTE -> state.omniRouteApiKeyInput.ifBlank { storage.getOmniRouteApiKey().orEmpty() }
        }

        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    testSuccess = false,
                    testResultMessage = "Cannot test connection: No API key provided or saved for ${provider.displayName}.",
                )
            }
            return
        }

        val modelId = when (provider) {
            AiProviderType.GEMINI -> state.geminiModelIdInput
            AiProviderType.OMNIROUTE -> state.omniRouteModelIdInput
        }

        val baseUrl = if (provider == AiProviderType.OMNIROUTE) state.omniRouteBaseUrlInput else null
        val adapter = if (provider == AiProviderType.GEMINI) geminiAdapter else omniRouteAdapter

        _uiState.update {
            it.copy(
                isTesting = true,
                testResultMessage = "Testing connection to ${provider.displayName}...",
                testSuccess = null,
            )
        }

        viewModelScope.launch {
            val result = adapter.testConnection(
                apiKey = apiKey,
                modelId = modelId,
                baseUrl = baseUrl,
            )

            result.fold(
                onSuccess = {
                    storage.setProviderStatus(provider, ProviderConnectionStatus.CONNECTED)
                    _uiState.update { current ->
                        when (provider) {
                            AiProviderType.GEMINI -> current.copy(
                                isTesting = false,
                                testSuccess = true,
                                geminiStatus = ProviderConnectionStatus.CONNECTED,
                                testResultMessage = "Successfully connected to Google Gemini using model '$modelId'.",
                            )
                            AiProviderType.OMNIROUTE -> current.copy(
                                isTesting = false,
                                testSuccess = true,
                                omniRouteStatus = ProviderConnectionStatus.CONNECTED,
                                testResultMessage = "Successfully connected to OmniRoute using model '$modelId'.",
                            )
                        }
                    }
                },
                onFailure = { error ->
                    storage.setProviderStatus(provider, ProviderConnectionStatus.CONNECTION_FAILED)
                    val sanitizedMsg = storage.sanitizeError(error)
                    _uiState.update { current ->
                        when (provider) {
                            AiProviderType.GEMINI -> current.copy(
                                isTesting = false,
                                testSuccess = false,
                                geminiStatus = ProviderConnectionStatus.CONNECTION_FAILED,
                                testResultMessage = "Connection failed: $sanitizedMsg",
                            )
                            AiProviderType.OMNIROUTE -> current.copy(
                                isTesting = false,
                                testSuccess = false,
                                omniRouteStatus = ProviderConnectionStatus.CONNECTION_FAILED,
                                testResultMessage = "Connection failed: $sanitizedMsg",
                            )
                        }
                    }
                }
            )
        }
    }

    fun requestRemoveKey() {
        _uiState.update { it.copy(showRemoveKeyConfirmDialog = true) }
    }

    fun confirmRemoveKey() {
        val provider = _uiState.value.selectedProvider
        when (provider) {
            AiProviderType.GEMINI -> {
                storage.clearGeminiApiKey()
                _uiState.update {
                    it.copy(
                        geminiApiKeyInput = "",
                        hasStoredGeminiKey = false,
                        geminiStatus = ProviderConnectionStatus.NOT_CONFIGURED,
                        showRemoveKeyConfirmDialog = false,
                        testResultMessage = "Gemini API key removed.",
                        testSuccess = null,
                    )
                }
            }
            AiProviderType.OMNIROUTE -> {
                storage.clearOmniRouteApiKey()
                _uiState.update {
                    it.copy(
                        omniRouteApiKeyInput = "",
                        hasStoredOmniRouteKey = false,
                        omniRouteStatus = ProviderConnectionStatus.NOT_CONFIGURED,
                        showRemoveKeyConfirmDialog = false,
                        testResultMessage = "OmniRoute API key removed.",
                        testSuccess = null,
                    )
                }
            }
        }
    }

    fun dismissRemoveKeyDialog() {
        _uiState.update { it.copy(showRemoveKeyConfirmDialog = false) }
    }

    fun dismissFeedbackMessage() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }
}
