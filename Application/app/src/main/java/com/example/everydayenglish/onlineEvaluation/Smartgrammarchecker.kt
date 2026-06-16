package com.example.everydayenglish.onlineEvaluation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.everydayenglish.grammarChecker.GrammarChecker
import com.example.everydayenglish.grammarChecker.GrammarResult
import com.example.everydayenglish.grammarChecker.OnnxGrammarChecker

class SmartGrammarChecker(
    context: Context
) : GrammarChecker {

    private val appContext      = context.applicationContext
    private val onlineChecker   = LanguageToolGrammarChecker()
    private val offlineChecker  = OnnxGrammarChecker(appContext)

    override suspend fun check(text: String): GrammarResult =
        if (isNetworkAvailable()) {
            try {
                onlineChecker.check(text).takeIf { it.summary != "Grammar check unavailable." }
                    ?: offlineChecker.check(text)
            } catch (e: Exception) {
                offlineChecker.check(text)
            }
        } else {
            offlineChecker.check(text)
        }

    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}