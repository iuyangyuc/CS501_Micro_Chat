package com.example.cs501_micro_chat.ui.auth

enum class LanguageOption(
    val displayName: String,
    val languageTag: String
) {
    English("English", "en"),
    Chinese("中文（简体）", "zh"),
    TraditionalChinese("中文（繁體）", "zh-TW"),
    Spanish("Español", "es"),
    French("Français", "fr"),
    Russian("Русский", "ru");

    companion object {
        fun fromName(name: String): LanguageOption {
            return entries.firstOrNull { it.name == name } ?: English
        }
    }
}
