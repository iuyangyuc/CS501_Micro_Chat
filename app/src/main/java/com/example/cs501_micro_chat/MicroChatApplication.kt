/**
 * MicroChatApplication.kt
 *
 * 应用程序入口类 - 初始化 Hilt 和 Firebase
 * Application Entry Point - Initializes Hilt and Firebase
 *
 * @author CS501 Team
 */
package com.example.cs501_micro_chat

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MicroChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase 会自动初始化，但我们可以在这里做额外的配置
        FirebaseApp.initializeApp(this)
    }
}

