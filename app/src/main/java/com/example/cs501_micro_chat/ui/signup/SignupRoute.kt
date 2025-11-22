package com.example.cs501_micro_chat.ui.signup

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

@Composable
fun SignupRoute(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val tag = SIGNUP_ROUTE_TAG
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
        Log.d(tag, "Google sign-in resultCode=${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account = task.result
            Log.d(tag, "Google account received: ${account?.email}")
            viewModel.onGoogleIdTokenReceived(account?.idToken)
        } else {
            val error = task.exception
            if (error is ApiException) {
                Log.w(tag, "Google sign-in failed with status=${error.statusCode}", error)
                viewModel.onGoogleSignInFailed(error)
            } else {
                Log.w(tag, "Google sign-in failed with unknown error", error)
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
            if (event is SignupEvent.SignupSuccess) {
                onSignupSuccess()
            }
        }
    }

    SignupScreen(
        state = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSignUpClick = viewModel::signUpWithEmail,
        onGoogleSignUpClick = {
            viewModel.onGoogleSignInStarted()
            runCatching {
                Log.d(tag, "Launching Google sign-in intent")
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }.onFailure { error ->
                Log.e(tag, "Failed to launch Google sign-in intent", error)
                val throwable = if (error is ActivityNotFoundException) {
                    IllegalStateException("Google Play Services is unavailable on this device.", error)
                } else {
                    error
                }
                viewModel.onGoogleSignInFailed(throwable)
            }
        },
        onNavigateToLogin = onNavigateToLogin,
        onDismissError = viewModel::dismissError,
        onLanguageSelected = viewModel::onLanguageSelected
    )
}

private const val SIGNUP_ROUTE_TAG = "SignupRoute"
