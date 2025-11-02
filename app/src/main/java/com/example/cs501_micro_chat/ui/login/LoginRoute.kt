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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    viewModel: LoginViewModel = viewModel()
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
            if (event is LoginEvent.LoginSuccess) {
                onLoginSuccess()
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
