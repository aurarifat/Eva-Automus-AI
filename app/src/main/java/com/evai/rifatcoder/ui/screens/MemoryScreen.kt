package com.evai.rifatcoder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.R
import com.evai.rifatcoder.data.database.entity.MemoryEntity
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaEmptyState
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.components.EvaTopBar
import com.evai.rifatcoder.ui.memory.MemoryViewModel
import com.evai.rifatcoder.ui.theme.EvaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MEMORY_CATEGORIES = listOf(
    "Preference", "Tool", "Project", "Goal", "Skill", "Work", "Personal", "General"
)

@Composable
fun MemoryScreen(
    modifier: Modifier = Modifier,
    viewModel: MemoryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessages()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_memory"),
        containerColor = EvaTheme.extendedColors.backgroundMain,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = EvaTheme.extendedColors.primaryPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("memory_add_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.memory_add_button_label),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Header Top Bar with Clear All action
            EvaTopBar(
                title = stringResource(R.string.title_memory),
                subtitle = stringResource(R.string.subtitle_memory),
                actions = {
                    if (uiState.savedCount > 0) {
                        IconButton(
                            onClick = { viewModel.openClearAllDialog() },
                            modifier = Modifier.testTag("memory_clear_all_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.memory_clear_all_action),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = EvaTheme.spacing.standard),
                verticalArrangement = Arrangement.spacedBy(EvaTheme.spacing.standard),
                contentPadding = PaddingValues(bottom = 88.dp, top = EvaTheme.spacing.small),
            ) {
                // Section 1: Overview & Master Toggle Card
                item(key = "overview_card") {
                    MemoryOverviewCard(
                        isEnabled = uiState.isMemoryEnabled,
                        savedCount = uiState.savedCount,
                        onToggle = { viewModel.toggleMemoryEnabled(it) },
                    )
                }

                // Section 2: Review Suggestions (if any pending)
                if (uiState.suggestedMemories.isNotEmpty()) {
                    item(key = "suggestions_header") {
                        Column {
                            EvaSectionHeader(
                                title = stringResource(R.string.memory_suggestions_header),
                            )
                            Text(
                                text = stringResource(R.string.memory_suggestions_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }

                    items(
                        items = uiState.suggestedMemories,
                        key = { "suggestion_${it.id}" },
                    ) { suggestion ->
                        SuggestionReviewCard(
                            memory = suggestion,
                            onConfirm = { viewModel.confirmSuggestion(suggestion.id) },
                            onEdit = { viewModel.openEditDialog(suggestion) },
                            onDismiss = { viewModel.dismissSuggestion(suggestion.id) },
                        )
                    }
                }

                // Section 3: Search & Category Filter Row
                item(key = "search_and_filters") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EvaSectionHeader(title = "Stored Facts")

                        // Search Bar
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_search_field"),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.memory_search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = EvaTheme.extendedColors.secondaryCyan,
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = EvaTheme.extendedColors.surfaceCard,
                                unfocusedContainerColor = EvaTheme.extendedColors.surfaceCard,
                                focusedBorderColor = EvaTheme.extendedColors.secondaryCyan,
                                unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                        )

                        // Category Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CategoryFilterChip(
                                label = "All",
                                isSelected = uiState.selectedCategory == null,
                                onClick = { viewModel.onCategorySelected(null) },
                                testTag = "category_chip_all",
                            )

                            MEMORY_CATEGORIES.forEach { cat ->
                                CategoryFilterChip(
                                    label = cat,
                                    isSelected = uiState.selectedCategory == cat,
                                    onClick = { viewModel.onCategorySelected(cat) },
                                    testTag = "category_chip_$cat",
                                )
                            }
                        }
                    }
                }

                // Section 4: Confirmed Memory List or Empty State
                if (uiState.confirmedMemories.isEmpty()) {
                    item(key = "empty_state") {
                        if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != null) {
                            EvaEmptyState(
                                icon = Icons.Default.Search,
                                title = stringResource(R.string.memory_search_empty_title),
                                description = stringResource(R.string.memory_search_empty_desc),
                                statusLevel = EvaStatusLevel.Available,
                                statusLabel = "0 Results",
                                testTag = "memory_search_empty_state",
                            )
                        } else {
                            EvaEmptyState(
                                icon = Icons.Default.Psychology,
                                title = stringResource(R.string.memory_empty_title),
                                description = stringResource(R.string.memory_empty_desc),
                                statusLevel = if (uiState.isMemoryEnabled) EvaStatusLevel.Available else EvaStatusLevel.Disabled,
                                statusLabel = if (uiState.isMemoryEnabled) "Memory Ready" else "Memory Paused",
                                testTag = "memory_empty_state",
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.confirmedMemories,
                        key = { it.id },
                    ) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onEdit = { viewModel.openEditDialog(memory) },
                            onDelete = { viewModel.openDeleteDialog(memory) },
                        )
                    }
                }

                // Privacy footer notice
                item(key = "privacy_footer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.memory_privacy_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // Dialog: Add Memory
    if (uiState.isAddDialogOpen) {
        MemoryEditorDialog(
            title = stringResource(R.string.memory_add_dialog_title),
            initialContent = "",
            initialCategory = uiState.selectedCategory ?: "Preference",
            initialImportance = 1,
            onDismiss = { viewModel.closeAddDialog() },
            onConfirm = { content, category, importance ->
                viewModel.saveMemory(content, category, importance)
            },
        )
    }

    // Dialog: Edit Memory
    uiState.editingMemory?.let { memoryToEdit ->
        MemoryEditorDialog(
            title = stringResource(R.string.memory_edit_dialog_title),
            initialContent = memoryToEdit.value,
            initialCategory = memoryToEdit.category,
            initialImportance = memoryToEdit.importance,
            onDismiss = { viewModel.closeEditDialog() },
            onConfirm = { content, category, importance ->
                viewModel.updateMemory(memoryToEdit.id, content, category, importance)
            },
        )
    }

    // Dialog: Delete Single Memory Confirmation
    uiState.deletingMemory?.let { memoryToDelete ->
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteDialog() },
            containerColor = EvaTheme.extendedColors.surfaceCard,
            title = {
                Text(
                    text = stringResource(R.string.memory_delete_confirm_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.memory_delete_confirm_desc) + "\n\n\"${memoryToDelete.value}\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteMemory(memoryToDelete.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvaTheme.extendedColors.error,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.testTag("memory_delete_confirm_button"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDeleteDialog() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Dialog: Clear All Memories Confirmation
    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearAllDialog() },
            containerColor = EvaTheme.extendedColors.surfaceCard,
            title = {
                Text(
                    text = stringResource(R.string.memory_clear_all_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.memory_clear_all_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAllMemories() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvaTheme.extendedColors.error,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.testTag("memory_clear_all_confirm_button"),
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeClearAllDialog() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun MemoryOverviewCard(
    isEnabled: Boolean,
    savedCount: Int,
    onToggle: (Boolean) -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_overview_card"),
    ) {
        Column(
            modifier = Modifier.padding(EvaTheme.spacing.standard),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.primaryPurple,
                        modifier = Modifier.size(26.dp),
                    )
                    Column {
                        Text(
                            text = if (isEnabled) {
                                stringResource(R.string.memory_status_active)
                            } else {
                                stringResource(R.string.memory_status_paused)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Text(
                            text = "$savedCount facts remembered",
                            style = MaterialTheme.typography.labelMedium,
                            color = EvaTheme.extendedColors.secondaryCyan,
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EvaTheme.extendedColors.primaryPurple,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = EvaTheme.extendedColors.borderSubtle,
                    ),
                    modifier = Modifier.testTag("memory_toggle_switch"),
                )
            }

            Text(
                text = if (isEnabled) {
                    stringResource(R.string.memory_status_desc_active)
                } else {
                    stringResource(R.string.memory_status_desc_paused)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Educational explanation of context architecture
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = EvaTheme.extendedColors.backgroundMain.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = EvaTheme.extendedColors.borderSubtle,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EvaTheme.extendedColors.secondaryCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "How EVA AI Handles Context",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                }

                Text(
                    text = "• Personal Memories: Verified facts & preferences used to personalize responses across all chats.\n• Chat History: Stored conversation archives for your review. Past sessions are never dumped into context.\n• Incognito Mode: When enabled, chats are strictly ephemeral and never stored in history or extracted as memories.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = EvaTheme.extendedColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SuggestionReviewCard(
    memory: MemoryEntity,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EvaTheme.extendedColors.primaryPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("suggestion_card_${memory.id}"),
    ) {
        Column(
            modifier = Modifier.padding(EvaTheme.spacing.standard),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Suggested",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Suggested • ${memory.category}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("memory_dismiss_suggestion_button_${memory.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = memory.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("memory_edit_suggestion_button_${memory.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = EvaTheme.extendedColors.secondaryCyan,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = EvaTheme.extendedColors.secondaryCyan, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.testTag("memory_confirm_suggestion_button_${memory.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvaTheme.extendedColors.primaryPurple,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Fact", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(memory.updatedAt) { dateFormatter.format(Date(memory.updatedAt)) }

    EvaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${memory.id}"),
    ) {
        Column(
            modifier = Modifier.padding(EvaTheme.spacing.standard),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = memory.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EvaTheme.extendedColors.secondaryCyan,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (memory.importance > 1) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "High Importance",
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("memory_edit_button_${memory.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit memory",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("memory_delete_button_${memory.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete memory",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Fact content
            Text(
                text = memory.value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )

            // Date / key info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Updated $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val bgColor = if (isSelected) {
        EvaTheme.extendedColors.primaryPurple
    } else {
        EvaTheme.extendedColors.surfaceCard
    }
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) {
        EvaTheme.extendedColors.primaryPurple
    } else {
        EvaTheme.extendedColors.borderSubtle
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    title: String,
    initialContent: String,
    initialCategory: String,
    initialImportance: Int,
    onDismiss: () -> Unit,
    onConfirm: (content: String, category: String, importance: Int) -> Unit,
) {
    var content by remember { mutableStateOf(initialContent) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var importance by remember { mutableIntStateOf(initialImportance) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EvaTheme.extendedColors.surfaceCard,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_dialog_input_content"),
                    label = { Text("Memory / Fact") },
                    placeholder = { Text("e.g. I prefer dark mode and clean code") },
                    minLines = 3,
                    maxLines = 5,
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = EvaTheme.extendedColors.error) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EvaTheme.extendedColors.primaryPurple,
                        unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                )

                // Category selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MEMORY_CATEGORIES.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.backgroundMain)
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White,
                                )
                            }
                        }
                    }
                }

                // Importance selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Importance",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "Normal", 2 to "High", 3 to "Critical").forEach { (lvl, lbl) ->
                            val isSelected = importance == lvl
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) EvaTheme.extendedColors.primaryPurple else EvaTheme.extendedColors.backgroundMain)
                                    .clickable { importance = lvl }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.trim().isBlank()) {
                        validationError = "Please enter the memory fact."
                    } else {
                        onConfirm(content.trim(), selectedCategory, importance)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvaTheme.extendedColors.primaryPurple,
                    contentColor = Color.White,
                ),
                modifier = Modifier.testTag("memory_dialog_save_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("memory_dialog_cancel_button"),
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
