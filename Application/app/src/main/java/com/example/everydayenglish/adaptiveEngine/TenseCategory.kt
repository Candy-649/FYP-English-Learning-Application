package com.example.everydayenglish.adaptiveEngine

enum class TenseCategory(val displayName: String) {
    FUTURE_SIMPLE("Future Simple"),
    PASSIVE_STATE("Passive / State"),
    PAST_CONTINUOUS("Past Continuous"),
    PAST_PERFECT("Past Perfect"),
    PAST_SIMPLE("Past Simple"),
    PERFECT_CONTINUOUS("Perfect Continuous"),
    PRESENT_CONTINUOUS("Present Continuous"),
    PRESENT_PERFECT("Present Perfect"),
    PRESENT_SIMPLE("Present Simple"),
    SITUATIONAL_FRAGMENTS("Situational Fragments"),
    SOCIAL_INTERPERSONAL("Social / Interpersonal");

    companion object {
        fun fromTenseString(tense: String?): TenseCategory? =
            entries.find {
                it.name.equals(tense?.replace(" ", "_"), ignoreCase = true)
            }
    }
}