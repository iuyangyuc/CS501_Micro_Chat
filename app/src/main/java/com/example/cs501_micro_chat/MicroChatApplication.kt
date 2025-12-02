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
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class MicroChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase 会自动初始化，但我们可以在这里做额外的配置
        FirebaseApp.initializeApp(this)

        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logUncaughtCrash(thread, throwable)
            // 交给系统默认处理（崩溃对用户可见）
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logUncaughtCrash(thread: Thread, throwable: Throwable) {
        try {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            val stack = writer.toString()
            Log.e("CrashReporter", "Uncaught crash on thread=${thread.name}: ${throwable.message}")
            val file = File(cacheDir, "last_crash.log")
            file.writeText(
                buildString {
                    appendLine("thread=${thread.name}")
                    appendLine("message=${throwable.message}")
                    appendLine("stacktrace=")
                    append(stack)
                }
            )
        } catch (_: Exception) {
            // 保底，不阻塞崩溃流程
        }
    }
}
