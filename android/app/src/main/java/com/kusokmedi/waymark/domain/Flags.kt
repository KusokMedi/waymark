package com.kusokmedi.waymark.domain

/**
 * Returns a flag emoji for an ISO 3166-1 alpha-2 country code (regional indicator symbols).
 * Falls back to a globe emoji when the code is missing or invalid.
 */
fun flagEmoji(isoAlpha2: String?): String {
    val code = isoAlpha2?.trim()?.uppercase() ?: return "🌐"
    if (code.length != 2 || code.any { it !in 'A'..'Z' }) return "🌐"
    val first = Character.codePointAt(code, 0) - 'A'.code + 0x1F1E6
    val second = Character.codePointAt(code, 1) - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
