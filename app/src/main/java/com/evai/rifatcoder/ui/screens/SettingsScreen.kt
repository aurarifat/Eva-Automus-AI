package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.evai.rifatcoder.R
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import androidx.compose.ui.platform.LocalContext
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.data.security.SecureConfigStorage
import com.evai.rifatcoder.ui.components.EvaTopBar
import com.evai.rifatcoder.ui.theme.EvaTheme

import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.evai.rifatcoder.data.database.EvaDatabase
import com.evai.rifatcoder.data.memory.MemoryPreferencesRepository
import com.evai.rifatcoder.data.repository.RealConversationRepository
import com.evai.rifatcoder.data.repository.RealMemoryRepository
import com.evai.rifatcoder.data.security.PermissionItem
import com.evai.rifatcoder.data.security.SystemPermissionManager
import com.evai.rifatcoder.ui.automation.AutomationCoordinator
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToAiConfig: () -> Unit = {},
    onNavigateToVoiceSettings: () -> Unit = {},
    onNavigateToAutomation: () -> Unit = {},
    onNavigateToEdgeGlow: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storage = remember { SecureConfigStorage(context) }
    val activeProvider = storage.getActiveProvider()
    val hasKey = storage.hasApiKey(activeProvider)
    val status = storage.getProviderStatus(activeProvider)

    // Real Memory Repository
    val memoryRepo = remember {
        RealMemoryRepository(
            EvaDatabase.getInstance(context),
            MemoryPreferencesRepository(context)
        )
    }
    val isMemoryEnabled by memoryRepo.isMemoryEnabledFlow.collectAsState()
    val confirmedMemoriesCount by memoryRepo.countConfirmedMemories().collectAsState(initial = 0)

    // Real Permission Auditor
    val permissionManager = remember { SystemPermissionManager(context) }
    var auditedPermissions by remember { mutableStateOf(permissionManager.auditAllPermissions()) }
    val grantedCount = auditedPermissions.count { it.isGranted }
    val totalPermCount = auditedPermissions.size

    // Dialog States
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showDataPrivacyDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val (aiStatusLevel, aiStatusLabel) = when {
        status == ProviderConnectionStatus.CONNECTED -> 
            Pair(EvaStatusLevel.Available, "Connected (${activeProvider.displayName})")
        hasKey -> 
            Pair(EvaStatusLevel.InProgress, "Configured (${activeProvider.displayName})")
        else -> 
            Pair(EvaStatusLevel.NotConfigured, "Not Configured")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings"),
    ) {
        EvaTopBar(
            title = stringResource(R.string.title_settings),
            subtitle = stringResource(R.string.subtitle_settings),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            EvaSectionHeader(title = "Core Configuration")

            SettingsCategoryCard(
                icon = Icons.Default.SmartToy,
                title = "AI Provider",
                description = "Google Gemini & OmniRoute API keys, model presets, and live connection test.",
                status = aiStatusLevel,
                statusLabel = aiStatusLabel,
                onClick = onNavigateToAiConfig,
                testTag = "settings_ai_provider_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Mic,
                title = "Voice & Speech",
                description = "Speech recognition language, TTS pitch & rate, and 'Hey Eva' feasibility.",
                status = EvaStatusLevel.Available,
                statusLabel = "Configured",
                onClick = onNavigateToVoiceSettings,
                testTag = "settings_voice_speech_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            val isAccessibilityActive = remember {
                com.evai.rifatcoder.data.accessibility.EvaAccessibilityService.isServiceEnabled(context)
            }
            SettingsCategoryCard(
                icon = Icons.Default.TouchApp,
                title = "App Control & Automation",
                description = "Android Accessibility Service for hands-free control, screen reading, and app interaction.",
                status = if (isAccessibilityActive) EvaStatusLevel.Available else EvaStatusLevel.Disabled,
                statusLabel = if (isAccessibilityActive) "Active" else "Disabled",
                onClick = onNavigateToAutomation,
                testTag = "settings_automation_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Palette,
                title = "Appearance",
                description = "Luxury dark theme, accent colors (#8A7FFF & #00E5FF), animated orb styling.",
                status = EvaStatusLevel.Available,
                statusLabel = "Dark Active",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            val glowPrefsRepo = remember { com.evai.rifatcoder.data.glow.EdgeGlowPreferencesRepository(context) }
            val glowPrefs = glowPrefsRepo.preferences.value
            val hasOverlayPerm = remember { com.evai.rifatcoder.data.glow.EdgeGlowOverlayService.canDrawOverlays(context) }

            SettingsCategoryCard(
                icon = Icons.Default.DisplaySettings,
                title = "Edge Glow & Voice Status",
                description = "Four-sided animated screen border, visual voice & automation states, and overlay permission.",
                status = if (glowPrefs.isEnabled) EvaStatusLevel.Available else EvaStatusLevel.Disabled,
                statusLabel = if (glowPrefs.isEnabled) {
                    if (hasOverlayPerm && glowPrefs.isSystemOverlayEnabled) "Overlay Active" else "In-App Active"
                } else "Disabled",
                onClick = onNavigateToEdgeGlow,
                testTag = "settings_edge_glow_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            EvaSectionHeader(title = "Security & Privacy")

            SettingsCategoryCard(
                icon = Icons.Default.Lock,
                title = "Memory & Privacy",
                description = "Inspect confirmed memories, review AI suggestions, and manage retention.",
                status = if (isMemoryEnabled) EvaStatusLevel.Available else EvaStatusLevel.Disabled,
                statusLabel = if (isMemoryEnabled) "$confirmedMemoriesCount Confirmed" else "Disabled",
                onClick = onNavigateToMemory,
                testTag = "settings_memory_privacy_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Shield,
                title = "Permission Audit",
                description = "Audit all 17 system permissions, review rationales, and manage Android settings.",
                status = if (grantedCount == totalPermCount) EvaStatusLevel.Available else EvaStatusLevel.InProgress,
                statusLabel = "$grantedCount / $totalPermCount Granted",
                onClick = {
                    auditedPermissions = permissionManager.auditAllPermissions()
                    showPermissionsDialog = true
                },
                testTag = "settings_permissions_audit_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Security,
                title = "Data Management & Wipe",
                description = "Clear conversation history, purge memories, clear automation logs, and reset credentials.",
                status = EvaStatusLevel.Available,
                statusLabel = "Manage",
                onClick = { showDataPrivacyDialog = true },
                testTag = "settings_data_wipe_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            SettingsCategoryCard(
                icon = Icons.Default.Policy,
                title = "Privacy Disclosure & Architecture",
                description = "Local-first storage, AES-256 Keystore encryption, zero background telemetry disclosure.",
                status = EvaStatusLevel.Available,
                statusLabel = "Verified",
                onClick = { showPrivacyPolicyDialog = true },
                testTag = "settings_privacy_policy_card",
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            EvaSectionHeader(title = "System Information")

            EvaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = EvaTheme.extendedColors.surfaceElevated,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SystemInfoRow(label = "Application", value = "EVA AI")
                    SystemInfoRow(label = "Developer", value = "Aura RIFAT")
                    SystemInfoRow(label = "Application ID", value = "com.evai.rifatcoder")
                    SystemInfoRow(label = "Version", value = "1.0.0-alpha (Phase 2)")
                    SystemInfoRow(label = "UI Architecture", value = "Jetpack Compose M3 + MVVM")
                    SystemInfoRow(label = "Target SDK", value = "Android 36 (Release 36.1)")
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }

        // ==========================================
        // 1. PERMISSIONS AUDIT DIALOG
        // ==========================================
        if (showPermissionsDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionsDialog = false },
                title = {
                    Text(
                        text = "System Permissions Audit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Audited $grantedCount granted out of $totalPermCount system permissions. EVA AI operates under a least-privilege security model.",
                            style = MaterialTheme.typography.bodySmall.copy(color = EvaTheme.extendedColors.textSecondary),
                        )
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                        LazyColumn(modifier = Modifier.height(340.dp)) {
                            items(auditedPermissions) { perm ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = EvaTheme.spacing.extraSmall)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = perm.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = EvaTheme.extendedColors.textPrimary,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        )
                                        EvaStatusIndicator(
                                            status = if (perm.isGranted) EvaStatusLevel.Available else EvaStatusLevel.NotConfigured,
                                            customLabel = if (perm.isGranted) "Allowed" else "Pending",
                                            showBorder = false,
                                        )
                                    }
                                    Text(
                                        text = perm.description,
                                        style = MaterialTheme.typography.bodySmall.copy(color = EvaTheme.extendedColors.textSecondary),
                                    )
                                    if (!perm.isGranted) {
                                        Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
                                        OutlinedButton(
                                            onClick = {
                                                permissionManager.openSpecialPermissionSettings(perm.id)
                                                showPermissionsDialog = false
                                            },
                                            modifier = Modifier.height(32.dp),
                                        ) {
                                            Text("Open Settings", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = EvaTheme.spacing.extraSmall),
                                        color = EvaTheme.extendedColors.borderSubtle,
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPermissionsDialog = false }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        permissionManager.openSpecialPermissionSettings("")
                        showPermissionsDialog = false
                    }) {
                        Text("App Details")
                    }
                }
            )
        }

        // ==========================================
        // 2. DATA MANAGEMENT & WIPE DIALOG
        // ==========================================
        if (showDataPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showDataPrivacyDialog = false },
                title = {
                    Text(
                        text = "Data Management & Privacy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "EVA AI stores conversations and personal memories strictly in local on-device SQLite databases. Use the actions below to securely clear local data.",
                            style = MaterialTheme.typography.bodySmall.copy(color = EvaTheme.extendedColors.textSecondary),
                        )
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                        // Clear Conversations
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val repo = RealConversationRepository(EvaDatabase.getInstance(context))
                                    repo.clearAllConversations()
                                    showDataPrivacyDialog = false
                                    feedbackMessage = "All conversation history has been permanently cleared."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EvaTheme.extendedColors.surfaceCard),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Clear All Conversation History", color = EvaTheme.extendedColors.error)
                        }

                        Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                        // Clear Memories
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    memoryRepo.deleteAllMemories()
                                    showDataPrivacyDialog = false
                                    feedbackMessage = "All personal memories have been purged."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EvaTheme.extendedColors.surfaceCard),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Purge Saved Memories", color = EvaTheme.extendedColors.error)
                        }

                        Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                        // Clear Automation History
                        Button(
                            onClick = {
                                AutomationCoordinator(context).clearHistory()
                                showDataPrivacyDialog = false
                                feedbackMessage = "Task automation execution history cleared."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EvaTheme.extendedColors.surfaceCard),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Clear Task Execution History", color = EvaTheme.extendedColors.textPrimary)
                        }

                        Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                        // Wipe AI Credentials
                        Button(
                            onClick = {
                                storage.clearGeminiApiKey()
                                storage.clearOmniRouteApiKey()
                                showDataPrivacyDialog = false
                                feedbackMessage = "Stored API keys wiped from Android KeyStore."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EvaTheme.extendedColors.surfaceCard),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Wipe Stored API Credentials", color = EvaTheme.extendedColors.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDataPrivacyDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // ==========================================
        // 3. PRIVACY DISCLOSURE & ARCHITECTURE DIALOG
        // ==========================================
        if (showPrivacyPolicyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyPolicyDialog = false },
                title = {
                    Text(
                        text = "EVA AI Privacy Architecture",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "• Local-First Data Storage:\nAll chat sessions, personal memory vectors, and task logs are persisted solely on your device within an encrypted private Room database.\n\n" +
                                    "• Android KeyStore Encryption:\nGemini and OmniRoute API credentials are encrypted with AES-256-GCM. Secret keys never leave your device or appear in logcat.\n\n" +
                                    "• Incognito Mode Isolation:\nWhen Incognito mode is active, conversation turns are never saved to disk and personal memory extraction is completely disabled.\n\n" +
                                    "• Accessibility Field Protection:\nPassword fields, PIN codes, OTPs, and credential inputs are strictly masked and never read or exposed.\n\n" +
                                    "• Non-Intrusive Edge Glow:\nThe four-side edge glow window uses touch-passthrough flags (FLAG_NOT_TOUCHABLE), ensuring zero tapjacking and zero UI interference.",
                            style = MaterialTheme.typography.bodySmall.copy(color = EvaTheme.extendedColors.textSecondary),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                        Text("Understood")
                    }
                }
            )
        }

        // Feedback Snackbar / Notice Dialog
        if (feedbackMessage != null) {
            AlertDialog(
                onDismissRequest = { feedbackMessage = null },
                title = { Text("Privacy Action Completed") },
                text = { Text(feedbackMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { feedbackMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: EvaStatusLevel,
    statusLabel: String,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EvaTheme.extendedColors.primaryPurple,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(EvaTheme.spacing.standard))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    EvaStatusIndicator(
                        status = status,
                        customLabel = statusLabel,
                        showBorder = false,
                    )
                }
                Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SystemInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EvaTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = EvaTheme.extendedColors.textSecondary,
            ),
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = EvaTheme.extendedColors.textPrimary,
            ),
            modifier = Modifier.weight(0.55f),
        )
    }
}
