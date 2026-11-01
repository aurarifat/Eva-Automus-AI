package com.evai.rifatcoder.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.repository.ConversationRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.data.security.SecureConfigStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val conversationRepository: ConversationRepository = RealConversationRepository(
        EvaDatabase.getInstance(application)
    ),
) : AndroidViewModel(application) {

    private val storage = SecureConfigStorage(application)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            greeting = resolveTimeGreeting(),
            assistantState = AssistantState.SetupRequired,
            aiConnectionState = AiConnectionState.NotConfigured,
            permissionState = PermissionSummaryState.SetupRequired,
            grantedPermissionCount = 0,
            totalPermissionCount = 14,
            recentConversations = emptyList(),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val permissionManager = com.evai.rifatcoder.data.security.SystemPermissionManager(application)
    private var hasPromptedPermissions = false

    init {
        refreshDashboardState()
        observeRecentConversations()
        observeVoiceMode()
    }

    private fun observeVoiceMode() {
        viewModelScope.launch {
            com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.isPersistentVoiceModeActive.collect { isActive ->
                _uiState.update { it.copy(isPersistentVoiceModeOn = isActive) }
            }
        }
    }

    fun toggleVoiceMode() {
        val next = !_uiState.value.isPersistentVoiceModeOn
        com.evai.rifatcoder.data.glow.EdgeGlowStateHolder.setPersistentVoiceMode(next)
        if (next) {
            com.evai.rifatcoder.data.voice.CentralVoiceStateManager.notifyListening()
        } else {
            com.evai.rifatcoder.data.voice.CentralVoiceStateManager.stopAll()
        }
    }

    fun dismissPermissionModal() {
        _uiState.update { it.copy(showStartupPermissionModal = false) }
    }

    fun openSpecialPermission(permissionId: String) {
        permissionManager.openSpecialPermissionSettings(permissionId)
    }

    private fun observeRecentConversations() {
        viewModelScope.launch {
            conversationRepository.getRecentConversations(3).collect { entities ->
                val recentItems = entities.map { entity ->
                    RecentConversationItem(
                        id = entity.id,
                        title = entity.title,
                        lastMessagePreview = entity.lastMessagePreview ?: "No messages yet",
                        formattedTimestamp = formatTimestamp(entity.updatedAt),
                    )
                }
                _uiState.update { it.copy(recentConversations = recentItems) }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val oneDay = 24 * 60 * 60 * 1000L
        val date = Date(timestamp)
        return when {
            diff < oneDay -> {
                val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                format.format(date)
            }
            diff < 2 * oneDay -> "Yesterday"
            else -> {
                val format = SimpleDateFormat("MMM d", Locale.getDefault())
                format.format(date)
            }
        }
    }

    fun refreshDashboardState() {
        val audited = permissionManager.auditAllPermissions()
        val grantedCount = audited.count { it.isGranted }
        val totalCount = audited.size

        val shouldPrompt = !hasPromptedPermissions && grantedCount < 3
        if (shouldPrompt) {
            hasPromptedPermissions = true
        }

        val permState = when {
            grantedCount == totalCount -> PermissionSummaryState.ReadyForConfiguredFeatures
            grantedCount > 0 -> PermissionSummaryState.SomePermissionsMissing
            else -> PermissionSummaryState.SetupRequired
        }

        val activeProvider = storage.getActiveProvider()
        val hasKey = storage.hasApiKey(activeProvider)
        val providerStatus = storage.getProviderStatus(activeProvider)

        val aiConnState = when {
            !hasKey -> AiConnectionState.NotConfigured
            providerStatus == ProviderConnectionStatus.CONNECTED -> AiConnectionState.Connected
            providerStatus == ProviderConnectionStatus.CONNECTION_FAILED -> AiConnectionState.NetworkError
            else -> AiConnectionState.NotTested
        }

        val assistantState = when {
            aiConnState == AiConnectionState.Connected -> AssistantState.Ready
            hasKey -> AssistantState.Ready
            else -> AssistantState.SetupRequired
        }

        _uiState.update { currentState ->
            currentState.copy(
                greeting = resolveTimeGreeting(),
                assistantState = assistantState,
                aiConnectionState = aiConnState,
                permissionState = permState,
                grantedPermissionCount = grantedCount,
                totalPermissionCount = totalCount,
                permissionItems = audited,
                showStartupPermissionModal = shouldPrompt || currentState.showStartupPermissionModal,
            )
        }
    }

    fun resolveTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
