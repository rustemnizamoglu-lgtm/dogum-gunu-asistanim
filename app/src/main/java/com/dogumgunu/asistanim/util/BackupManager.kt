package com.dogumgunu.asistanim.util

import android.content.Context
import android.util.Base64
import com.dogumgunu.asistanim.data.Category
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/** Yedek dosyası: fotoğraflar dahil tek bir JSON dosyası. */
object BackupManager {
    private const val APP_ID = "DogumGunuAsistanim"

    fun toJson(ctx: Context, people: List<Person>): String {
        val arr = JSONArray()
        for (p in people) {
            val o = JSONObject()
            o.put("firstName", p.firstName)
            o.put("lastName", p.lastName)
            o.put("day", p.day)
            o.put("month", p.month)
            o.put("year", p.year ?: JSONObject.NULL)
            o.put("category", p.category)
            o.put("phone", p.phone)
            o.put("note", p.note)
            o.put("feb29Mode", p.feb29Mode)
            o.put("remindersEnabled", p.remindersEnabled)
            o.put("customEnabled", p.customEnabled)
            o.put("customDaysBefore", p.customDaysBefore)
            o.put("customHour", p.customHour)
            o.put("customMinute", p.customMinute)
            val f = PhotoStore.file(ctx, p.photo)
            if (f != null && f.exists()) {
                o.put("photo", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            } else {
                o.put("photo", JSONObject.NULL)
            }
            arr.put(o)
        }
        val root = JSONObject()
        root.put("app", APP_ID)
        root.put("version", 1)
        root.put("exportedAt", LocalDateTime.now().toString())
        root.put("people", arr)
        return root.toString(2)
    }

    /** Geçersiz dosyada IllegalArgumentException fırlatır (mesajı kullanıcıya gösterilebilir). */
    fun fromJson(ctx: Context, text: String): List<Person> {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Bu dosya geçerli bir yedek dosyası değil.")
        }
        if (root.optString("app") != APP_ID) {
            throw IllegalArgumentException("Bu dosya Doğum Günü Asistanım yedeği değil.")
        }
        val arr = root.optJSONArray("people") ?: return emptyList()
        val list = ArrayList<Person>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val first = o.optString("firstName", "").trim()
            val day = o.optInt("day", 0)
            val month = o.optInt("month", 0)
            val year = if (o.isNull("year")) null else o.optInt("year", 0).takeIf { it > 0 }
            if (first.isEmpty() || !BirthdayCalc.isValidDate(day, month, year)) continue

            val photoName = if (o.isNull("photo")) null else {
                try {
                    PhotoStore.saveBytes(ctx, Base64.decode(o.getString("photo"), Base64.DEFAULT))
                } catch (e: Exception) {
                    null
                }
            }
            val category = o.optString("category", Category.OTHER)
            list.add(
                Person(
                    firstName = first,
                    lastName = o.optString("lastName", "").trim(),
                    day = day,
                    month = month,
                    year = year,
                    category = if (category in Category.all) category else Category.OTHER,
                    phone = o.optString("phone", ""),
                    note = o.optString("note", ""),
                    photo = photoName,
                    feb29Mode = o.optInt("feb29Mode", Person.FEB29_FEB28),
                    remindersEnabled = o.optBoolean("remindersEnabled", true),
                    customEnabled = o.optBoolean("customEnabled", false),
                    customDaysBefore = o.optInt("customDaysBefore", 0).coerceIn(0, 365),
                    customHour = o.optInt("customHour", 9).coerceIn(0, 23),
                    customMinute = o.optInt("customMinute", 0).coerceIn(0, 59)
                )
            )
        }
        return list
    }
}
