package com.example.cs501_micro_chat.ui.auth

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun LanguageSwitcher(
    label: String,
    selectedLanguage: LanguageOption,
    onLanguageSelected: (LanguageOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val labelContext = remember(selectedLanguage, context) { context.localized(selectedLanguage) }
    val selectedLabel = labelContext.getString(selectedLanguage.labelRes)

    Box(modifier = modifier) {
        Text(
            text = "$label · $selectedLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LanguageOption.values().forEach { option ->
                val optionLabel = context.localized(option).getString(option.labelRes)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        expanded = false
                        if (option != selectedLanguage) {
                            onLanguageSelected(option)
                        }
                    }
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
fun Context.localized(language: LanguageOption): Context {
    val locale = Locale.forLanguageTag(language.languageTag)
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    return createConfigurationContext(configuration)
}
