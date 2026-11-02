package com.dogumgunu.asistanim.util

import android.content.Context
import android.provider.ContactsContract
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Telefon rehberindeki "doğum günü" alanı olan kişileri okur (READ_CONTACTS izni gerekir). */
object ContactsImporter {

    private val fullDate = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""")
    private val noYear = Regex("""--(\d{1,2})-(\d{1,2})""")

    /** Rehberde kullanılan tarih biçimlerini (yyyy-MM-dd, --MM-dd) çözer: Triple(gün, ay, yıl?). */
    fun parseBirthday(raw: String?): Triple<Int, Int, Int?>? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        val m1 = fullDate.find(s)
        if (m1 != null && m1.range.first == 0) {
            val y = m1.groupValues[1].toInt()
            val mo = m1.groupValues[2].toInt()
            val d = m1.groupValues[3].toInt()
            // 1900'den küçük yıllar çoğu rehberde "yıl yok" anlamına gelir.
            val year = if (y < 1900) null else y
            return if (BirthdayCalc.isValidDate(d, mo, year)) Triple(d, mo, year) else null
        }
        val m2 = noYear.find(s)
        if (m2 != null && m2.range.first == 0) {
            val mo = m2.groupValues[1].toInt()
            val d = m2.groupValues[2].toInt()
            return if (BirthdayCalc.isValidDate(d, mo, null)) Triple(d, mo, null) else null
        }
        return null
    }

    /** Ad Soyad -> (ad, soyad): son sözcük soyadı kabul edilir. */
    fun splitName(display: String): Pair<String, String> {
        val parts = display.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "" to ""
            parts.size == 1 -> parts[0] to ""
            else -> parts.dropLast(1).joinToString(" ") to parts.last()
        }
    }

    data class Found(val people: List<Person>, val withoutValidDate: Int)

    suspend fun read(ctx: Context): Found = withContext(Dispatchers.IO) {
        val resolver = ctx.contentResolver

        val phones = HashMap<Long, String>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val iNum = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                if (!phones.containsKey(id)) phones[id] = c.getString(iNum).orEmpty()
            }
        }

        val result = ArrayList<Person>()
        var bad = 0
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Event.START_DATE
            ),
            "${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.CommonDataKinds.Event.TYPE}=?",
            arrayOf(
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
            ),
            null
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
            val iName = c.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
            val iDate = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE)
            while (c.moveToNext()) {
                val parsed = parseBirthday(c.getString(iDate))
                val (first, last) = splitName(c.getString(iName).orEmpty())
                if (parsed == null || first.isEmpty()) {
                    bad++
                    continue
                }
                result.add(
                    Person(
                        firstName = first,
                        lastName = last,
                        day = parsed.first,
                        month = parsed.second,
                        year = parsed.third,
                        phone = phones[c.getLong(iId)].orEmpty()
                    )
                )
            }
        }
        Found(result, bad)
    }
}
