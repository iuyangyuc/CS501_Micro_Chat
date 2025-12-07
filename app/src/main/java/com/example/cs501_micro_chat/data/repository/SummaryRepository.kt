package com.example.cs501_micro_chat.data.repository

import android.util.Log
import com.example.cs501_micro_chat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor() {

    private val endpoint: String = "${BuildConfig.TRANSLATION_BASE_URL.trimEnd('/')}/summarize"
    private val tag = "SummaryRepository"

    suspend fun summarize(
        messages: List<String>,
        instructions: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (messages.isEmpty()) {
                throw IllegalArgumentException("No messages to summarize")
            }

            val payload = JSONObject().apply {
                put("messages", JSONArray().apply { messages.forEach { put(it) } })
                put("instructions", instructions)
            }.toString()

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
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
                    throw IOException("Summarization failed ($code): $responseText")
                }

                val json = JSONObject(responseText)
                val summary = json.optString("summary")
                if (summary.isNullOrBlank()) {
                    throw IOException("Summary service returned empty content")
                }
                summary
            } finally {
                connection.disconnect()
            }
        }
    }.onFailure { error ->
        Log.e(tag, "summarize failed reason=${error.message}", error)
    }
}
