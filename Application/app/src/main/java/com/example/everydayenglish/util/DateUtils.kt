package com.example.everydayenglish.util

import java.util.Calendar

fun isNewDay(lastMs: Long): Boolean {
    if (lastMs == 0L) return true
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return lastMs < cal.timeInMillis
}
