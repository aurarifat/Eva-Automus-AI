package com.evai.rifatcoder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evai.rifatcoder.data.security.AiProviderType
import com.evai.rifatcoder.data.security.ProviderConnectionStatus
import com.evai.rifatcoder.ui.components.EvaCard
import com.evai.rifatcoder.ui.components.EvaPrimaryButton
import com.evai.rifatcoder.ui.components.EvaSecondaryButton
import androidx.activity.compose.BackHandler
import com.evai.rifatcoder.ui.components.EvaSectionHeader
import com.evai.rifatcoder.ui.components.EvaStatusIndicator
import com.evai.rifatcoder.ui.components.EvaStatusLevel
import com.evai.rifatcoder.ui.components.EvaTopBar
import com.evai.rifatcoder.ui.config.AiConfigViewModel
import com.evai.rifatcoder.ui.theme.EvaTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiConfigScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiConfigViewModel = viewModel(),
) {
    BackHandler(onBack = onNavigateBack)
    val uiState by viewModel.uiState.collectAsState()

    val currentStatusLevel = when (uiState.currentStatus) {
        ProviderConnectionStatus.CONNECTED -> EvaStatusLevel.Available
        ProviderConnectionStatus.NOT_CONFIGURED -> EvaStatusLevel.NotConfigured
        ProviderConnectionStatus.TESTING -> EvaStatusLevel.InProgress
        ProviderConnectionStatus.CONNECTION_FAILED -> EvaStatusLevel.Error
        ProviderConnectionStatus.CONFIGURATION_CHANGED -> EvaStatusLevel.InProgress
    }

    val currentStatusLabel = when (uiState.currentStatus) {
        ProviderConnectionStatus.CONNECTED -> "Connected"
        ProviderConnectionStatus.NOT_CONFIGURED -> "Not Configured"
        ProviderConnectionStatus.TESTING -> "Testing..."
        ProviderConnectionStatus.CONNECTION_FAILED -> "Connection Failed"
        ProviderConnectionStatus.CONFIGURATION_CHANGED -> "Configuration Changed (Retest Required)"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_ai_config")
            .imePadding(),
    ) {
        // TOP APP BAR
        EvaTopBar(
            title = "AI Connection",
            subtitle = "${uiState.selectedProvider.displayName} Configuration",
            onBackClick = onNavigateBack,
            actions = {
                EvaStatusIndicator(
                    status = currentStatusLevel,
                    customLabel = currentStatusLabel,
                    showBorder = false,
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EvaTheme.spacing.standard),
        ) {
            Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))

            // SECTION B: PROVIDER SELECTION CARD
            EvaSectionHeader(title = "Select AI Provider")
            ProviderSelectionTabs(
                selectedProvider = uiState.selectedProvider,
                onSelectProvider = { viewModel.selectProvider(it) },
            )

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

            // SECTION C: API KEY SECTION
            EvaCard(
                modifier = Modifier.fillMaxWidth().testTag("config_api_key_card"),
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
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = EvaTheme.extendedColors.primaryPurple,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(EvaTheme.spacing.small))
                            Text(
                                text = "API Key",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = EvaTheme.extendedColors.textPrimary,
                                ),
                            )
                        }

                        if (uiState.hasStoredKeyForSelectedProvider) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EvaTheme.extendedColors.success,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Key Stored Securely",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EvaTheme.extendedColors.success,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    OutlinedTextField(
                        value = uiState.currentApiKeyInput,
                        onValueChange = { viewModel.updateApiKey(it) },
                        placeholder = {
                            Text(
                                text = if (uiState.hasStoredKeyForSelectedProvider) "••••••••••••••••••••••••••••" else "Enter ${uiState.selectedProvider.displayName} API Key",
                                color = EvaTheme.extendedColors.textDisabled,
                            )
                        },
                        visualTransformation = if (uiState.isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleKeyVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (uiState.isKeyVisible) "Hide API Key" else "Show API Key",
                                    tint = EvaTheme.extendedColors.textSecondary,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_api_key"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EvaTheme.extendedColors.primaryPurple,
                            unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                            focusedTextColor = EvaTheme.extendedColors.textPrimary,
                            unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Keys are encrypted on-device via Android Keystore. They are never sent to third parties or logged.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontSize = 11.sp,
                        ),
                    )

                    if (uiState.hasStoredKeyForSelectedProvider) {
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))
                        EvaSecondaryButton(
                            text = "Remove Stored Key",
                            onClick = { viewModel.requestRemoveKey() },
                            leadingIcon = Icons.Default.Delete,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "btn_remove_key",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

            // SECTION D: MODEL SELECTION SECTION
            EvaCard(
                modifier = Modifier.fillMaxWidth().testTag("config_model_card"),
                backgroundColor = EvaTheme.extendedColors.surfaceCard,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Model Identifier",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EvaTheme.extendedColors.textPrimary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    OutlinedTextField(
                        value = uiState.currentModelIdInput,
                        onValueChange = { viewModel.updateModelId(it) },
                        label = { Text("Model ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_model_id"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EvaTheme.extendedColors.secondaryCyan,
                            unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                            focusedTextColor = EvaTheme.extendedColors.textPrimary,
                            unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.small))

                    Text(
                        text = "Recommended Model Presets:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EvaTheme.extendedColors.textSecondary,
                            fontWeight = FontWeight.Medium,
                        ),
                    )

                    Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(EvaTheme.spacing.extraSmall),
                    ) {
                        uiState.knownModels.forEach { model ->
                            val isSelected = uiState.currentModelIdInput == model
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) EvaTheme.extendedColors.secondaryCyan.copy(alpha = 0.2f) else EvaTheme.extendedColors.surfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.borderSubtle,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable { viewModel.updateModelId(model) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) EvaTheme.extendedColors.secondaryCyan else EvaTheme.extendedColors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                )
                            }
                        }
                    }

                    // Optional Base URL for OmniRoute
                    if (uiState.selectedProvider == AiProviderType.OMNIROUTE) {
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.standard))

                        Text(
                            text = "OmniRoute Base Endpoint",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EvaTheme.extendedColors.textPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(EvaTheme.spacing.extraSmall))
                        OutlinedTextField(
                            value = uiState.omniRouteBaseUrlInput,
                            onValueChange = { viewModel.updateOmniRouteBaseUrl(it) },
                            label = { Text("Base URL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_omniroute_base_url"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EvaTheme.extendedColors.secondaryCyan,
                                unfocusedBorderColor = EvaTheme.extendedColors.borderSubtle,
                                focusedTextColor = EvaTheme.extendedColors.textPrimary,
                                unfocusedTextColor = EvaTheme.extendedColors.textPrimary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))

            // SECTION E & F: ACTIONS (TEST CONNECTION & SAVE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EvaTheme.spacing.small),
            ) {
                EvaSecondaryButton(
                    text = if (uiState.isTesting) "Testing..." else "Test Connection",
                    onClick = { viewModel.testConnection() },
                    leadingIcon = Icons.Default.NetworkCheck,
                    enabled = !uiState.isTesting,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_test_connection",
                )

                EvaPrimaryButton(
                    text = "Save Configuration",
                    onClick = { viewModel.saveConfiguration() },
                    leadingIcon = Icons.Default.Save,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_save_config",
                )
            }

            // Test Result / Status Message Area
            if (uiState.testResultMessage != null) {
                Spacer(modifier = Modifier.height(EvaTheme.spacing.medium))
                val isSuccess = uiState.testSuccess == true
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSuccess) EvaTheme.extendedColors.success.copy(alpha = 0.12f) else EvaTheme.extendedColors.error.copy(alpha = 0.12f),
                            shape = EvaTheme.shapes.medium,
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSuccess) EvaTheme.extendedColors.success.copy(alpha = 0.4f) else EvaTheme.extendedColors.error.copy(alpha = 0.4f),
                            shape = EvaTheme.shapes.medium,
                        )
                        .padding(EvaTheme.spacing.medium)
                        .testTag("test_result_message_card"),
                ) {
                    Text(
                        text = uiState.testResultMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSuccess) EvaTheme.extendedColors.success else EvaTheme.extendedColors.error,
                            lineHeight = 18.sp,
                        ),
                    )
                }
            }

            if (uiState.userFeedbackMessage != null) {
                Spacer(modifier = Modifier.height(EvaTheme.spacing.small))
                Text(
                    text = uiState.userFeedbackMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EvaTheme.extendedColors.success,
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(EvaTheme.spacing.large))
        }
    }

    // Confirmation dialog for removing key
    if (uiState.showRemoveKeyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRemoveKeyDialog() },
            title = {
                Text(
                    text = "Remove ${uiState.selectedProvider.displayName} Key?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EvaTheme.extendedColors.textPrimary,
                    )
                )
            },
            text = {
                Text(
                    text = "This will erase the encrypted credential from local storage. You will need to re-enter it to use ${uiState.selectedProvider.displayName}.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRemoveKey() }) {
                    Text(
                        text = "Remove",
                        color = EvaTheme.extendedColors.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRemoveKeyDialog() }) {
                    Text(
                        text = "Cancel",
                        color = EvaTheme.extendedColors.textSecondary,
                    )
                }
            },
            containerColor = EvaTheme.extendedColors.surfaceElevated,
            shape = EvaTheme.shapes.large,
        )
    }
}

@Composable
private fun ProviderSelectionTabs(
    selectedProvider: AiProviderType,
    onSelectProvider: (AiProviderType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = EvaTheme.extendedColors.surfaceCard,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = EvaTheme.extendedColors.borderSubtle,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(4.dp),
    ) {
        AiProviderType.entries.forEach { provider ->
            val isSelected = selectedProvider == provider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) EvaTheme.extendedColors.primaryPurple else Color.Transparent
                    )
                    .clickable { onSelectProvider(provider) }
                    .padding(vertical = 10.dp)
                    .testTag("tab_provider_${provider.name.lowercase()}"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isSelected) Color.White else EvaTheme.extendedColors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
