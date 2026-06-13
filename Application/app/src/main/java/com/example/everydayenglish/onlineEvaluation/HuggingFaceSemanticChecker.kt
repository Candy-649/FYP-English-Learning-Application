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

/**
 * 使用 Hugging Face Inference API 计算语义相似度。
 *
 * 模型：sentence-transformers/all-MiniLM-L6-v2
 * 原理：将 userAnswer 和每个 referenceAnswer 分别转成 embedding，
 *       由模型在服务端直接计算余弦相似度并返回分数数组。
 *       取最高分作为最终得分 —— 纯数学运算，与 prompt 无关。
 *
 * API 文档：https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2
 *
 * 请求格式：
 *   POST https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2
 *   Authorization: Bearer <HF_TOKEN>
 *   Body: {
 *     "inputs": {
 *       "source_sentence": "<userAnswer>",
 *       "sentences": ["<ref1>", "<ref2>", ...]
 *     }
 *   }
 *
 * 响应格式：[0.85, 0.72]  // 每个参考答案对应一个余弦相似度分数
 */
class HuggingFaceSemanticChecker(
    private val apiToken: String,
    private val modelId: String = "sentence-transformers/all-MiniLM-L6-v2"
) : SemanticChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private val apiUrl get() = "https://api-inference.huggingface.co/models/$modelId"

    override suspend fun evaluate(
        userAnswer: String,
        referenceAnswers: List<String>
    ): SemanticResult = withContext(Dispatchers.IO) {
        val score = callHuggingFaceApi(userAnswer, referenceAnswers)
        SemanticResult(
            score     = score,
            isCorrect = score >= SemanticResult.CORRECT_THRESHOLD
        )
    }

    private fun callHuggingFaceApi(
        userAnswer: String,
        referenceAnswers: List<String>
    ): Double {
        val body = JSONObject().apply {
            put("inputs", JSONObject().apply {
                put("source_sentence", userAnswer)
                put("sentences", JSONArray(referenceAnswers))
            })
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_TYPE))
            .build()

        val responseText = client.newCall(request).execute().use { resp ->
            // 503 = 模型正在加载（冷启动），直接抛出让上层重试或降级
            check(resp.isSuccessful) {
                "HuggingFace API error ${resp.code}: ${resp.body?.string()}"
            }
            resp.body!!.string()
        }

        return parseScores(responseText)
    }


    private fun parseScores(raw: String): Double {
        val array = JSONArray(raw)
        var maxScore = 0.0
        for (i in 0 until array.length()) {
            val score = array.getDouble(i)
            if (score > maxScore) maxScore = score
        }
        return maxScore.coerceIn(0.0, 1.0)
    }
}