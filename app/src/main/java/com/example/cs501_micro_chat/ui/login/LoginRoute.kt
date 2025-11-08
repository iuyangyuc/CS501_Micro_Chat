/**
 * LoginRoute.kt
 *
 * 登录路由文件 - 处理登录界面的导航和 Google 登录集成
 * Login Route - Handles login navigation and Google Sign-In integration
 *
 * 主要功能 / Main Functions:
 * - 配置 Google 登录客户端 / Configure Google Sign-In client
 * - 处理 Google 登录结果回调 / Handle Google Sign-In result callbacks
 * - 管理登录成功事件导航 / Manage login success navigation
 * - 协调 ViewModel 和 UI Screen / Coordinate ViewModel and UI Screen
 *
 * @author CS501 Team
 * @date 2025-11-02
 */
package com.example.cs501_micro_chat.ui.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cs501_micro_chat.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

@Composable
fun LoginRoute(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    onViewTerms: () -> Unit = {},
    onViewPrivacy: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleSignInClient: GoogleSignInClient = remember(context) {
        val webClientId = context.getString(R.string.default_web_client_id)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(LOGIN_ROUTE_TAG, "Google sign-in resultCode=${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            Log.d(LOGIN_ROUTE_TAG, "Google account received: ${account?.email}")
            viewModel.onGoogleIdTokenReceived(account?.idToken)
        } else {
            val error = task.exception
            if (error is ApiException) {
                Log.w(LOGIN_ROUTE_TAG, "Google sign-in failed with status=${error.statusCode}", error)
                if (error.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED || result.resultCode == Activity.RESULT_CANCELED) {
                    viewModel.onGoogleSignInCancelled()
                } else {
                    viewModel.onGoogleSignInFailed(error)
                }
            } else {
                Log.w(LOGIN_ROUTE_TAG, "Google sign-in failed with unknown error", error)
                if (result.resultCode == Activity.RESULT_CANCELED) {
                    viewModel.onGoogleSignInCancelled()
                } else {
                    viewModel.onGoogleSignInFailed(
                        error ?: IllegalStateException("Google sign-in cancelled.")
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            Log.d(LOGIN_ROUTE_TAG, "Received event: $event")
            if (event is LoginEvent.LoginSuccess) {
                try {
                    Log.d(LOGIN_ROUTE_TAG, "Calling onLoginSuccess callback")
                    onLoginSuccess()
                    Log.d(LOGIN_ROUTE_TAG, "onLoginSuccess callback completed")
                } catch (e: Exception) {
                    Log.e(LOGIN_ROUTE_TAG, "Error in onLoginSuccess callback", e)
                }
            }
        }
    }

    LoginScreen(
        state = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onAgreementCheckedChange = viewModel::onAgreementCheckedChange,
        onProviderSelected = viewModel::onProviderSelected,
        onResetProviderSelection = viewModel::resetProviderSelection,
        onLanguageSelected = viewModel::onLanguageSelected,
        onLoginClick = viewModel::signInWithEmail,
        onGoogleLoginClick = {
            val shouldLaunch = viewModel.onGoogleSignInRequest()
            if (shouldLaunch) {
                runCatching {
                    Log.d(LOGIN_ROUTE_TAG, "Launching Google sign-in intent")
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }.onFailure { error ->
                    Log.e(LOGIN_ROUTE_TAG, "Failed to launch Google sign-in intent", error)
                    val throwable = if (error is ActivityNotFoundException) {
                        IllegalStateException("Google Play Services is unavailable on this device.", error)
                    } else {
                        error
                    }
                    viewModel.onGoogleSignInFailed(throwable)
                }
            }
        },
        onNavigateToSignup = onNavigateToSignup,
        onDismissError = viewModel::dismissError,
        onViewTerms = onViewTerms,
        onViewPrivacy = onViewPrivacy,
        headerLogTag = LOGIN_ROUTE_TAG
    )
}

private const val LOGIN_ROUTE_TAG = "LoginRoute"
