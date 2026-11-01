package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.R
import com.evai.rifatcoder.ui.components.EvaAnimatedOrb
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaIconButton
import com.evai.rifatcoder.ui.components.EvaPrimaryButton
import com.evai.rifatcoder.ui.components.EvaSecondaryButton
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.home.AiConnectionState
import com.evai.rifatcoder.ui.home.AssistantState
import com.evai.rifatcoder.ui.home.HomeViewModel
import com.evai.rifatcoder.ui.home.PermissionSummaryState
import com.evai.rifatcoder.ui.theme.EvaTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToChatWithId: (String) -> Unit = { onNavigateToChat() },
    onNavigateToHistory: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToAiConfig: () -> Unit = onNavigateToSettings,
    onNavigateToAutomation: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        viewModel.refreshDashboardState()
    }

    val uiState by viewModel.uiState.collectAsState()
    var toolInfoDialogText by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_home"),
    ) {
        // SECTION A — TOP APP BAR
        HomeTopAppBar(
            onHomeClick = onNavigateToHome,
            onSettingsClick = onNavigateToSettings,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            // SECTION B — PERSONALIZED GREETING
            PersonalizedGreetingSection(
                greeting = uiState.greeting,
                prompt = uiState.greetingPrompt,
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // SECTION C — CENTRAL AI VISUAL / VOICE ORB
            CentralVoiceOrbSection(
                onOrbClick = { viewModel.toggleVoiceMode() },
            )

            if (uiState.isPersistentVoiceModeOn) {
                EvaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = EvaTheme.spacing.small)
                        .clickable { viewModel.toggleVoiceMode() }
                        .testTag("home_voice_mode_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = EvaTheme.extendedColors.secondaryCyan,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Mode Active • 4-Edge Glow ON",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = EvaTheme.extendedColors.secondaryCyan,
                            )
                            Text(
                                text = "Listening for commands & screen reading. Tap here to turn OFF.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = EvaTheme.extendedColors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            // SECTION D — ASSISTANT STATUS
            AssistantStatusSection(
                assistantState = if (uiState.isPersistentVoiceModeOn) AssistantState.Listening else uiState.assistantState,
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

            // SECTION E — PRIMARY ACTION BUTTONS
            PrimaryActionsSection(
                onOpenChat = onNavigateToChat,
                onOpenVoice = onNavigateToVoice,
                isVoiceModeOn = uiState.isPersistentVoiceModeOn,
                onToggleVoiceMode = { viewModel.toggleVoiceMode() },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

            // SECTION F — AI CONNECTION CARD
            AiConnectionCard(
                connectionState = uiState.aiConnectionState,
                onConfigureAi = onNavigateToAiConfig,
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // SECTION G — PERMISSION STATUS CARD
            PermissionStatusCard(
                permissionState = uiState.permissionState,
                grantedCount = uiState.grantedPermissionCount,
                totalCount = uiState.totalPermissionCount,
                onManagePermissions = onNavigateToSettings,
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // SECTION H — RECENT CONVERSATIONS
            RecentConversationsSection(
                conversations = uiState.recentConversations,
                onViewAllClick = onNavigateToHistory,
                onConversationClick = onNavigateToChatWithId,
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // SECTION I — QUICK TOOLS
            QuickToolsSection(
                onToolClick = { toolId ->
                    when (toolId) {
                        "memory" -> onNavigateToMemory()
                        "integrations" -> onNavigateToSettings()
                        "device_tools" -> {
                            toolInfoDialogText = Pair(
                                "Device Tools",
                                "Device hardware controllers (Flashlight, Wi-Fi, Bluetooth, Audio modes) are scheduled for implementation in Phase 6."
                            )
                        }
                        "automation" -> onNavigateToAutomation()
                    }
                }
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

            // Bottom Tagline & Version
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = EvaTheme.extendedColors.textSecondary,
                ),
            )
            Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.app_version_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = EvaTheme.extendedColors.textDisabled,
                ),
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }
    }

    // Modal dialog for upcoming tool info
    if (toolInfoDialogText != null) {
        val (title, body) = toolInfoDialogText!!
        AlertDialog(
            onDismissRequest = { toolInfoDialogText = null },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                )
            },
            text = {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { toolInfoDialogText = null }) {
                    Text(
                        text = "Understood",
                        color = EvaTheme.extendedColors.primaryPurple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            shape = EvaTheme.shapes.large,
        )
    }

    // Startup system permissions and access levels dialog
    if (uiState.showStartupPermissionModal) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionModal() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.primaryPurple,
                    )
                    Text(
                        text = "System Access & Permissions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "EVA AI requires system authorizations to execute automation tasks, edge glow overlays, and voice commands.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EvaTheme.extendedColors.textSecondary,
                    )

                    uiState.permissionItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = EvaTheme.extendedColors.surfaceCard,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = EvaTheme.extendedColors.textPrimary,
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = EvaTheme.extendedColors.textSecondary,
                                    maxLines = 2,
                                )
                            }
                            if (item.isGranted) {
                                Text(
                                    text = "Active",
                                    color = EvaTheme.extendedColors.success,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            } else {
                                TextButton(
                                    onClick = { viewModel.openSpecialPermission(item.id) },
                                ) {
                                    Text(
                                        text = "Enable",
                                        color = EvaTheme.extendedColors.secondaryCyan,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPermissionModal() }) {
                    Text(
                        text = "Continue",
                        color = EvaTheme.extendedColors.primaryPurple,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            shape = EvaTheme.shapes.large,
        )
    }
}

// SECTION A: TOP APP BAR
@Composable
private fun HomeTopAppBar(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val cdLogo = stringResource(R.string.cd_home_logo)
    val cdSettings = stringResource(R.string.cd_settings_button)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EvaTheme.extendedColors.backgroundMain)
            .statusBarsPadding()
            .padding(
                horizontal = EvaTheme.spacing.standard,
                vertical = EvaTheme.spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    role = Role.Button,
                    onClick = onHomeClick,
                )
                .semantics { contentDescription = cdLogo }
                .padding(vertical = EvaTheme.spacing.extraSmall, horizontal = EvaTheme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                EvaTheme.extendedColors.primaryPurple,
                                EvaTheme.extendedColors.secondaryCyan,
                            )
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(EvaTheme.spacing.small))

            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                        letterSpacing = 0.5.sp,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    ),
                    maxLines = 1,
                )
            }
        }

        EvaIconButton(
            icon = Icons.Default.Settings,
            contentDescription = cdSettings,
            onClick = onSettingsClick,
            tint = EvaTheme.extendedColors.textSecondary,
            containerColor = EvaTheme.extendedColors.surfaceCard,
            testTag = "home_settings_button",
        )
    }
}

// SECTION B: PERSONALIZED GREETING
@Composable
private fun PersonalizedGreetingSection(
    greeting: String,
    prompt: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EvaTheme.spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = EvaTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = EvaTheme.extendedColors.secondaryCyan,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

// SECTION C: CENTRAL AI VISUAL / VOICE ORB
@Composable
private fun CentralVoiceOrbSection(
    onOrbClick: () -> Unit,
) {
    val cdOrb = stringResource(R.string.cd_voice_orb)

    Box(
        modifier = Modifier
            .padding(vertical = EvaTheme.spacing.small)
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClick = onOrbClick,
            )
            .semantics { contentDescription = cdOrb }
            .testTag("home_central_voice_orb"),
        contentAlignment = Alignment.Center,
    ) {
        EvaAnimatedOrb(
            size = 172.dp,
        )
    }
}

// SECTION D: ASSISTANT STATUS
@Composable
private fun AssistantStatusSection(
    assistantState: AssistantState,
) {
    val statusLevel = when (assistantState) {
        AssistantState.Ready -> EvaStatusLevel.Available
        AssistantState.SetupRequired -> EvaStatusLevel.NotConfigured
        AssistantState.VoiceNotAvailable -> EvaStatusLevel.Disabled
        AssistantState.Listening -> EvaStatusLevel.InProgress
        AssistantState.Thinking -> EvaStatusLevel.InProgress
        AssistantState.Speaking -> EvaStatusLevel.Available
        AssistantState.Executing -> EvaStatusLevel.InProgress
        AssistantState.Error -> EvaStatusLevel.Error
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = EvaTheme.spacing.standard),
    ) {
        EvaStatusIndicator(
            status = statusLevel,
            customLabel = assistantState.label,
        )
        Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
        Text(
            text = assistantState.description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = EvaTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// SECTION E: PRIMARY ACTION BUTTONS
@Composable
private fun PrimaryActionsSection(
    onOpenChat: () -> Unit,
    onOpenVoice: () -> Unit,
    isVoiceModeOn: Boolean = false,
    onToggleVoiceMode: () -> Unit = onOpenVoice,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_primary_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
    ) {
        EvaPrimaryButton(
            text = stringResource(R.string.action_open_chat),
            onClick = onOpenChat,
            leadingIcon = Icons.Default.ChatBubble,
            modifier = Modifier.weight(1f),
            testTag = "home_action_open_chat",
        )

        EvaSecondaryButton(
            text = if (isVoiceModeOn) "Stop Voice Mode" else "Voice Mode (Edge Glow)",
            onClick = onToggleVoiceMode,
            leadingIcon = Icons.Default.Mic,
            modifier = Modifier.weight(1f),
            containerColor = if (isVoiceModeOn) EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.25f) else EvaTheme.extendedColors.surfaceCard,
            contentColor = if (isVoiceModeOn) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textPrimary,
            testTag = "home_action_open_voice",
        )
    }
}

// SECTION F: AI CONNECTION CARD
@Composable
private fun AiConnectionCard(
    connectionState: AiConnectionState,
    onConfigureAi: () -> Unit,
) {
    val statusLevel = when (connectionState) {
        AiConnectionState.Connected -> EvaStatusLevel.Available
        AiConnectionState.NotTested -> EvaStatusLevel.InProgress
        AiConnectionState.NotConfigured -> EvaStatusLevel.NotConfigured
        AiConnectionState.AuthenticationFailed -> EvaStatusLevel.Error
        AiConnectionState.NetworkError -> EvaStatusLevel.Error
        AiConnectionState.ProviderError -> EvaStatusLevel.Error
    }

    EvaCard(
        modifier = Modifier.fillMaxWidth().testTag("home_ai_connection_card"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.primaryPurple,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.title_ai_connection),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                    )
                }

                EvaStatusIndicator(
                    status = statusLevel,
                    customLabel = connectionState.label,
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            Text(
                text = connectionState.detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                    lineHeight = 18.sp,
                ),
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            EvaSecondaryButton(
                text = stringResource(R.string.btn_configure_ai),
                onClick = onConfigureAi,
                modifier = Modifier.fillMaxWidth(),
                testTag = "home_btn_configure_ai",
            )
        }
    }
}

// SECTION G: PERMISSION STATUS CARD
@Composable
private fun PermissionStatusCard(
    permissionState: PermissionSummaryState,
    grantedCount: Int,
    totalCount: Int,
    onManagePermissions: () -> Unit,
) {
    val statusLevel = when (permissionState) {
        PermissionSummaryState.ReadyForConfiguredFeatures -> EvaStatusLevel.Available
        PermissionSummaryState.SomePermissionsMissing -> EvaStatusLevel.InProgress
        PermissionSummaryState.SetupRequired -> EvaStatusLevel.NotConfigured
    }

    EvaCard(
        modifier = Modifier.fillMaxWidth().testTag("home_permission_card"),
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.secondaryCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.title_permissions),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                    )
                }

                EvaStatusIndicator(
                    status = statusLevel,
                    customLabel = "${permissionState.label} ($grantedCount/$totalCount)",
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

            Text(
                text = stringResource(R.string.permissions_desc),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EvaTheme.extendedColors.textSecondary,
                    lineHeight = 18.sp,
                ),
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            EvaSecondaryButton(
                text = stringResource(R.string.btn_manage_permissions),
                onClick = onManagePermissions,
                modifier = Modifier.fillMaxWidth(),
                testTag = "home_btn_manage_permissions",
            )
        }
    }
}

// SECTION H: RECENT CONVERSATIONS
@Composable
private fun RecentConversationsSection(
    conversations: List<com.evai.rifatcoder.ui.home.RecentConversationItem>,
    onViewAllClick: () -> Unit,
    onConversationClick: (String) -> Unit,
) {
    EvaCard(
        modifier = Modifier.fillMaxWidth().testTag("home_recent_conversations_card"),
        backgroundColor = EvaTheme.extendedColors.surfaceElevated,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            EvaSectionHeader(
                title = stringResource(R.string.title_recent_conversations),
                trailingContent = {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.primaryPurple,
                        ),
                        modifier = Modifier
                            .clickable(onClick = onViewAllClick)
                            .testTag("home_view_all_history_button")
                            .padding(4.dp),
                    )
                }
            )

            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = EvaTheme.spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.textDisabled,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.empty_conversations_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = EvaTheme.extendedColors.textSecondary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.empty_conversations_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textDisabled,
                        ),
                    )
                }
            } else {
                conversations.take(3).forEach { conversation ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onConversationClick(conversation.id) }
                            .padding(vertical = EvaTheme.spacing.extraSmall, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = conversation.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EvaTheme.extendedColors.textPrimary,
                                ),
                            )
                            Text(
                                text = conversation.lastMessagePreview,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EvaTheme.extendedColors.textSecondary,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = conversation.formattedTimestamp,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EvaTheme.extendedColors.textDisabled,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// SECTION I: QUICK TOOLS
@Composable
private fun QuickToolsSection(
    onToolClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        EvaSectionHeader(title = stringResource(R.string.title_quick_tools))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
        ) {
            QuickToolCard(
                icon = Icons.Default.Build,
                label = stringResource(R.string.tool_device_tools),
                phaseTag = "Phase 6",
                onClick = { onToolClick("device_tools") },
                modifier = Modifier.weight(1f),
            )
            QuickToolCard(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.AltRoute,
                label = stringResource(R.string.tool_automation),
                phaseTag = "Phase 7",
                onClick = { onToolClick("automation") },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
        ) {
            QuickToolCard(
                icon = Icons.Default.Psychology,
                label = stringResource(R.string.tool_memory),
                phaseTag = "Phase 5",
                onClick = { onToolClick("memory") },
                modifier = Modifier.weight(1f),
            )
            QuickToolCard(
                icon = Icons.Default.Extension,
                label = stringResource(R.string.tool_integrations),
                phaseTag = "Phase 8",
                onClick = { onToolClick("integrations") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickToolCard(
    icon: ImageVector,
    label: String,
    phaseTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EvaCard(
        modifier = modifier,
        backgroundColor = EvaTheme.extendedColors.surfaceCard,
        contentPadding = EvaTheme.spacing.medium,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EvaTheme.extendedColors.secondaryCyan,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = phaseTag,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EvaTheme.extendedColors.primaryPurple,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .background(
                            color = EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EvaTheme.extendedColors.textPrimary,
                ),
            )
        }
    }
}
