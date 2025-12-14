/**
 * MicroChatApplication.kt
 *
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
        // Firebase auto-initializes, but we can do additional configuration here
        FirebaseApp.initializeApp(this)

        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logUncaughtCrash(thread, throwable)
            // Pass to system default handler (crash visible to user)
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
            // Fallback, don't block crash flow
        }
    }
}
