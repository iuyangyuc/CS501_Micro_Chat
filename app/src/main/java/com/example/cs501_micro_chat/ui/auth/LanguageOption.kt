package com.example.cs501_micro_chat.ui.auth

import androidx.annotation.StringRes
import com.example.cs501_micro_chat.R

enum class LanguageOption(
    @StringRes val labelRes: Int,
    val languageTag: String
) {
    Chinese(R.string.login_language_chinese, "zh"),
    English(R.string.login_language_english, "en")
}
