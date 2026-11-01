package com.evai.rifatcoder.ui.config

import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.ProviderConnectionStatus

data class AiConfigUiState(
    val selectedProvider: AiProviderType = AiProviderType.GEMINI,
    val geminiApiKeyInput: String = "",
    val geminiModelIdInput: String = "gemini-3.5-flash",
    val hasStoredGeminiKey: Boolean = false,
    val geminiStatus: ProviderConnectionStatus = ProviderConnectionStatus.NOT_CONFIGURED,

    val omniRouteApiKeyInput: String = "",
    val omniRouteModelIdInput: String = "omni-default",
    val omniRouteBaseUrlInput: String = "https://api.omniroute.ai/v1/",
    val hasStoredOmniRouteKey: Boolean = false,
    val omniRouteStatus: ProviderConnectionStatus = ProviderConnectionStatus.NOT_CONFIGURED,

    val isKeyVisible: Boolean = false,
    val isTesting: Boolean = false,
    val testResultMessage: String? = null,
    val testSuccess: Boolean? = null,
    val showRemoveKeyConfirmDialog: Boolean = false,
    val userFeedbackMessage: String? = null,
) {
    val currentApiKeyInput: String
        get() = when (selectedProvider) {
            AiProviderType.GEMINI -> geminiApiKeyInput
            AiProviderType.OMNIROUTE -> omniRouteApiKeyInput
        }

    val currentModelIdInput: String
        get() = when (selectedProvider) {
            AiProviderType.GEMINI -> geminiModelIdInput
            AiProviderType.OMNIROUTE -> omniRouteModelIdInput
        }

    val hasStoredKeyForSelectedProvider: Boolean
        get() = when (selectedProvider) {
            AiProviderType.GEMINI -> hasStoredGeminiKey
            AiProviderType.OMNIROUTE -> hasStoredOmniRouteKey
        }

    val currentStatus: ProviderConnectionStatus
        get() = when (selectedProvider) {
            AiProviderType.GEMINI -> geminiStatus
            AiProviderType.OMNIROUTE -> omniRouteStatus
        }

    val knownModels: List<String>
        get() = when (selectedProvider) {
            AiProviderType.GEMINI -> listOf(
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-flash-latest",
                "gemini-2.5-flash-image",
            )
            AiProviderType.OMNIROUTE -> listOf(
                "omni-default",
                "claude-3-5-sonnet",
                "gpt-4o",
                "gemini-3.5-flash",
            )
        }
}
