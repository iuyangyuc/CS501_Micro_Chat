package com.example.cs501_micro_chat.services

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseStorageServiceTest {

    private lateinit var service: FirebaseStorageService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        service = FirebaseStorageService()
    }

    @Test
    fun addModifyDeleteFile_roundTrip() = runBlocking {
        val initialPayload = "Hello Firebase Storage!"
        val updatedPayload = "Updated Firebase Storage!"
        val remotePath = "instrumentation/${System.currentTimeMillis()}.txt"

        val sourceFile = File.createTempFile("storage-add", ".txt").apply {
            writeText(initialPayload)
        }
        val updatedFile = File.createTempFile("storage-modify", ".txt").apply {
            writeText(updatedPayload)
        }

        val firstDownload = File.createTempFile("storage-download-initial", ".txt")
        val secondDownload = File.createTempFile("storage-download-updated", ".txt")
        val finalDownload = File.createTempFile("storage-download-missing", ".txt")

        try {
            val addUri = service.addFile(remotePath, Uri.fromFile(sourceFile))
            assertTrue(addUri.toString().isNotBlank())

            service.downloadFile(remotePath, firstDownload)
            assertEquals(initialPayload, firstDownload.readText())

            val modifyUri = service.modifyFile(remotePath, Uri.fromFile(updatedFile))
            assertTrue(modifyUri.toString().isNotBlank())

            service.downloadFile(remotePath, secondDownload)
            assertEquals(updatedPayload, secondDownload.readText())

            service.deleteFile(remotePath)

            val downloadResult = runCatching { service.downloadFile(remotePath, finalDownload) }
            assertTrue(downloadResult.isFailure)
        } finally {
            sourceFile.delete()
            updatedFile.delete()
            firstDownload.delete()
            secondDownload.delete()
            finalDownload.delete()
        }
    }
}
