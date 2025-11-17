package com.example.cs501_micro_chat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.auth.LanguageOption

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LanguageSettingsContent(
        state = state,
        onInterfaceLanguageSelected = viewModel::selectInterfaceLanguage,
        onTranslationLanguageSelected = viewModel::selectTranslationLanguage,
        onAutoTranslateToggle = viewModel::toggleAutoTranslate
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSettingsContent(
    state: SettingsUiState,
    onInterfaceLanguageSelected: (LanguageOption) -> Unit,
    onTranslationLanguageSelected: (LanguageOption) -> Unit,
    onAutoTranslateToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.language_settings_interface_label),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.language_settings_interface_description),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            LanguageChipSection(
                selected = state.interfaceLanguage,
                onSelect = onInterfaceLanguageSelected
            )
        }

        item {
            Text(
                text = stringResource(R.string.language_settings_translation_label),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.language_settings_translation_description),
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            LanguageChipSection(
                selected = state.translationLanguage,
                onSelect = onTranslationLanguageSelected
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.translation_auto_toggle_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                   Text(
                        text = stringResource(R.string.translation_auto_toggle_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText
                    )
                }
                Switch(
                    checked = state.autoTranslateEnabled,
                    onCheckedChange = onAutoTranslateToggle
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageChipSection(
    selected: LanguageOption,
    onSelect: (LanguageOption) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LanguageOption.entries.forEach { option ->
                LanguageChip(
                    option = option,
                    isSelected = option == selected,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    option: LanguageOption,
    isSelected: Boolean,
    onSelect: (LanguageOption) -> Unit
) {
    val border = FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = isSelected,
        borderColor = MaterialTheme.colorScheme.outline,
        selectedBorderColor = MaterialTheme.colorScheme.primary
    )
    FilterChip(
        selected = isSelected,
        onClick = { onSelect(option) },
        label = {
            Text(
                text = option.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        border = border,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null
    )
}
