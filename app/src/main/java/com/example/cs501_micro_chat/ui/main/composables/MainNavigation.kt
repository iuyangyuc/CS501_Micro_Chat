package com.example.cs501_micro_chat.ui.main.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cs501_micro_chat.ui.debug.DebugScreen
import com.example.cs501_micro_chat.ui.login.LoginRoute
import com.example.cs501_micro_chat.ui.signup.SignupRoute

private object AuthDestinations {
    const val Signup = "signup"
    const val Login = "login"

    const val fireConnDebug = "fDebug"

}

@Composable
fun MicroChatApp() {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
//            startDestination = AuthDestinations.Login
            startDestination = AuthDestinations.fireConnDebug
        ) {
            composable(route = AuthDestinations.Signup) {
                SignupRoute(
                    onNavigateToLogin = {
                        navController.popBackStack(AuthDestinations.Login, inclusive = false)
                    },
                    onSignupSuccess = {
                        navController.navigate(AuthDestinations.Login) {
                            popUpTo(AuthDestinations.Signup) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(route = AuthDestinations.Login) {
                LoginRoute(
                    onNavigateToSignup = {
                        navController.navigate(AuthDestinations.Signup) {
                            popUpTo(AuthDestinations.Signup) {
                                inclusive = true
                            }
                        }
                    },
                    onLoginSuccess = {
                        // TODO: Navigate to conversations once implemented.
                    }
                )
            }
            composable(route = AuthDestinations.fireConnDebug) {
                DebugScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
