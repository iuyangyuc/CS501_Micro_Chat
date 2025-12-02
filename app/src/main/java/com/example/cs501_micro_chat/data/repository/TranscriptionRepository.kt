package com.example.cs501_micro_chat.data.repository

import android.util.Log
import com.example.cs501_micro_chat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor() {

    private val endpoint: String = "${BuildConfig.TRANSCRIPTION_BASE_URL.trimEnd('/')}/transcribe"
    private val tag = "TranscriptionRepo"

    suspend fun transcribe(mediaUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(tag, "transcribe start url=$mediaUrl")
            val audioBytes = downloadBytes(mediaUrl)
            if (audioBytes.isEmpty()) throw IOException("Empty audio content")

            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            try {
                DataOutputStream(connection.outputStream).use { output ->
                    fun writeLine(value: String) {
                        output.writeBytes(value)
                        output.writeBytes("\r\n")
                    }

                    writeLine("--$boundary")
                    writeLine("Content-Disposition: form-data; name=\"file\"; filename=\"voice.mp3\"")
                    writeLine("Content-Type: audio/mpeg")
                    writeLine("")
                    output.write(audioBytes)
                    output.writeBytes("\r\n")

                    writeLine("--$boundary")
                    writeLine("Content-Disposition: form-data; name=\"model\"")
                    writeLine("")
                    writeLine("gpt-4o-mini-transcribe")

                    writeLine("--$boundary--")
                }

                val code = connection.responseCode
                val responseText = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                if (code !in 200..299) {
                    throw IOException("Transcription failed ($code): $responseText")
                }

                val json = JSONObject(responseText)
                json.optString("text").takeIf { it.isNotBlank() }
                    ?: throw IOException("Transcription service returned empty text")
            } finally {
                connection.disconnect()
            }
        }
    }.onFailure { error ->
        Log.e(tag, "transcribe failed url=$mediaUrl reason=${error.message}", error)
    }

    private fun downloadBytes(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        return try {
            connection.inputStream.use { input -> input.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}
