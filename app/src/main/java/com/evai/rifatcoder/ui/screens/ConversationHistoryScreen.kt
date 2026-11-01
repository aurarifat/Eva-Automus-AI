package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.R
import com.evai.rifatcoder.data.database.entity.ConversationEntity
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.history.ConversationHistoryViewModel
import com.evai.rifatcoder.ui.theme.EvaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: ConversationHistoryViewModel = viewModel(),
    onSelectConversation: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_conversation_history")
            .background(EvaTheme.extendedColors.backgroundMain)
            .statusBarsPadding(),
    ) {
        // TOP APP BAR
        HistoryTopBar(
            hasConversations = uiState.conversations.isNotEmpty(),
            onNavigateBack = onNavigateBack,
            onClearAll = { viewModel.onRequestClearAll() },
        )

        // SEARCH INPUT BAR
        HistorySearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onClearQuery = { viewModel.onClearSearch() },
        )

        // CONVERSATIONS CONTENT
        if (uiState.conversations.isEmpty()) {
            HistoryEmptyState(
                isSearch = uiState.searchQuery.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("history_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = uiState.conversations,
                    key = { it.id },
                ) { conversation ->
                    ConversationItemCard(
                        conversation = conversation,
                        onClick = { onSelectConversation(conversation.id) },
                        onRename = { viewModel.onStartRename(conversation) },
                        onDelete = { viewModel.onStartDelete(conversation) },
                    )
                }
            }
        }
    }

    // RENAME DIALOG
    if (uiState.renameTarget != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissRename() },
            title = {
                Text(
                    text = stringResource(R.string.history_rename_dialog_title),
                    color = EvaTheme.extendedColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.renameInput,
                        onValueChange = { viewModel.onRenameInputChange(it) },
                        label = { Text(stringResource(R.string.history_rename_dialog_hint)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EvaTheme.extendedColors.primaryPurple,
                            unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                            focusedTextColor = EvaTheme.extendedColors.textPrimary,
                            unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_rename_input"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmRename() },
                    modifier = Modifier.testTag("dialog_rename_confirm"),
                ) {
                    Text(
                        stringResource(R.string.history_save),
                        color = EvaTheme.extendedColors.secondaryCyan,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissRename() },
                    modifier = Modifier.testTag("dialog_rename_cancel"),
                ) {
                    Text(
                        stringResource(R.string.history_cancel),
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceCard,
            shape = RoundedCornerShape(16.dp),
        )
    }

    // DELETE DIALOG
    if (uiState.deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDelete() },
            title = {
                Text(
                    text = stringResource(R.string.history_delete_dialog_title),
                    color = EvaTheme.extendedColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_delete_dialog_message),
                    color = EvaTheme.extendedColors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmDelete() },
                    modifier = Modifier.testTag("dialog_delete_confirm"),
                ) {
                    Text(
                        stringResource(R.string.history_delete),
                        color = EvaTheme.extendedColors.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissDelete() },
                    modifier = Modifier.testTag("dialog_delete_cancel"),
                ) {
                    Text(
                        stringResource(R.string.history_cancel),
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceCard,
            shape = RoundedCornerShape(16.dp),
        )
    }

    // CLEAR ALL DIALOG
    if (uiState.showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissClearAll() },
            title = {
                Text(
                    text = stringResource(R.string.history_clear_all_dialog_title),
                    color = EvaTheme.extendedColors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_clear_all_dialog_message),
                    color = EvaTheme.extendedColors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onConfirmClearAll() },
                    modifier = Modifier.testTag("dialog_clear_all_confirm"),
                ) {
                    Text(
                        stringResource(R.string.history_clear_all),
                        color = EvaTheme.extendedColors.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onDismissClearAll() },
                    modifier = Modifier.testTag("dialog_clear_all_cancel"),
                ) {
                    Text(
                        stringResource(R.string.history_cancel),
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceCard,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun HistoryTopBar(
    hasConversations: Boolean,
    onNavigateBack: () -> Unit,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("history_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_button),
                    tint = EvaTheme.extendedColors.textPrimary,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.title_conversation_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EvaTheme.extendedColors.textPrimary,
            )
        }

        if (hasConversations) {
            IconButton(
                onClick = onClearAll,
                modifier = Modifier.testTag("history_clear_all_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = stringResource(R.string.history_cd_clear_all),
                    tint = EvaTheme.extendedColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.history_search_hint),
                color = EvaTheme.extendedColors.textSecondary,
                fontSize = 14.sp,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = EvaTheme.extendedColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.testTag("history_search_clear_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.history_cd_search_clear),
                        tint = EvaTheme.extendedColors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = EvaTheme.extendedColors.surfaceCard,
            unfocusedContainerColor = EvaTheme.extendedColors.surfaceCard,
            focusedBorderColor = EvaTheme.extendedColors.primaryPurple,
            unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
            focusedTextColor = EvaTheme.extendedColors.textPrimary,
            unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("history_search_input"),
    )
}

@Composable
private fun ConversationItemCard(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_${conversation.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EvaTheme.extendedColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("history_item_title_${conversation.id}"),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("history_rename_${conversation.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.history_rename),
                            tint = EvaTheme.extendedColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("history_delete_${conversation.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.history_delete),
                            tint = EvaTheme.extendedColors.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (!conversation.lastMessagePreview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.lastMessagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = EvaTheme.extendedColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatConversationDate(conversation.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(
    isSearch: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.testTag("history_empty_state"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearch) "No matching conversations" else stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = EvaTheme.extendedColors.textPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearch) "Try searching for a different keyword" else stringResource(R.string.history_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = EvaTheme.extendedColors.textSecondary,
            )
        }
    }
}

private fun formatConversationDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val oneDay = 24 * 60 * 60 * 1000L

    return when {
        diff < oneDay -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Today at ${format.format(date)}"
        }
        diff < 2 * oneDay -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday at ${format.format(date)}"
        }
        else -> {
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            format.format(date)
        }
    }
}
