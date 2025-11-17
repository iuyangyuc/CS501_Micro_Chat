package com.example.cs501_micro_chat.ui.signup

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.auth.LanguageSwitcher
import com.example.cs501_micro_chat.ui.auth.localized
import com.example.cs501_micro_chat.ui.theme.CS501_Micro_ChatTheme

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    state: SignupUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignUpClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onLanguageSelected: (LanguageOption) -> Unit,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
) {
    val scrollState = rememberScrollState()
    val strings = rememberSignupStrings(state.selectedLanguage)
    val backgroundColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            SignupTopBar(
                backLabel = strings.backToLogin,
                languageLabel = strings.languageSwitchLabel,
                selectedLanguage = state.selectedLanguage,
                onNavigateToLogin = onNavigateToLogin,
                onLanguageSelected = onLanguageSelected
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val density = LocalDensity.current
            val boxConstraints = constraints
            val width = with(density) { boxConstraints.maxWidth.toDp() }
            val height = with(density) { boxConstraints.maxHeight.toDp() }
            val layoutDirection = LocalLayoutDirection.current
            val isExpandedWidth = width >= 720.dp
            val isMediumWidth = width >= 600.dp
            val isCompactHeight = height <= 640.dp

            val sectionSpacing = if (isExpandedWidth) 32.dp else 24.dp
            val widthConstraint = when {
                isExpandedWidth -> 600.dp
                isMediumWidth -> 520.dp
                else -> Dp.Unspecified
            }
            val horizontalPaddingStart = when {
                isExpandedWidth -> 48.dp
                isMediumWidth -> 32.dp
                else -> contentPadding.calculateLeftPadding(layoutDirection)
            }
            val horizontalPaddingEnd = when {
                isExpandedWidth -> 48.dp
                isMediumWidth -> 32.dp
                else -> contentPadding.calculateRightPadding(layoutDirection)
            }
            val topPadding = when {
                isExpandedWidth -> 36.dp
                isCompactHeight -> 24.dp
                else -> contentPadding.calculateTopPadding()
            }
            val bottomPadding = when {
                isExpandedWidth -> 48.dp
                isCompactHeight -> 24.dp
                else -> contentPadding.calculateBottomPadding()
            }
            val headerCentered = isMediumWidth || isExpandedWidth
            val footerCentered = headerCentered
            val fieldSpacing = if (isExpandedWidth) 20.dp else 16.dp

            Column(
                modifier = Modifier
                    .widthIn(max = widthConstraint)
                    .align(Alignment.TopCenter)
                    .padding(
                        start = horizontalPaddingStart,
                        top = topPadding,
                        end = horizontalPaddingEnd,
                        bottom = bottomPadding
                    )
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing)
            ) {
                HeaderSection(
                    strings = strings,
                    centered = headerCentered
                )
                FormSection(
                    strings = strings,
                    state = state,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onConfirmPasswordChange = onConfirmPasswordChange,
                    onSignUpClick = onSignUpClick,
                    onGoogleSignUpClick = onGoogleSignUpClick,
                    onDismissError = onDismissError,
                    fieldSpacing = fieldSpacing
                )
                FooterSection(
                    switchLabel = strings.switchToLogin,
                    onNavigateToLogin = onNavigateToLogin,
                    centered = footerCentered
                )
            }
        }
    }
}

@Composable
private fun SignupTopBar(
    backLabel: String,
    languageLabel: String,
    selectedLanguage: LanguageOption,
    onNavigateToLogin: () -> Unit,
    onLanguageSelected: (LanguageOption) -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backLabel,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            LanguageSwitcher(
                label = languageLabel,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }
    }
}

@Composable
private fun HeaderSection(
    strings: SignupStrings,
    centered: Boolean
) {
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = strings.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = strings.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormSection(
    strings: SignupStrings,
    state: SignupUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignUpClick: () -> Unit,
    onDismissError: () -> Unit,
    fieldSpacing: Dp = 16.dp
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(fieldSpacing)) {
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = strings.emailLabel) },
            singleLine = true,
            leadingIcon = { IconWithTint(Icons.Filled.Email) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = strings.passwordLabel) },
            leadingIcon = { IconWithTint(Icons.Filled.Lock) },
            trailingIcon = {
                PasswordToggle(
                    isVisible = passwordVisible,
                    onToggle = { passwordVisible = !passwordVisible },
                    showLabel = strings.showPassword,
                    hideLabel = strings.hidePassword
                )
            },
            singleLine = true,
            supportingText = {
                Text(
                    text = strings.passwordSupporting,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = strings.confirmPasswordLabel) },
            leadingIcon = { IconWithTint(Icons.Filled.Lock) },
            trailingIcon = {
                PasswordToggle(
                    isVisible = confirmPasswordVisible,
                    onToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                    showLabel = strings.showPassword,
                    hideLabel = strings.hidePassword
                )
            },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSignUpClick() })
        )

        AnimatedVisibility(
            visible = state.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            state.errorMessage?.let { error ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissError() }
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }

        Button(
            onClick = onSignUpClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.loadingProvider == AuthProvider.Email) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(text = strings.loading)
            } else {
                Text(text = strings.primaryButton)
            }
        }

        OutlinedButton(
            onClick = onGoogleSignUpClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            if (state.loadingProvider == AuthProvider.Google) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = strings.loading,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = strings.googleIconDescription,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = strings.googleButton,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Text(
            text = strings.terms,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun FooterSection(
    switchLabel: String,
    onNavigateToLogin: () -> Unit,
    centered: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = switchLabel,
                color = MaterialTheme.colorScheme.primary,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class SignupStrings(
    val languageSwitchLabel: String,
    val backToLogin: String,
    val title: String,
    val subtitle: String,
    val emailLabel: String,
    val passwordLabel: String,
    val confirmPasswordLabel: String,
    val passwordSupporting: String,
    val terms: String,
    val primaryButton: String,
    val googleButton: String,
    val loading: String,
    val switchToLogin: String,
    val googleIconDescription: String,
    val showPassword: String,
    val hidePassword: String
)

@Composable
private fun rememberSignupStrings(language: LanguageOption): SignupStrings {
    val context = LocalContext.current
    return remember(language, context) {
        val localizedContext = context.localized(language)
        SignupStrings(
            languageSwitchLabel = localizedContext.getString(R.string.login_language_switch),
            backToLogin = localizedContext.getString(R.string.signup_back_to_login),
            title = localizedContext.getString(R.string.signup_title),
            subtitle = localizedContext.getString(R.string.signup_subtitle),
            emailLabel = localizedContext.getString(R.string.signup_email_label),
            passwordLabel = localizedContext.getString(R.string.signup_password_label),
            confirmPasswordLabel = localizedContext.getString(R.string.signup_confirm_password_label),
            passwordSupporting = localizedContext.getString(R.string.signup_password_supporting),
            terms = localizedContext.getString(R.string.signup_terms),
            primaryButton = localizedContext.getString(R.string.signup_primary_button),
            googleButton = localizedContext.getString(R.string.signup_google_button),
            loading = localizedContext.getString(R.string.signup_loading),
            switchToLogin = localizedContext.getString(R.string.signup_switch_to_login),
            googleIconDescription = localizedContext.getString(R.string.signup_google_icon_description),
            showPassword = localizedContext.getString(R.string.signup_show_password),
            hidePassword = localizedContext.getString(R.string.signup_hide_password)
        )
    }
}

@Composable
private fun PasswordToggle(
    isVisible: Boolean,
    onToggle: () -> Unit,
    showLabel: String,
    hideLabel: String
) {
    val icon: ImageVector = if (isVisible) {
        Icons.Outlined.VisibilityOff
    } else {
        Icons.Outlined.Visibility
    }
    val description = if (isVisible) hideLabel else showLabel
    IconButton(onClick = onToggle) {
        ImageVectorIcon(
            imageVector = icon,
            contentDescription = description
        )
    }
}

@Composable
private fun IconWithTint(
    imageVector: ImageVector
) {
    ImageVectorIcon(
        imageVector = imageVector,
        contentDescription = null
    )
}

@Composable
private fun ImageVectorIcon(
    imageVector: ImageVector,
    contentDescription: String?
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignupScreenPreview() {
    CS501_Micro_ChatTheme(darkTheme = false) {
        SignupScreen(
            state = SignupUiState(
                email = "hello@example.com",
                password = "Password123!",
                confirmPassword = "Password123!"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignUpClick = {},
            onGoogleSignUpClick = {},
            onNavigateToLogin = {},
            onLanguageSelected = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignupScreenDarkPreview() {
    CS501_Micro_ChatTheme(darkTheme = true) {
        SignupScreen(
            state = SignupUiState(
                email = "hello@example.com",
                password = "Password123!",
                confirmPassword = "Password123!",
                errorMessage = "Passwords do not match",
                loadingProvider = AuthProvider.Email
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignUpClick = {},
            onGoogleSignUpClick = {},
            onNavigateToLogin = {},
            onLanguageSelected = {},
            onDismissError = {}
        )
    }
}
