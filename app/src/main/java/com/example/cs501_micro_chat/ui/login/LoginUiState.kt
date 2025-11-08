/**
 * LoginUiState.kt
 *
 * 登录界面状态数据类 - 定义登录界面的所有状态
 * Login UI State Data Class - Defines all states for the login screen
 *
 * 主要内容 / Main Contents:
 * - LoginUiState: 登录界面状态数据类 / Login screen state data class
 *   - 用户输入（邮箱、密码）/ User input (email, password)
 *   - 加载状态和错误信息 / Loading state and error messages
 *   - 用户协议勾选状态 / Agreement checkbox state
 *   - 语言选择和活动的登录方式 / Language selection and active provider
 *   - 计算属性：表单验证状态 / Computed properties: form validation state
 *
 * - LanguageOption: 语言选项枚举 / Language option enum
 *   - 中文和英文选项 / Chinese and English options
 *   - 语言标签和资源 ID / Language tags and resource IDs
 *
 * 设计模式 / Design Pattern:
 * - 不可变数据类 (Immutable Data Class)
 * - 单一数据源 (Single Source of Truth)
 *
 * @author CS501 Team
 * @date 2025-11-02
 */
package com.example.cs501_micro_chat.ui.login

import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loadingProvider: AuthProvider? = null,
    val errorMessage: String? = null,
    val agreementChecked: Boolean = false,
    val selectedLanguage: LanguageOption = LanguageOption.Chinese,
    val activeProvider: AuthProvider? = null
) {
    val isLoading: Boolean
        get() = loadingProvider != null

    val isEmailValid: Boolean
        get() = email.isNotBlank()

    val isPasswordValid: Boolean
        get() = password.isNotBlank()

    val isEmailFormReady: Boolean
        get() = activeProvider == AuthProvider.Email &&
            agreementChecked && isEmailValid && isPasswordValid && !isLoading
}
