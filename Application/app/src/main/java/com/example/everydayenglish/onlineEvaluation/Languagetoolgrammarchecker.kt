package com.example.everydayenglish.onlineEvaluation

import com.example.everydayenglish.grammarChecker.GrammarChecker
import com.example.everydayenglish.grammarChecker.GrammarIssue
import com.example.everydayenglish.grammarChecker.GrammarResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LanguageToolGrammarChecker : GrammarChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun check(text: String): GrammarResult = withContext(Dispatchers.IO) {
        runCatching {
            callLanguageTool(text)
        }.getOrElse {
            // 网络抖动时降级：返回"无问题"，不阻断用户流程
            GrammarResult(issues = emptyList(), summary = "Grammar check unavailable.")
        }
    }

    private fun callLanguageTool(text: String): GrammarResult {
        val body = FormBody.Builder()
            .add("text", text)
            .add("language", "en-US")
            // 关闭拼写检查，只保留语法规则（减少误报）
            .add("disabledCategories", "TYPOS")
            .build()

        val request = Request.Builder()
            .url("https://api.languagetool.org/v2/check")
            .post(body)
            .build()

        val responseText = client.newCall(request).execute().use { resp ->
            check(resp.isSuccessful) {
                "LanguageTool API error ${resp.code}: ${resp.body?.string()}"
            }
            resp.body!!.string()
        }

        return parseResponse(responseText)
    }

    private fun parseResponse(raw: String): GrammarResult {
        val root    = JSONObject(raw)
        val matches = root.getJSONArray("matches")

        if (matches.length() == 0) {
            return GrammarResult(issues = emptyList(), summary = "Grammar looks good!")
        }

        val issues = (0 until matches.length()).map { i ->
            val m            = matches.getJSONObject(i)
            val replacements = m.getJSONArray("replacements")
            val suggestions  = (0 until minOf(replacements.length(), 3))
                .map { replacements.getJSONObject(it).getString("value") }

            GrammarIssue(
                message = m.optString("message", "Grammar issue detected"),
                shortMessage = m.optString("shortMessage", "Issue"),
                offset = m.getInt("offset"),
                length = m.getInt("length"),
                replacements = suggestions
            )
        }

        // 汇总 summary：列出所有短描述，最多 3 条
        val summary = buildSummary(issues)
        return GrammarResult(issues = issues, summary = summary)
    }

    private fun buildSummary(issues: List<GrammarIssue>): String {
        val count = issues.size
        val labels = issues.take(3).map { it.shortMessage }.distinct()
        return if (count == 1) {
            "Grammar issue: ${labels.first()}."
        } else {
            "$count grammar issues found: ${labels.joinToString(", ")}."
        }
    }
}