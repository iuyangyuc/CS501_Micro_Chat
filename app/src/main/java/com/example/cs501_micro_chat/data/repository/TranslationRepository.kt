package com.example.cs501_micro_chat.data.repository

import android.util.Log
import com.example.cs501_micro_chat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor() {

    private val endpoint: String = "${BuildConfig.TRANSLATION_BASE_URL.trimEnd('/')}/translate"
    private val tag = "TranslationRepository"

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String = "auto",
        instructions: String = "Sound professional"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(tag, "translate start target=$targetLanguage len=${text.length}")
            val payload = JSONObject().apply {
                put("text", text)
                put("targetLanguage", targetLanguage)
                put("sourceLanguage", sourceLanguage)
                put("instructions", instructions)
            }.toString()

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            try {
                connection.outputStream.use { output ->
                    output.writer(Charsets.UTF_8).use { writer ->
                        writer.write(payload)
                    }
                }

                val code = connection.responseCode
                Log.d(tag, "translate responseCode=$code")
                val responseText = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                if (code !in 200..299) {
                    throw IOException("Translation failed ($code): $responseText")
                }

                val json = JSONObject(responseText)
                val translation = json.optString("translation")
                if (translation.isNullOrBlank()) {
                    throw IOException("Translation service returned empty content")
                }
                Log.d(tag, "translate success target=$targetLanguage")
                translation
            } finally {
                connection.disconnect()
            }
        }
    }.onFailure { error ->
        Log.e(tag, "translate failed target=$targetLanguage reason=${error.message}", error)
    }
}
