package com.dogumgunu.asistanim.util

import com.dogumgunu.asistanim.data.Person
import java.time.LocalDate
import java.util.Locale

/** Türkçe metin yardımcıları: ay/gün adları, ismin tamlayan eki, arama için sadeleştirme. */
object TurkishText {
    val TR: Locale = Locale.forLanguageTag("tr")

    val months = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )

    /** DayOfWeek.value - 1 ile indekslenir (Pazartesi = 0). */
    val dayNames = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
    val dayNamesShort = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    private const val VOWELS = "aeıioöuüAEIİOÖUÜ"

    fun dateText(day: Int, month: Int, year: Int? = null): String {
        val base = "$day ${months[month - 1]}"
        return if (year != null) "$base $year" else base
    }

    fun dateText(date: LocalDate, withYear: Boolean = false): String =
        dateText(date.dayOfMonth, date.monthValue, if (withYear) date.year else null)

    /** "Ayşe Yılmaz" -> "Ayşe Yılmaz'ın", "Ayşe" -> "Ayşe'nin", "Ali" -> "Ali'nin". */
    fun genitive(name: String): String {
        val n = name.trim()
        if (n.isEmpty()) return n
        val lastVowel = n.lastOrNull { it in VOWELS } ?: return "$n'in"
        val core = when (lastVowel) {
            'a', 'ı', 'A', 'I' -> "ı"
            'e', 'i', 'E', 'İ' -> "i"
            'o', 'u', 'O', 'U' -> "u"
            else -> "ü"
        }
        return if (n.last() in VOWELS) "$n'n${core}n" else "$n'${core}n"
    }

    /** Aramada büyük/küçük harf ve Türkçe karakter farkını yok sayar. */
    fun fold(s: String): String {
        val lower = s.lowercase(TR)
        val sb = StringBuilder(lower.length)
        for (c in lower) {
            sb.append(
                when (c) {
                    'ç' -> 'c'
                    'ğ' -> 'g'
                    'ı' -> 'i'
                    'ö' -> 'o'
                    'ş' -> 's'
                    'ü' -> 'u'
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** Hazır kutlama mesajları. {ad} ismin ilk adıyla değiştirilir. */
    fun greetings(person: Person, newAge: Int?): List<String> {
        val ad = person.firstName.trim().ifEmpty { "dostum" }
        val list = mutableListOf<String>()
        if (newAge != null) list.add("$newAge. yaşın kutlu olsun $ad! 🎂 Nice sağlıklı, mutlu yıllara!")
        list.add("Doğum günün kutlu olsun $ad! 🎂 Nice mutlu, sağlıklı ve huzurlu yıllara!")
        list.add("Sevgili $ad, yeni yaşında tüm dileklerinin gerçek olmasını diliyorum. İyi ki doğdun! 🎉")
        list.add("İyi ki doğdun $ad! 🎈 Seninle geçen her gün çok değerli. Nice yaşlara!")
        list.add("Doğum günün kutlu olsun $ad! Yeni yaşın sana sağlık, mutluluk ve bol kahkaha getirsin. 🥳")
        list.add("Canım $ad, doğum gününü en içten dileklerimle kutlarım. Hep mutlu ol! 💐")
        list.add("Nice mutlu yıllara $ad! Yeni yaşın hayırlı olsun. 🎁")
        return list
    }
}
