package com.example.cs501_micro_chat.ui.theme

import androidx.annotation.StringRes
import com.example.cs501_micro_chat.R

enum class ThemeOption(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_option_system),
    LIGHT(R.string.theme_option_light),
    DARK(R.string.theme_option_dark);
}
