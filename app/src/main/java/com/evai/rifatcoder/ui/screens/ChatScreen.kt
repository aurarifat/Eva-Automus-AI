package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.R
import com.evai.rifatcoder.domain.chat.ChatMessage
import com.evai.rifatcoder.domain.chat.ChatRole
import com.evai.rifatcoder.domain.chat.MessageStatus
import com.evai.rifatcoder.ui.chat.ChatUiState
import com.evai.rifatcoder.ui.chat.ChatViewModel
import com.evai.rifatcoder.ui.components.EvaAnimatedOrb
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaIconButton
import com.evai.rifatcoder.ui.components.EvaLoadingIndicator
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.theme.EvaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    onNavigateToConfig: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Refresh model state when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshEngineState()
    }

    // Auto-scroll to newest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_chat")
            .imePadding(),
    ) {
        // SECTION A — TOP APP BAR
        ChatTopBar(
            subtitle = uiState.selectedModel,
            isIncognito = uiState.isIncognito,
            onToggleIncognito = { viewModel.toggleIncognito(it) },
            onNewChat = { viewModel.startNewChat() },
            onNavigateToHistory = onNavigateToHistory,
            onClearChat = { viewModel.requestClearConversation() },
            onShowInfo = { viewModel.showInfoDialog() },
            onConfigureAi = onNavigateToConfig,
        )

        if (uiState.isIncognito) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EvaTheme.extendedColors.secondaryCyan,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Incognito Mode: Chats will not be saved to history or memory",
                    style = MaterialTheme.typography.labelSmall,
                    color = EvaTheme.extendedColors.secondaryCyan,
                )
            }
        }

        // SECTION B & C — MESSAGE HISTORY AREA / EMPTY STATE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (uiState.isEmpty) {
                ChatEmptyState(
                    suggestedPrompts = uiState.suggestedPrompts,
                    onSelectPrompt = { prompt ->
                        viewModel.sendMessage(promptOverride = prompt)
                    },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_message_list"),
                    contentPadding = PaddingValues(
                        horizontal = EvaTheme.spacing.standard,
                        vertical = EvaTheme.spacing.medium,
                    ),
                    verticalArrangement = Arrangement.spacedBy(EvaTheme.spacing.medium),
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id },
                    ) { message ->
                        ChatMessageItem(
                            message = message,
                            onRetry = { viewModel.retryMessage(message.id) },
                            onConfigureAi = onNavigateToConfig,
                        )
                    }

                    if (uiState.isSending) {
                        item(key = "generating_indicator") {
                            GeneratingIndicatorItem()
                        }
                    }
                }
            }
        }

        // SECTION D & E — MESSAGE COMPOSER
        ChatMessageComposer(
            text = uiState.composerText,
            canSend = uiState.canSend,
            onTextChanged = { viewModel.updateComposerText(it) },
            onSend = { viewModel.sendMessage() },
            onMicClick = { viewModel.showVoiceNotice() },
        )
    }

    // Confirmation dialog for clearing conversation
    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearDialog() },
            title = {
                Text(
                    text = stringResource(R.string.chat_clear_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.chat_clear_confirm_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClearConversation() }) {
                    Text(
                        text = stringResource(R.string.chat_clear_confirm_action),
                        color = EvaTheme.extendedColors.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearDialog() }) {
                    Text(
                        text = stringResource(R.string.chat_cancel_action),
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            shape = EvaTheme.shapes.large,
        )
    }

    // Info dialog
    if (uiState.showInfoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissInfoDialog() },
            title = {
                Text(
                    text = stringResource(R.string.chat_info_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.chat_info_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissInfoDialog() }) {
                    Text(
                        text = "OK",
                        color = EvaTheme.extendedColors.primaryPurple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            shape = EvaTheme.shapes.large,
        )
    }

    // Voice input reserved notice dialog
    if (uiState.showVoiceNoticeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissVoiceNotice() },
            title = {
                Text(
                    text = stringResource(R.string.chat_voice_notice_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.chat_voice_notice_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissVoiceNotice() }) {
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
}

// ==========================================
// SECTION A: TOP APP BAR
// ==========================================
@Composable
private fun ChatTopBar(
    subtitle: String,
    isIncognito: Boolean = false,
    onToggleIncognito: (Boolean) -> Unit = {},
    onNewChat: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onClearChat: () -> Unit,
    onShowInfo: () -> Unit,
    onConfigureAi: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onConfigureAi),
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
                    ),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.secondaryCyan,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            EvaIconButton(
                icon = Icons.Default.Security,
                contentDescription = if (isIncognito) "Incognito Mode Active" else "Enable Incognito Mode",
                onClick = { onToggleIncognito(!isIncognito) },
                tint = if (isIncognito) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textSecondary,
                containerColor = if (isIncognito) EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.2f) else EvaTheme.extendedColors.surfaceCard,
                testTag = "chat_top_bar_incognito",
            )

            Spacer(modifier = Modifier.width(EvaTheme.spacing.extraSmall))

            EvaIconButton(
                icon = Icons.Default.Schedule,
                contentDescription = stringResource(R.string.chat_cd_history),
                onClick = onNavigateToHistory,
                tint = EvaTheme.extendedColors.secondaryCyan,
                containerColor = EvaTheme.extendedColors.surfaceCard,
                testTag = "chat_top_bar_history",
            )

            Spacer(modifier = Modifier.width(EvaTheme.spacing.extraSmall))

            EvaIconButton(
                icon = Icons.Default.AddComment,
                contentDescription = stringResource(R.string.chat_cd_new_chat),
                onClick = onNewChat,
                tint = EvaTheme.extendedColors.primaryPurple,
                containerColor = EvaTheme.extendedColors.surfaceCard,
                testTag = "chat_top_bar_new_chat",
            )

            Spacer(modifier = Modifier.width(EvaTheme.spacing.extraSmall))

            Box {
                EvaIconButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.chat_cd_more),
                    onClick = { menuExpanded = true },
                    tint = EvaTheme.extendedColors.textSecondary,
                    containerColor = EvaTheme.extendedColors.surfaceCard,
                    testTag = "chat_top_bar_more_menu",
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(EvaTheme.extendedColors.surfaceElevated),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isIncognito) "Disable Incognito Mode" else "Enable Incognito Mode",
                                color = EvaTheme.extendedColors.textPrimary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isIncognito) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textSecondary,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleIncognito(!isIncognito)
                        },
                        modifier = Modifier.testTag("chat_menu_incognito"),
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "AI Connection Settings",
                                color = EvaTheme.extendedColors.textPrimary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = EvaTheme.extendedColors.primaryPurple,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onConfigureAi()
                        },
                        modifier = Modifier.testTag("chat_menu_configure_ai"),
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.chat_clear_conversation),
                                color = EvaTheme.extendedColors.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = EvaTheme.extendedColors.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onClearChat()
                        },
                        modifier = Modifier.testTag("chat_menu_clear"),
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.chat_info_title),
                                color = EvaTheme.extendedColors.textPrimary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = EvaTheme.extendedColors.secondaryCyan,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onShowInfo()
                        },
                        modifier = Modifier.testTag("chat_menu_info"),
                    )
                }
            }
        }
    }
}

// ==========================================
// SECTION C: EMPTY STATE
// ==========================================
@Composable
private fun ChatEmptyState(
    suggestedPrompts: List<String>,
    onSelectPrompt: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(EvaTheme.spacing.large)
            .testTag("chat_empty_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EvaAnimatedOrb(
            size = 130.dp,
            modifier = Modifier.padding(bottom = EvaTheme.spacing.medium),
        )

        Text(
            text = stringResource(R.string.chat_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = EvaTheme.extendedColors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

        Text(
            text = stringResource(R.string.chat_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = EvaTheme.extendedColors.textSecondary,
                textAlign = TextAlign.Center,
            ),
        )

        Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
        ) {
            suggestedPrompts.forEach { prompt ->
                EvaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = EvaTheme.extendedColors.surfaceCard,
                    contentPadding = EvaTheme.spacing.medium,
                    onClick = { onSelectPrompt(prompt) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = EvaTheme.extendedColors.textPrimary,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = EvaTheme.extendedColors.primaryPurple,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION B: MESSAGE ITEM
// ==========================================
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onRetry: () -> Unit,
    onConfigureAi: () -> Unit = {},
) {
    val isUser = message.role == ChatRole.User
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "chat_message_user" else "chat_message_assistant"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = EvaTheme.spacing.extraSmall),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(EvaTheme.extendedColors.primaryPurple, EvaTheme.extendedColors.secondaryCyan)
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EVA AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.secondaryCyan,
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (isUser) EvaTheme.extendedColors.surfaceElevated else EvaTheme.extendedColors.surfaceCard,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.35f) else EvaTheme.extendedColors.borderSubtle,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    ),
                )
                .padding(EvaTheme.spacing.medium),
        ) {
            Column {
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = EvaTheme.extendedColors.textPrimary,
                            lineHeight = 22.sp,
                        ),
                    )
                }

                if (message.status == MessageStatus.NotConfigured) {
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EvaStatusIndicator(
                            status = EvaStatusLevel.NotConfigured,
                            customLabel = "Provider Unconfigured",
                        )
                        TextButton(
                            onClick = onConfigureAi,
                            modifier = Modifier.testTag("chat_configure_provider_action"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = EvaTheme.extendedColors.primaryPurple,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EvaTheme.extendedColors.primaryPurple,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                } else if (message.status == MessageStatus.Failed) {
                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        EvaStatusIndicator(
                            status = EvaStatusLevel.Error,
                            customLabel = "Failed",
                        )
                        TextButton(onClick = onRetry) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = EvaTheme.extendedColors.primaryPurple,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.chat_retry),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EvaTheme.extendedColors.primaryPurple,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = EvaTheme.extendedColors.textDisabled,
                ),
            )
            if (isUser) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = when (message.status) {
                        MessageStatus.Pending -> Icons.Default.Schedule
                        else -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = EvaTheme.extendedColors.textDisabled,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun GeneratingIndicatorItem() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp),
    ) {
        EvaLoadingIndicator(
            size = 20.dp,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
        Text(
            text = "EVA AI is processing...",
            style = MaterialTheme.typography.bodySmall.copy(
                color = EvaTheme.extendedColors.secondaryCyan,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ==========================================
// SECTION D: MESSAGE COMPOSER
// ==========================================
@Composable
private fun ChatMessageComposer(
    text: String,
    canSend: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    val cdSend = stringResource(R.string.chat_cd_send)
    val cdMic = stringResource(R.string.chat_cd_mic)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EvaTheme.extendedColors.backgroundSecondary)
            .padding(
                horizontal = EvaTheme.spacing.standard,
                vertical = EvaTheme.spacing.small,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = EvaTheme.extendedColors.surfaceCard,
                    shape = RoundedCornerShape(24.dp),
                )
                .border(
                    width = 1.dp,
                    color = EvaTheme.extendedColors.borderSubtle,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = stringResource(R.string.chat_hint),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EvaTheme.extendedColors.textDisabled,
                        ),
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_composer_input"),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedTextColor = EvaTheme.extendedColors.textPrimary,
                    unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
                    cursorColor = EvaTheme.extendedColors.primaryPurple,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default,
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) onSend()
                    }
                ),
            )

            // Reserved voice microphone button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = cdMic }
                    .testTag("chat_composer_mic_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = cdMic,
                    tint = EvaTheme.extendedColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Send button
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) EvaTheme.extendedColors.primaryPurple else Color.Transparent
                    )
                    .semantics { contentDescription = cdSend }
                    .testTag("chat_composer_send_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = cdSend,
                    tint = if (canSend) Color.White else EvaTheme.extendedColors.textDisabled,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
