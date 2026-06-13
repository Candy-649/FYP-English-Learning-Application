package com.example.everydayenglish.onlineEvaluation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class DeepSeekFeedbackGenerator(
    private val apiKey: String
) : FeedbackGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val SYSTEM_PROMPT = """
        You are a friendly English grammar teacher giving feedback to a student.
        You will receive:
        - The student's answer
        - One or more reference (correct) answers
        - A grammar check result (may be null if unavailable)
        - A semantic similarity score (0.0 = completely wrong meaning, 1.0 = correct meaning)

        Your task: Write 1–2 encouraging sentences that:
        1. Acknowledge what the student got right (if anything).
        2. Briefly explain the key issue (grammar or meaning), referencing the reference answer.
        3. Show the student how to correct or improve their answer.

        Rules:
        - Be concise and encouraging, never harsh.
        - Do NOT repeat the score number in your response.
        - Respond in plain text only. No JSON, no markdown.
    """.trimIndent()

    override suspend fun generate(
        userAnswer       : String,
        referenceAnswers : List<String>,
        grammarSummary   : String?,
        semanticScore    : Double
    ): String = withContext(Dispatchers.IO) {
        callDeepSeekApi(userAnswer, referenceAnswers, grammarSummary, semanticScore)
    }

    private fun callDeepSeekApi(
        userAnswer       : String,
        referenceAnswers : List<String>,
        grammarSummary   : String?,
        semanticScore    : Double
    ): String {
        val refBlock = referenceAnswers
            .mapIndexed { i, ref -> "${i + 1}. $ref" }
            .joinToString("\n")

        val meaningLevel = when {
            semanticScore >= 0.90 -> "The meaning is essentially correct."
            semanticScore >= 0.75 -> "The meaning is mostly correct but slightly off."
            semanticScore >= 0.50 -> "The meaning is partially correct."
            else                  -> "The meaning is quite different from the expected answer."
        }

        val userContent = """
            Student's answer: "$userAnswer"

            Reference answer(s):
            $refBlock

            Grammar check: ${grammarSummary ?: "Not available."}
            Meaning assessment: $meaningLevel

            Please provide brief, encouraging teaching feedback.
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("max_tokens", 120)
            put("temperature", 0.3)  // 教学场景略提温度，措辞更自然
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_TYPE))
            .build()

        val responseText = client.newCall(request).execute().use { resp ->
            check(resp.isSuccessful) {
                "DeepSeek API error ${resp.code}: ${resp.body?.string()}"
            }
            resp.body!!.string()
        }

        return JSONObject(responseText)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}