package com.dogumgunu.asistanim.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Kişi kategorileri (veritabanında kısa anahtar olarak saklanır). */
object Category {
    const val FAMILY = "aile"
    const val FRIEND = "arkadas"
    const val WORK = "is"
    const val OTHER = "diger"

    val all = listOf(FAMILY, FRIEND, WORK, OTHER)

    fun label(key: String): String = when (key) {
        FAMILY -> "Aile"
        FRIEND -> "Arkadaş"
        WORK -> "İş"
        else -> "Diğer"
    }
}

@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String = "",
    val day: Int,
    val month: Int,
    /** Doğum yılı bilinmiyorsa null. */
    val year: Int? = null,
    val category: String = Category.OTHER,
    val phone: String = "",
    val note: String = "",
    /** Uygulamanın özel klasöründeki fotoğraf dosyasının adı. */
    val photo: String? = null,
    /** 29 Şubat doğumlular için artık olmayan yıllarda hatırlatma günü. */
    val feb29Mode: Int = FEB29_FEB28,
    val remindersEnabled: Boolean = true,
    val customEnabled: Boolean = false,
    val customDaysBefore: Int = 0,
    val customHour: Int = 9,
    val customMinute: Int = 0
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")

    companion object {
        const val FEB29_FEB28 = 0
        const val FEB29_MARCH1 = 1
    }
}
