package com.dogumgunu.asistanim.util

object PhoneUtil {
    /**
     * WhatsApp (wa.me) için ülke kodlu, yalnızca rakamlardan oluşan numara üretir.
     * Ülke kodu yoksa Türkiye (90) varsayılır. Geçersizse null döner.
     */
    fun toWhatsApp(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val hasPlus = trimmed.startsWith("+")
        var digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        if (!hasPlus && digits.startsWith("00")) digits = digits.removePrefix("00")
        else if (!hasPlus) {
            digits = when {
                digits.startsWith("0") && digits.length == 11 -> "90" + digits.drop(1)
                digits.length == 10 -> "90$digits"
                else -> digits
            }
        }
        return if (digits.length in 8..15) digits else null
    }
}
