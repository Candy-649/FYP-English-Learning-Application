package com.example.everydayenglish.onlineEvaluation

import com.example.everydayenglish.data.entity.EvaluationResult
import com.example.everydayenglish.data.entity.GrammarError
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
You are an English grammar teacher evaluating a student's fill-in-the-blank answer.

You will receive:
- The student's answer
- One or more reference answers
- A grammar check result
- A semantic similarity score (0.0–1.0), or "unavailable" if the scoring service is offline
- The grammar point being tested (e.g. "Past Simple", "Present Perfect")

STEP 1 — Judge correctness.
Set isCorrect to true if the student's answer is mostly correct in meaning with at most minor issues.
Set isCorrect to false if there are significant grammar errors or the meaning is far from the reference.

STEP 2 — Identify errors.
Produce an "errors" array. Each object must have:
  "type"        : one of TENSE | MORPHOLOGY | SPELLING | LEXICAL | ADDITION | WORD_ORDER | OMISSION
  "errorPhrase" : the exact verbatim substring from the student's answer that is wrong
                  (required for all types EXCEPT OMISSION)
  "afterPhrase" : the exact verbatim word in the student's answer that immediately precedes
                  the missing word (required for OMISSION only, omit for all others)
  "correction"  : the correct replacement string (omit this field entirely for ADDITION errors)
  "ruleHint"    : one concise English sentence explaining the relevant grammar rule

Error type guide:
  TENSE      — wrong verb tense form (e.g. go → went in a past simple context)
  MORPHOLOGY — wrong word form other than tense (e.g. childs → children, more fast → faster)
  SPELLING   — misspelled word (e.g. recieve → receive)
  LEXICAL    — wrong word choice; the word itself is correctly spelled
  ADDITION   — a word is present that should not be there (no "correction" field)
  WORD_ORDER — elements are in the wrong order
  OMISSION   — a required word is completely missing from the answer

CRITICAL RULES:
- "errorPhrase" and "afterPhrase" MUST be copied character-for-character from the student's answer.
- When isCorrect is true and the answer is essentially correct, errors MUST be an empty array [].
- Do NOT reproduce the full reference answer verbatim in any field.
- The "correction" field must be absent entirely for ADDITION errors (not null, not empty — absent).

STEP 3 — Write encouragement.
One short, natural English sentence. Be specific and positive for correct answers. Be warm and forward-looking for incorrect ones.

Respond ONLY with valid JSON — no markdown code fences, no preamble, nothing else:
{"isCorrect": true, "encouragement": "...", "errors": []}
""".trimIndent()

    override suspend fun generate(
        userAnswer      : String,
        referenceAnswers: List<String>,
        grammarSummary  : String?,
        semanticScore   : Double?,
        tenseCategory   : String?
    ): EvaluationResult = withContext(Dispatchers.IO) {
        callDeepSeekApi(userAnswer, referenceAnswers, grammarSummary, semanticScore, tenseCategory)
    }

    private fun callDeepSeekApi(
        userAnswer      : String,
        referenceAnswers: List<String>,
        grammarSummary  : String?,
        semanticScore   : Double?,
        tenseCategory   : String?
    ): EvaluationResult {
        val refBlock = referenceAnswers
            .mapIndexed { i, ref -> "${i + 1}. $ref" }
            .joinToString("\n")

        val meaningLevel = when {
            semanticScore == null  -> "Semantic similarity score is unavailable; evaluate based on grammar and reference answers only."
            semanticScore >= 0.90  -> "The meaning is essentially correct."
            semanticScore >= 0.75  -> "The meaning is mostly correct but slightly off."
            semanticScore >= 0.50  -> "The meaning is partially correct."
            else                   -> "The meaning is quite different from the expected answer."
        }

        val userContent = """
Student's answer: "$userAnswer"

Reference answer(s):
$refBlock

Grammar check: ${grammarSummary ?: "Not available."}
Meaning assessment: $meaningLevel
Grammar point being tested: ${tenseCategory ?: "Not specified."}
        """.trimIndent()

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("max_tokens", 350)
            put("temperature", 0.3)
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

        val content = JSONObject(responseText)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        val json      = JSONObject(content)
        val errorsArr = json.getJSONArray("errors")
        val errors    = (0 until errorsArr.length()).map { i ->
            val e = errorsArr.getJSONObject(i)
            GrammarError(
                type = e.getString("type"),
                errorPhrase = e.optString("errorPhrase").takeIf { it.isNotEmpty() },
                afterPhrase = e.optString("afterPhrase").takeIf { it.isNotEmpty() },
                correction = e.optString("correction").takeIf { it.isNotEmpty() },
                ruleHint = e.getString("ruleHint")
            )
        }

        return EvaluationResult(
            isCorrect     = json.getBoolean("isCorrect"),
            encouragement = json.getString("encouragement"),
            errors        = errors
        )
    }
}
