package com.example.everydayenglish.grammarChecker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object ModelDownloader {
    private const val MODEL_FILE = "grammar_model.onnx"
    private const val MODEL_URL = "https://firebasestorage.googleapis.com/v0/b/everyday-english-636b9.firebasestorage.app/o/grammar_model.onnx?alt=media&token=2d41c947-ad8c-474f-8bb5-a1c77ccc81df"

    fun isModelReady(context: Context): Boolean =
        File(context.filesDir, MODEL_FILE).exists()

    suspend fun ensureModel(context: Context): Boolean = withContext(Dispatchers.IO) {
        val dest = File(context.filesDir, MODEL_FILE)
        if (dest.exists()) return@withContext true
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(Request.Builder().url(MODEL_URL).build()).execute()
            if (!response.isSuccessful) return@withContext false
            val temp = File(context.filesDir, "$MODEL_FILE.tmp")
            response.body?.byteStream()?.use { it.copyTo(temp.outputStream()) } ?: return@withContext false
            temp.renameTo(dest)
            true
        } catch (e: Exception) {
            Log.e("ModelDownloader", "Download failed", e)
            false
        }
    }
}