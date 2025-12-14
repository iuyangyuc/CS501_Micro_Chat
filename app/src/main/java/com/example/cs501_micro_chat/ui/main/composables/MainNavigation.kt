package com.example.cs501_micro_chat.ui.main.composables

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cs501_micro_chat.ui.login.LoginRoute
import com.example.cs501_micro_chat.ui.main.HomeScreen
import com.example.cs501_micro_chat.ui.signup.SignupRoute

private object AuthDestinations {
    const val Signup = "signup"
    const val Login = "login"
    const val Home = "home"
}

@Composable
fun MicroChatApp() {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AuthDestinations.Login
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
                        navController.navigate(AuthDestinations.Signup)
                    },
                    onLoginSuccess = {
                        Log.d("MainNavigation", "onLoginSuccess: Navigating to Home screen")
                        try {
                            navController.navigate(AuthDestinations.Home) {
                                popUpTo(AuthDestinations.Login) {
                                    inclusive = true
                                }
                            }
                            Log.d("MainNavigation", "onLoginSuccess: Navigation succeeded")
                        } catch (e: Exception) {
                            Log.e("MainNavigation", "onLoginSuccess: Navigation failed", e)
                        }
                    }
                )
            }
            composable(route = AuthDestinations.Home) {
                HomeScreen(
                    onLogout = {
                        // Navigate back to login screen
                        navController.navigate(AuthDestinations.Login) {
                            popUpTo(AuthDestinations.Home) {
                                inclusive = true
                            }
                        }
                    }
                )
            }
        }
    }
}
