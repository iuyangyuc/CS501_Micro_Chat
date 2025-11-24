package com.example.cs501_micro_chat.data.repository

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

    private val endpoint = "https://cs501-micro-chat-728068207217.us-east4.run.app/translate"

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String = "auto",
        instructions: String = "Sound professional"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
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
                translation
            } finally {
                connection.disconnect()
            }
        }
    }
}
