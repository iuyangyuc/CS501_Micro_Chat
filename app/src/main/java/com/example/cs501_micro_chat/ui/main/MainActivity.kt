package com.example.cs501_micro_chat.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cs501_micro_chat.ui.common.LanguagePreferenceObserver
import com.example.cs501_micro_chat.ui.main.composables.MicroChatApp
import com.example.cs501_micro_chat.ui.theme.CS501_Micro_ChatTheme
import com.example.cs501_micro_chat.ui.theme.ThemeOption
import com.example.cs501_micro_chat.ui.theme.ThemeViewModel
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    @Suppress("unused")
    lateinit var languagePreferenceObserver: LanguagePreferenceObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up global exception handler to catch crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MainActivity", "Uncaught exception in thread ${thread.name}", throwable)
            // Print detailed stack trace
            throwable.printStackTrace()
            // Re-throw exception to show default crash dialog
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()

        uploadTestFile()

        try {
            setContent {
                val themeViewModel: ThemeViewModel = hiltViewModel()
                val themeOption by themeViewModel.themeOption.collectAsStateWithLifecycle()
                val systemDark = isSystemInDarkTheme()
                val useDarkTheme = when (themeOption) {
                    ThemeOption.SYSTEM -> systemDark
                    ThemeOption.DARK -> true
                    ThemeOption.LIGHT -> false
                }
                CS501_Micro_ChatTheme(darkTheme = useDarkTheme) {
                    MicroChatApp()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in setContent", e)
            throw e
        }
    }

    private fun uploadTestFile() {
        val storage = FirebaseStorage.getInstance()
        val ref = storage.reference.child("test_files/hello.txt")
        val data = "Hello Firebase Storage!".toByteArray()

        ref.putBytes(data)
            .addOnSuccessListener {
                Log.d("StorageTest", "Upload success!")
            }
            .addOnFailureListener { error ->
                Log.e("StorageTest", "Upload failed: ${error.message}")
            }
    }
}
