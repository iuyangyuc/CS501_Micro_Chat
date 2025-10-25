package com.example.cs501_micro_chat.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cs501_micro_chat.ui.main.composables.MicroChatApp
import com.example.cs501_micro_chat.ui.theme.CS501_Micro_ChatTheme
import com.google.firebase.storage.FirebaseStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        uploadTestFile()

        setContent {
            CS501_Micro_ChatTheme {
                MicroChatApp()
            }
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
