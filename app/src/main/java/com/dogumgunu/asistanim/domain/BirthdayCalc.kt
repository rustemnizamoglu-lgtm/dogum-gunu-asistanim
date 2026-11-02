package com.dogumgunu.asistanim.domain

import com.dogumgunu.asistanim.data.Person
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Doğum günü tarih hesapları (Android'e bağımlı değildir, birim testle denenebilir). */
object BirthdayCalc {

    /** Verilen yıldaki doğum günü. 29 Şubat, artık olmayan yılda seçilen güne kayar. */
    fun occurrence(day: Int, month: Int, feb29Mode: Int, year: Int): LocalDate {
        if (month == 2 && day == 29 && !Year.isLeap(year.toLong())) {
            return if (feb29Mode == Person.FEB29_MARCH1) LocalDate.of(year, 3, 1)
            else LocalDate.of(year, 2, 28)
        }
        return LocalDate.of(year, month, day)
    }

    fun occurrence(p: Person, year: Int): LocalDate = occurrence(p.day, p.month, p.feb29Mode, year)

    /** [from] günü dahil, bir sonraki doğum günü. */
    fun next(p: Person, from: LocalDate): LocalDate {
        val thisYear = occurrence(p, from.year)
        return if (!thisYear.isBefore(from)) thisYear else occurrence(p, from.year + 1)
    }

    fun daysUntil(p: Person, today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, next(p, today)).toInt()

    /** Bir sonraki doğum gününde dolacak yaş (yıl bilinmiyorsa null). */
    fun turningAge(p: Person, today: LocalDate): Int? {
        val y = p.year ?: return null
        return next(p, today).year - y
    }

    /** Bugün itibarıyla yaş (yıl bilinmiyorsa null). */
    fun currentAge(p: Person, today: LocalDate): Int? {
        val y = p.year ?: return null
        val age = today.year - y
        return if (occurrence(p, today.year).isAfter(today)) age - 1 else age
    }

    /** Gün/ay/yıl gerçek bir tarih mi? Yıl yoksa 29 Şubat'a izin verilir. */
    fun isValidDate(day: Int, month: Int, year: Int?): Boolean {
        if (month !in 1..12 || day < 1) return false
        val max = if (year != null) YearMonth.of(year, month).lengthOfMonth()
        else Month.of(month).maxLength()
        return day <= max
    }
}
