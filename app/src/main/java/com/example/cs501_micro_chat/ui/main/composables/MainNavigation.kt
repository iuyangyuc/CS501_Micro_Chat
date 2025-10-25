package com.example.cs501_micro_chat.ui.main.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cs501_micro_chat.ui.auth.composables.LoginScreen
import com.example.cs501_micro_chat.ui.signup.SignupRoute

private object AuthDestinations {
    const val Signup = "signup"
    const val Login = "login"
}

@Composable
fun MicroChatApp() {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AuthDestinations.Signup
        ) {
            composable(route = AuthDestinations.Signup) {
                SignupRoute(
                    onNavigateToLogin = {
                        navController.navigate(AuthDestinations.Login)
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
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSignup = {
                        navController.navigate(AuthDestinations.Signup) {
                            popUpTo(AuthDestinations.Signup) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}
