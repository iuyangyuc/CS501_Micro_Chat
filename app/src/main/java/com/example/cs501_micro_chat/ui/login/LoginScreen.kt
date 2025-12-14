/**
 * LoginScreen.kt
 *
 * Login Screen UI Component - Provides complete login visual interface and interactions
 *
 * Main Functions:
 * - Responsive animated login card layout
 * - Email login form (email + password)
 * - Google third-party login interface
 * - Language switching (Chinese/English)
 * - Terms and privacy policy confirmation
 * - Dynamic background and transition animations
 * - Error messages and loading state display
 *
 * Architecture:
 * - Uses Jetpack Compose declarative UI
 * - Responsive layout adapts to different screen sizes
 * - Material Design 3 design guidelines
 *
 * @author CS501 Team
 * @date 2025-11-02
 */
package com.example.cs501_micro_chat.ui.login

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.cs501_micro_chat.R
import com.example.cs501_micro_chat.ui.auth.AuthProvider
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.auth.LanguageSwitcher
import com.example.cs501_micro_chat.ui.auth.localized
import com.example.cs501_micro_chat.ui.theme.CS501_Micro_ChatTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgreementCheckedChange: (Boolean) -> Unit,
    onProviderSelected: (AuthProvider) -> Unit,
    onResetProviderSelection: () -> Unit,
    onLanguageSelected: (LanguageOption) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onDismissError: () -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
    headerLogTag: String = "LoginScreen"
) {
    val strings = rememberLoginStrings(state.selectedLanguage)
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val chipColor = MaterialTheme.colorScheme.surfaceVariant
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    val hasFieldFocus = emailFocused || passwordFocused
    val isFormFocused = state.activeProvider == AuthProvider.Email && hasFieldFocus

    LaunchedEffect(state.activeProvider) {
        if (state.activeProvider != AuthProvider.Email) {
            emailFocused = false
            passwordFocused = false
        }
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Use BoxWithConstraintsScope properties directly
        val width = this.maxWidth
        val height = this.maxHeight
        val layoutDirection = LocalLayoutDirection.current
        val widthRatio = (width.value / 360f).coerceIn(1f, 3f)
        val heightRatio = (height.value / 720f).coerceIn(0.5f, 1.8f)
        val compactFactor = 1.1f - heightRatio

        val collapsedCardFraction = (0.45f + compactFactor * 0.20f).coerceIn(0.35f, 0.65f)
        val googleCardFraction = (collapsedCardFraction + 0.15f + compactFactor * 0.12f - (widthRatio - 1f) * 0.04f)
            .coerceIn(0.35f, 0.90f)
        val emailCardFraction = (collapsedCardFraction + 0.30f).coerceIn(0.70f, 0.95f)
        val focusCardFraction = (emailCardFraction + 0.08f - compactFactor * 0.05f).coerceIn(0.83f, 1.0f)

        val targetCardFraction = when {
            state.activeProvider == null -> collapsedCardFraction
            isFormFocused -> focusCardFraction
            state.activeProvider == AuthProvider.Google -> googleCardFraction
            else -> emailCardFraction
        }

        val cardFraction by animateFloatAsState(
            targetValue = targetCardFraction,
            label = "cardFraction"
        )

        val headerOverlap = (0.12f + (widthRatio - 1f) * 0.04f).coerceIn(0.08f, 0.20f)
        val minHeaderFraction = (0.30f - (heightRatio - 1f) * 0.06f).coerceIn(0.20f, 0.40f)
        val maxHeaderFraction = (0.90f + (widthRatio - 1f) * 0.04f).coerceIn(0.45f, 0.90f)
        val backgroundFraction = (1f - cardFraction + headerOverlap + 0.1f).coerceIn(minHeaderFraction, maxHeaderFraction)

        val overlayAlphaTarget = when {
            state.activeProvider == null -> 0f
            isFormFocused -> 0.35f
            else -> (0.18f + (heightRatio - 1f) * 0.04f).coerceIn(0.15f, 0.26f)
        }
        val overlayAlpha by animateFloatAsState(
            targetValue = overlayAlphaTarget,
            label = "overlayAlpha"
        )

        val baseElevation = (6f + (widthRatio - 1f) * 6f).coerceIn(6f, 18f)
        val cardElevationTarget = when {
            state.activeProvider == null -> baseElevation.dp
            isFormFocused -> minOf(baseElevation + 6f, 22f).dp
            else -> minOf(baseElevation + 2f, 20f).dp
        }
        val cardElevation by animateDpAsState(
            targetValue = cardElevationTarget,
            label = "cardElevation"
        )

        val cardCornerRadius = (28f + (widthRatio - 1f) * 8f).coerceIn(28f, 42f).dp
        val cardShape = RoundedCornerShape(topStart = cardCornerRadius, topEnd = cardCornerRadius, bottomStart = 0.dp, bottomEnd = 0.dp)

        val cardWidthModifier = Modifier.fillMaxWidth()

        val cardHeightFraction = cardFraction.coerceIn(0.35f, 1.0f)
        val baseHorizontalPadding = (24f + (widthRatio - 1f) * 12f).coerceIn(20f, 44f).dp
        val baseVerticalPadding = (24f + (1f - heightRatio) * 12f).coerceIn(18f, 34f).dp
        val resolvedHorizontal = maxOf(
            baseHorizontalPadding,
            contentPadding.calculateLeftPadding(layoutDirection),
            contentPadding.calculateRightPadding(layoutDirection)
        )
        val resolvedVertical = maxOf(
            baseVerticalPadding,
            contentPadding.calculateTopPadding(),
            contentPadding.calculateBottomPadding()
        )
        val cardContentPadding = PaddingValues(
            horizontal = resolvedHorizontal,
            vertical = resolvedVertical
        )

        val googleSectionMinHeight = (320f + (heightRatio - 1f) * 120f).coerceIn(260f, 440f).dp
        val googleHorizontalPadding = (16f + (widthRatio - 1f) * 6f).coerceIn(16f, 28f).dp
        val googleTitleOffset = (12f + (heightRatio - 1f) * 24f).coerceIn(-8f, 28f).dp

        // Language switcher position - adaptive to screen size
        val languageOffsetEnd = ((width - 360.dp).coerceAtLeast(0.dp) * 0.2f).coerceAtMost(160.dp)
        val languageOffsetTop = (height * 0.08f).coerceIn(48.dp, 120.dp)

        HeaderBackground(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(backgroundFraction)
                .align(Alignment.TopCenter),
            overlayAlpha = overlayAlpha,
            logTag = headerLogTag
        )

        Surface(
            modifier = cardWidthModifier
                .fillMaxHeight(cardHeightFraction)
                .background(Color.Transparent)
                .align(Alignment.BottomCenter),
            color = Color.Transparent,
            tonalElevation = cardElevation,
            shadowElevation = cardElevation,
            shape = cardShape
        ) {
            Box(
                modifier = Modifier
                    .clip(cardShape)
                    .background(cardColor)
            ) {
                LoginCardContent(
                    strings = strings,
                    state = state,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onAgreementCheckedChange = onAgreementCheckedChange,
                    onProviderSelected = onProviderSelected,
                    onResetProviderSelection = onResetProviderSelection,
                    onLoginClick = onLoginClick,
                    onGoogleLoginClick = onGoogleLoginClick,
                    onNavigateToSignup = onNavigateToSignup,
                    onDismissError = onDismissError,
                    onViewTerms = onViewTerms,
                    onViewPrivacy = onViewPrivacy,
                    onEmailFocusChange = { emailFocused = it },
                    onPasswordFocusChange = { passwordFocused = it },
                    contentPadding = cardContentPadding,
                    googleSectionMinHeight = googleSectionMinHeight,
                    googleHorizontalPadding = googleHorizontalPadding,
                    googleTitleOffset = googleTitleOffset
                )
            }
        }

        // Language switcher - ensure it's on top layer and clickable, adaptive position
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = languageOffsetTop, end = 16.dp + languageOffsetEnd),
            color = chipColor.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            tonalElevation = 4.dp
        ) {
            LanguageSwitcher(
                label = strings.languageSwitchLabel,
                selectedLanguage = state.selectedLanguage,
                onLanguageSelected = onLanguageSelected,
                modifier = Modifier
            )
        }

    }
}

@Composable
private fun HeaderBackground(
    modifier: Modifier = Modifier,
    overlayAlpha: Float,
    logTag: String
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.gradient_text_stacked))
    val context = LocalContext.current
    val fallbackImage = remember(context) {
        runCatching {
            context.resources.openRawResource(R.raw.background).use(BitmapFactory::decodeStream)?.asImageBitmap()
        }.getOrNull()
    }
    val progress by animateLottieCompositionAsState(
        composition = compositionResult.value,
        iterations = LottieConstants.IterateForever,
        isPlaying = compositionResult.value != null
    )

    LaunchedEffect(compositionResult.value) {
        val status = if (compositionResult.value != null) "loaded" else "loading"
        Log.d(logTag, "Lottie composition $status")
    }
    LaunchedEffect(compositionResult.error) {
        compositionResult.error?.let { error ->
            Log.e(logTag, "Failed to load Lottie composition", error)
        }
    }

    val fallbackGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Box(modifier = modifier) {
        val composition = compositionResult.value
        val shouldFallback = compositionResult.isFailure || composition == null

        if (!shouldFallback && composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.FillBounds,
                clipToCompositionBounds = false,
                safeMode = true
            )
        }

        if (shouldFallback) {
            if (fallbackImage != null) {
                Image(
                    bitmap = fallbackImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallbackGradient)
                )
            }
        }

        if (overlayAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = overlayAlpha)
                    )
            )
        }
    }
}

@Composable
private fun LoginCardContent(
    strings: LoginStrings,
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgreementCheckedChange: (Boolean) -> Unit,
    onProviderSelected: (AuthProvider) -> Unit,
    onResetProviderSelection: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onDismissError: () -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit,
    onEmailFocusChange: (Boolean) -> Unit,
    onPasswordFocusChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    googleSectionMinHeight: Dp,
    googleHorizontalPadding: Dp,
    googleTitleOffset: Dp
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(state.activeProvider) {
        if (state.activeProvider != AuthProvider.Email) {
            passwordVisible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AnimatedVisibility(
            visible = state.errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            state.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissError() }
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }

        when (state.activeProvider) {
            null -> ProviderSelection(strings, onProviderSelected, onNavigateToSignup)
            AuthProvider.Email, AuthProvider.Google -> {
                MethodSwitchRow(
                    strings = strings,
                    selected = state.activeProvider,
                    onProviderSelected = onProviderSelected,
                    onReset = onResetProviderSelection
                )
                Spacer(modifier = Modifier.size(6.dp))
                when (state.activeProvider) {
                    AuthProvider.Email -> EmailLoginSection(
                        strings = strings,
                        state = state,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onAgreementCheckedChange = onAgreementCheckedChange,
                        onLoginClick = onLoginClick,
                        onNavigateToSignup = onNavigateToSignup,
                        onViewTerms = onViewTerms,
                        onViewPrivacy = onViewPrivacy,
                        onEmailFocusChange = onEmailFocusChange,
                        onPasswordFocusChange = onPasswordFocusChange,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = it }
                    )

                    AuthProvider.Google -> GoogleLoginSection(
                        strings = strings,
                        state = state,
                        onAgreementCheckedChange = onAgreementCheckedChange,
                        onGoogleLoginClick = onGoogleLoginClick,
                        onNavigateToSignup = onNavigateToSignup,
                        onViewTerms = onViewTerms,
                        onViewPrivacy = onViewPrivacy,
                        minHeight = googleSectionMinHeight,
                        horizontalPadding = googleHorizontalPadding,
                        titleOffset = googleTitleOffset
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderSelection(
    strings: LoginStrings,
    onProviderSelected: (AuthProvider) -> Unit,
    onNavigateToSignup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.chooseMethodTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = strings.chooseMethodSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = { onProviderSelected(AuthProvider.Email) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(text = strings.chooseEmail)
        }

        OutlinedButton(
            onClick = { onProviderSelected(AuthProvider.Google) },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(text = strings.chooseGoogle)
        }

        // Register button - allows user to navigate to signup page
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = strings.registerHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onNavigateToSignup),
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
private fun MethodSwitchRow(
    strings: LoginStrings,
    selected: AuthProvider,
    onProviderSelected: (AuthProvider) -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderToggleButton(
            text = strings.emailMethodLabel,
            isSelected = selected == AuthProvider.Email,
            onClick = { onProviderSelected(AuthProvider.Email) },
            modifier = Modifier.weight(1f)
        )
        ProviderToggleButton(
            text = strings.googleMethodLabel,
            isSelected = selected == AuthProvider.Google,
            onClick = { onProviderSelected(AuthProvider.Google) },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = strings.switchMethod,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable { onReset() }
        )
    }
}

@Composable
private fun ProviderToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(text = text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(text = text)
        }
    }
}

@Composable
private fun EmailLoginSection(
    strings: LoginStrings,
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAgreementCheckedChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit,
    onEmailFocusChange: (Boolean) -> Unit,
    onPasswordFocusChange: (Boolean) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.emailLoginTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = strings.emailLoginSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onEmailFocusChange(it.isFocused) },
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
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onPasswordFocusChange(it.isFocused) },
            label = { Text(text = strings.passwordLabel) },
            singleLine = true,
            leadingIcon = { IconWithTint(Icons.Filled.Lock) },
            trailingIcon = {
                PasswordToggle(
                    isVisible = passwordVisible,
                    onToggle = { onPasswordVisibilityChange(!passwordVisible) },
                    showLabel = strings.showPassword,
                    hideLabel = strings.hidePassword
                )
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onLoginClick() })
        )

        AgreementRow(
            strings = strings,
            checked = state.agreementChecked,
            onCheckedChange = onAgreementCheckedChange,
            onViewTerms = onViewTerms,
            onViewPrivacy = onViewPrivacy
        )

        Button(
            onClick = onLoginClick,
            enabled = state.isEmailFormReady,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (state.loadingProvider == AuthProvider.Email) {
                LoadingIndicator(text = strings.loading)
            } else {
                Text(text = strings.primaryButton)
            }
        }

        Text(
            text = strings.registerHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onNavigateToSignup),
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
private fun GoogleLoginSection(
    strings: LoginStrings,
    state: LoginUiState,
    onAgreementCheckedChange: (Boolean) -> Unit,
    onGoogleLoginClick: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit,
    minHeight: Dp,
    horizontalPadding: Dp,
    titleOffset: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 10.dp)
            .heightIn(min = minHeight),
//        verticalArrangement = Arrangement.spacedBy(20.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title section at the top
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
            //modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = strings.googleLoginTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = strings.googleLoginSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        //Spacer(modifier = Modifier.weight(1f))

        // Bottom section with agreement and button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f),
            //modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AgreementRow(
                strings = strings,
                checked = state.agreementChecked,
                onCheckedChange = onAgreementCheckedChange,
                onViewTerms = onViewTerms,
                onViewPrivacy = onViewPrivacy,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onGoogleLoginClick,
                enabled = state.agreementChecked && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (state.loadingProvider == AuthProvider.Google) {
                    LoadingIndicator(
                        text = strings.loading,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = strings.googleIconDescription,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.googleButton,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = strings.registerHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onNavigateToSignup),
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Composable
private fun AgreementRow(
    strings: LoginStrings,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        pushStringAnnotation(tag = TERMS_TAG, annotation = TERMS_TAG)
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
            append(strings.agreementTerms)
        }
        pop()
        append(" ${strings.agreementConnector} ")
        pushStringAnnotation(tag = PRIVACY_TAG, annotation = PRIVACY_TAG)
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
            append(strings.agreementPrivacy)
        }
        pop()
        append(strings.agreementSuffix)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        AgreementToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = strings.toggleDescription,
            checkedLabel = strings.toggleChecked,
            uncheckedLabel = strings.toggleUnchecked
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.agreementPrompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onCheckedChange(!checked) }
            )
            ClickableText(
                text = annotated,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { offset ->
                    val annotation = annotated.getStringAnnotations(start = offset, end = offset)
                        .firstOrNull()
                    when (annotation?.tag) {
                        TERMS_TAG -> onViewTerms()
                        PRIVACY_TAG -> onViewPrivacy()
                        else -> onCheckedChange(!checked)
                    }
                }
            )
        }
    }
}

@Composable
private fun AgreementToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    checkedLabel: String,
    uncheckedLabel: String
) {
    val icon = if (checked) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked
    val tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(15.dp)
            .padding(top = 1.dp)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (checked) checkedLabel else uncheckedLabel
            }
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

private data class LoginStrings(
    val languageSwitchLabel: String,
    val chooseMethodTitle: String,
    val chooseMethodSubtitle: String,
    val chooseEmail: String,
    val chooseGoogle: String,
    val emailMethodLabel: String,
    val googleMethodLabel: String,
    val switchMethod: String,
    val emailLoginTitle: String,
    val emailLoginSubtitle: String,
    val googleLoginTitle: String,
    val googleLoginSubtitle: String,
    val emailLabel: String,
    val passwordLabel: String,
    val showPassword: String,
    val hidePassword: String,
    val primaryButton: String,
    val googleButton: String,
    val googleIconDescription: String,
    val loading: String,
    val registerHint: String,
    val agreementPrompt: String,
    val toggleDescription: String,
    val toggleChecked: String,
    val toggleUnchecked: String,
    val agreementConnector: String,
    val agreementTerms: String,
    val agreementPrivacy: String,
    val agreementSuffix: String
)

@Composable
private fun rememberLoginStrings(language: LanguageOption): LoginStrings {
    val context = LocalContext.current
    return remember(language, context) {
        val localizedContext = context.localized(language)
        LoginStrings(
            languageSwitchLabel = localizedContext.getString(R.string.login_language_switch),
            chooseMethodTitle = localizedContext.getString(R.string.login_choose_method_title),
            chooseMethodSubtitle = localizedContext.getString(R.string.login_choose_method_subtitle),
            chooseEmail = localizedContext.getString(R.string.login_choose_email),
            chooseGoogle = localizedContext.getString(R.string.login_choose_google),
            emailMethodLabel = localizedContext.getString(R.string.login_method_email_label),
            googleMethodLabel = localizedContext.getString(R.string.login_method_google_label),
            switchMethod = localizedContext.getString(R.string.login_switch_method),
            emailLoginTitle = localizedContext.getString(R.string.login_email_signin_title),
            emailLoginSubtitle = localizedContext.getString(R.string.login_email_signin_subtitle),
            googleLoginTitle = localizedContext.getString(R.string.login_google_signin_title),
            googleLoginSubtitle = localizedContext.getString(R.string.login_google_signin_subtitle),
            emailLabel = localizedContext.getString(R.string.login_email_label),
            passwordLabel = localizedContext.getString(R.string.login_password_label),
            showPassword = localizedContext.getString(R.string.signup_show_password),
            hidePassword = localizedContext.getString(R.string.signup_hide_password),
            primaryButton = localizedContext.getString(R.string.login_primary_button),
            googleButton = localizedContext.getString(R.string.login_google_button),
            googleIconDescription = localizedContext.getString(R.string.signup_google_icon_description),
            loading = localizedContext.getString(R.string.login_loading),
            registerHint = localizedContext.getString(R.string.login_register_hint),
            agreementPrompt = localizedContext.getString(R.string.login_agreement_prompt),
            toggleDescription = localizedContext.getString(R.string.login_agreement_toggle_description),
            toggleChecked = localizedContext.getString(R.string.login_agreement_toggle_checked),
            toggleUnchecked = localizedContext.getString(R.string.login_agreement_toggle_unchecked),
            agreementConnector = localizedContext.getString(R.string.login_agreement_connector),
            agreementTerms = localizedContext.getString(R.string.login_agreement_terms),
            agreementPrivacy = localizedContext.getString(R.string.login_agreement_privacy),
            agreementSuffix = localizedContext.getString(R.string.login_agreement_suffix)
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
    val description = if (isVisible) {
        hideLabel
    } else {
        showLabel
    }
    IconButton(onClick = onToggle) {
        IconWithTint(
            imageVector = icon,
            contentDescription = description
        )
    }
}

@Composable
private fun RowScope.LoadingIndicator(
    text: String,
    color: Color = MaterialTheme.colorScheme.onPrimary
) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = color,
        strokeWidth = 2.dp
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(text = text)
}

@Composable
private fun IconWithTint(
    imageVector: ImageVector,
    contentDescription: String? = null
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private const val TERMS_TAG = "terms"
private const val PRIVACY_TAG = "privacy"

@Preview(showBackground = true, showSystemUi = true, locale = "zh")
@Composable
private fun LoginScreenPreview() {
    CS501_Micro_ChatTheme(darkTheme = false) {
        LoginScreen(
            state = LoginUiState(
                email = "lf1991@bu.edu",
                password = "123456",
                agreementChecked = true,
                activeProvider = AuthProvider.Email
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onAgreementCheckedChange = {},
            onProviderSelected = {},
            onResetProviderSelection = {},
            onLanguageSelected = {},
            onLoginClick = {},
            onGoogleLoginClick = {},
            onNavigateToSignup = {},
            onDismissError = {},
            onViewTerms = {},
            onViewPrivacy = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "en")
@Composable
private fun LoginScreenErrorPreview() {
    CS501_Micro_ChatTheme(darkTheme = false) {
        LoginScreen(
            state = LoginUiState(
                email = "lf1991@bu.edu",
                password = "123456",
                loadingProvider = AuthProvider.Email,
                errorMessage = "Email or password is incorrect.",
                agreementChecked = false,
                activeProvider = AuthProvider.Email
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onAgreementCheckedChange = {},
            onProviderSelected = {},
            onResetProviderSelection = {},
            onLanguageSelected = {},
            onLoginClick = {},
            onGoogleLoginClick = {},
            onNavigateToSignup = {},
            onDismissError = {},
            onViewTerms = {},
            onViewPrivacy = {}
        )
    }
}
