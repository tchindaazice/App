package com.example.utils

fun maskSensitive(text: String?, keepStart: Int = 6, keepEnd: Int = 4): String {
    if (text.isNullOrBlank()) return "••••••••"
    if (text.length <= keepStart + keepEnd) return "••••"
    val start = text.take(keepStart)
    val end = text.takeLast(keepEnd)
    return "$start••••$end"
}
