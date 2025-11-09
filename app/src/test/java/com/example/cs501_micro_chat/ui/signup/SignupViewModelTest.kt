package com.example.cs501_micro_chat.ui.signup

import com.example.cs501_micro_chat.MainDispatcherRule
import com.example.cs501_micro_chat.data.repository.AuthRepository
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun passwordMismatch_setsErrorMessage() = runTest {
        val repository = FakeSignupRepository()
        val viewModel = SignupViewModel(repository)

        viewModel.onLanguageSelected(LanguageOption.English)
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onConfirmPasswordChange("Password2!")

        viewModel.signUpWithEmail()

        val state = viewModel.uiState.value
        assertEquals("Passwords do not match.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun successfulEmailSignup_emitsSuccessEvent() = runTest {
        val repository = FakeSignupRepository()
        val viewModel = SignupViewModel(repository)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onConfirmPasswordChange("Password1!")

        val eventDeferred = async { viewModel.events.first() }
        viewModel.signUpWithEmail()

        assertEquals(SignupEvent.SignupSuccess, eventDeferred.await())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun googleSignInFailure_setsErrorMessage() = runTest {
        val repository = FakeSignupRepository().apply {
            shouldFailGoogle = true
        }
        val viewModel = SignupViewModel(repository)

        viewModel.onGoogleSignInStarted()
        viewModel.onGoogleIdTokenReceived("fake-token")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(!state.errorMessage.isNullOrBlank())
    }

    private class FakeSignupRepository : AuthRepository {
        var shouldFailEmail = false
        var shouldFailGoogle = false

        override suspend fun createUser(email: String, password: String) {
            if (shouldFailEmail) {
                throw IllegalStateException("Email signup failed")
            }
        }

        override suspend fun signInWithEmail(email: String, password: String) {
            if (shouldFailEmail) {
                throw IllegalStateException("Email sign-in failed")
            }
        }

        override suspend fun signInWithGoogle(idToken: String) {
            if (shouldFailGoogle) {
                throw IllegalStateException("Google sign-in failed")
            }
        }
    }
}
