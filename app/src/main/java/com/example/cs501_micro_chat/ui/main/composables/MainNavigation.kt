package com.example.cs501_micro_chat.ui.main.composables

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cs501_micro_chat.ui.auth.LanguageOption
import com.example.cs501_micro_chat.ui.debug.DebugScreen
import com.example.cs501_micro_chat.ui.login.LoginRoute
import com.example.cs501_micro_chat.ui.main.HomeScreen
import com.example.cs501_micro_chat.ui.signup.SignupRoute

private object AuthDestinations {
    const val Signup = "signup"
    const val Login = "login"
    const val Home = "home"
    const val fireConnDebug = "fDebug"
}

private const val SIGNUP_LANGUAGE_KEY = "signup_language"

@Composable
fun MicroChatApp() {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AuthDestinations.Login
//            startDestination = AuthDestinations.fireConnDebug  // 临时：重新创建测试数据
        ) {
            composable(route = AuthDestinations.Signup) {
                val languageName = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(SIGNUP_LANGUAGE_KEY)
                val initialLanguage = languageName
                    ?.let { runCatching { LanguageOption.valueOf(it) }.getOrNull() }
                    ?: LanguageOption.Chinese
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.remove<String>(SIGNUP_LANGUAGE_KEY)
                SignupRoute(
                    initialLanguage = initialLanguage,
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
            composable(route = AuthDestinations.Login) { backStackEntry ->
                LoginRoute(
                    onNavigateToSignup = { language ->
                        backStackEntry.savedStateHandle[SIGNUP_LANGUAGE_KEY] = language.name
                        navController.navigate(AuthDestinations.Signup) {
                            popUpTo(AuthDestinations.Signup) {
                                inclusive = true
                            }
                        }
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
            composable(route = AuthDestinations.fireConnDebug) {
                DebugScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
