package com.example.everydayenglish.data.entity

import org.json.JSONArray
import org.json.JSONObject

/**
 * A single identified grammar error in the student's answer.
 * Follows the Surface Strategy Taxonomy (Dulay, Burt & Krashen, 1982)
 * refined by BRY17 (Bryant et al., 2017).
 */
/**
 * A single identified grammar error in the student's answer.
 * Follows the Surface Strategy Taxonomy (Dulay, Burt & Krashen, 1982)
 * refined by BRY17 (Bryant et al., 2017).
 */
data class GrammarError(
    /** TENSE | MORPHOLOGY | SPELLING | LEXICAL | ADDITION | WORD_ORDER | OMISSION */
    val type        : String,
    /** Verbatim substring from user answer that is wrong (all types except OMISSION). */
    val errorPhrase : String? = null,
    /** Word immediately before the missing word, used to locate insertion point (OMISSION only). */
    val afterPhrase : String? = null,
    /** Correct replacement form; absent for ADDITION errors. */
    val correction  : String? = null,
    /** One-sentence English rule hint shown to the student. */
    val ruleHint    : String
)

data class EvaluationResult(
    val isCorrect    : Boolean,
    /** Short encouraging sentence shown to the student. */
    val encouragement: String,
    /** Structured error list; empty when the answer is correct. */
    val errors       : List<GrammarError>
) {
    /**
     * Serialise to a JSON string for storage in the existing Room
     * ExerciseRecord.feedback column (String). No schema migration needed.
     */
    fun toStorageString(): String = JSONObject().apply {
        put("isCorrect", isCorrect)
        put("encouragement", encouragement)
        val arr = JSONArray()
        errors.forEach { e ->
            arr.put(JSONObject().apply {
                put("type", e.type)
                e.errorPhrase?.let { put("errorPhrase", it) }
                e.afterPhrase?.let { put("afterPhrase", it) }
                e.correction?.let  { put("correction",  it) }
                put("ruleHint", e.ruleHint)
            })
        }
        put("errors", arr)
    }.toString()

    companion object {
        /**
         * Parse a storage string produced by [toStorageString] back into an
         * [EvaluationResult]. Returns null if the string is not valid JSON or
         * does not match the expected schema (e.g. legacy plain-text feedback).
         */
        fun fromStorageString(s: String): EvaluationResult? = try {
            val j   = JSONObject(s)
            val arr = j.getJSONArray("errors")
            val errors = (0 until arr.length()).map { i ->
                val e = arr.getJSONObject(i)
                GrammarError(
                    type        = e.getString("type"),
                    errorPhrase = e.optString("errorPhrase").takeIf { it.isNotEmpty() },
                    afterPhrase = e.optString("afterPhrase").takeIf { it.isNotEmpty() },
                    correction  = e.optString("correction").takeIf  { it.isNotEmpty() },
                    ruleHint    = e.getString("ruleHint")
                )
            }
            EvaluationResult(
                isCorrect     = j.getBoolean("isCorrect"),
                encouragement = j.getString("encouragement"),
                errors        = errors
            )
        } catch (_: Exception) { null }
    }
}