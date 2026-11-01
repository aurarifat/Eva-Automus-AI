package com.evai.rifatcoder.ui.home

import androidx.compose.ui.graphics.vector.ImageVector

enum class AssistantState(val label: String, val description: String) {
    Ready("Assistant Ready", "Core system initialized and standing by"),
    SetupRequired("Setup Required", "Configure an AI provider to enable smart assistance"),
    VoiceNotAvailable("Voice Not Available", "Voice synthesis engine is currently inactive"),
    Listening("Listening", "Microphone audio stream active"),
    Thinking("Thinking", "Processing context and synthesizing response"),
    Speaking("Speaking", "Delivering neural voice audio"),
    Executing("Executing", "Executing device automation workflow"),
    Error("System Error", "An error occurred during operation"),
}

enum class AiConnectionState(val label: String, val detail: String) {
    NotConfigured("Not Configured", "No AI provider or API credentials installed"),
    NotTested("Not Tested", "Provider credential present but unverified"),
    Connected("Connected", "Successfully authenticated with remote provider"),
    AuthenticationFailed("Authentication Failed", "API key or token rejected"),
    NetworkError("Network Error", "Unable to establish provider socket connection"),
    ProviderError("Provider Error", "Upstream server returned operational error"),
}

enum class PermissionSummaryState(val label: String) {
    SetupRequired("Setup Required"),
    SomePermissionsMissing("Some Permissions Missing"),
    ReadyForConfiguredFeatures("Ready for Configured Features"),
}

data class RecentConversationItem(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val formattedTimestamp: String,
)

data class QuickToolItem(
    val id: String,
    val name: String,
    val phaseTag: String,
    val description: String,
)

data class HomeUiState(
    val greeting: String = "Good day",
    val greetingPrompt: String = "What would you like me to do?",
    val assistantState: AssistantState = AssistantState.SetupRequired,
    val aiConnectionState: AiConnectionState = AiConnectionState.NotConfigured,
    val permissionState: PermissionSummaryState = PermissionSummaryState.SetupRequired,
    val grantedPermissionCount: Int = 0,
    val totalPermissionCount: Int = 14,
    val recentConversations: List<RecentConversationItem> = emptyList(),
    val isPersistentVoiceModeOn: Boolean = false,
    val showStartupPermissionModal: Boolean = false,
    val permissionItems: List<com.evai.rifatcoder.data.security.PermissionItem> = emptyList(),
)
