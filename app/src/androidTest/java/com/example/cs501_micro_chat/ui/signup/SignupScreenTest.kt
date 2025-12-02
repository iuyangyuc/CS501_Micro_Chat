package com.example.cs501_micro_chat.ui.signup

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.theme.CS501_Micro_ChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SignupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun errorMessage_isDisplayedAndDismissible() {
        val errorText = composeTestRule.activity.getString(R.string.error_password_mismatch)
        var dismissed = false

        composeTestRule.setContent {
            CS501_Micro_ChatTheme {
                SignupScreen(
                    state = SignupUiState(
                        email = "user@example.com",
                        password = "Password123",
                        confirmPassword = "Password123",
                        errorMessage = errorText
                    ),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onSignUpClick = {},
                    onGoogleSignUpClick = {},
                    onNavigateToLogin = {},
                    onLanguageSelected = {},
                    onDismissError = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText(errorText).assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun emailSignup_showsLoadingIndicator() {
        val loadingText = composeTestRule.activity.getString(R.string.signup_loading)
        val googleText = composeTestRule.activity.getString(R.string.signup_google_button)
        val signUpText = composeTestRule.activity.getString(R.string.signup_primary_button)

        composeTestRule.setContent {
            CS501_Micro_ChatTheme {
                SignupScreen(
                    state = SignupUiState(
                        email = "user@example.com",
                        password = "Password123",
                        confirmPassword = "Password123",
                        loadingProvider = AuthProvider.Email
                    ),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onSignUpClick = {},
                    onGoogleSignUpClick = {},
                    onNavigateToLogin = {},
                    onLanguageSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText(loadingText).assertIsDisplayed()
        composeTestRule.onNodeWithText(googleText).assertIsDisplayed()
        composeTestRule.onNodeWithText(signUpText).assertIsNotEnabled()
    }

    @Test
    fun languageSwitcher_showsSelectedLanguage() {
        val languageLabel = composeTestRule.activity.getString(R.string.login_language_switch)
        val expected = "$languageLabel · ${LanguageOption.Spanish.displayName}"

        composeTestRule.setContent {
            CS501_Micro_ChatTheme {
                SignupScreen(
                    state = SignupUiState(selectedLanguage = LanguageOption.Spanish),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onSignUpClick = {},
                    onGoogleSignUpClick = {},
                    onNavigateToLogin = {},
                    onLanguageSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun signUpButton_usesLatestFieldValues() {
        val emailLabel = composeTestRule.activity.getString(R.string.signup_email_label)
        val passwordLabel = composeTestRule.activity.getString(R.string.signup_password_label)
        val confirmLabel = composeTestRule.activity.getString(R.string.signup_confirm_password_label)
        val signUpText = composeTestRule.activity.getString(R.string.signup_primary_button)
        var capturedEmail = ""
        var capturedPassword = ""
        var capturedConfirm = ""
        var signUpClicks = 0

        composeTestRule.setContent {
            var uiState by remember { mutableStateOf(SignupUiState()) }

            CS501_Micro_ChatTheme {
                SignupScreen(
                    state = uiState,
                    onEmailChange = { value ->
                        capturedEmail = value
                        uiState = uiState.copy(email = value)
                    },
                    onPasswordChange = { value ->
                        capturedPassword = value
                        uiState = uiState.copy(password = value)
                    },
                    onConfirmPasswordChange = { value ->
                        capturedConfirm = value
                        uiState = uiState.copy(confirmPassword = value)
                    },
                    onSignUpClick = { signUpClicks++ },
                    onGoogleSignUpClick = {},
                    onNavigateToLogin = {},
                    onLanguageSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText(emailLabel, useUnmergedTree = true)
            .performTextInput("new_user@example.com")
        composeTestRule.onNodeWithText(passwordLabel, useUnmergedTree = true)
            .performTextInput("Password123")
        composeTestRule.onNodeWithText(confirmLabel, useUnmergedTree = true)
            .performTextInput("Password123")

        composeTestRule.onNodeWithText(signUpText).performClick()

        composeTestRule.runOnIdle {
            assertEquals("new_user@example.com", capturedEmail)
            assertEquals("Password123", capturedPassword)
            assertEquals("Password123", capturedConfirm)
            assertEquals(1, signUpClicks)
        }

        composeTestRule.onNodeWithText(emailLabel).assertIsDisplayed()
    }
}
