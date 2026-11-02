package com.dogumgunu.asistanim

import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import com.dogumgunu.asistanim.domain.ReminderEngine
import com.dogumgunu.asistanim.domain.SettingsSnapshot
import com.dogumgunu.asistanim.util.ContactsImporter
import com.dogumgunu.asistanim.util.PhoneUtil
import com.dogumgunu.asistanim.util.TurkishText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

private fun person(
    day: Int,
    month: Int,
    year: Int? = null,
    mode: Int = Person.FEB29_FEB28,
    id: Long = 1,
    enabled: Boolean = true
) = Person(id = id, firstName = "Ayşe", lastName = "Yılmaz", day = day, month = month, year = year,
    feb29Mode = mode, remindersEnabled = enabled)

class BirthdayCalcTest {
    @Test fun birthdayToday_isZeroDays() {
        val today = LocalDate.of(2026, 3, 15)
        assertEquals(0, BirthdayCalc.daysUntil(person(15, 3), today))
        assertEquals(today, BirthdayCalc.next(person(15, 3), today))
    }

    @Test fun passedBirthday_rollsToNextYear() {
        val today = LocalDate.of(2026, 3, 16)
        assertEquals(LocalDate.of(2027, 3, 15), BirthdayCalc.next(person(15, 3), today))
        assertEquals(364, BirthdayCalc.daysUntil(person(15, 3), today))
    }

    @Test fun feb29_inNonLeapYear_usesChosenDay() {
        val p28 = person(29, 2, mode = Person.FEB29_FEB28)
        val p1 = person(29, 2, mode = Person.FEB29_MARCH1)
        assertEquals(LocalDate.of(2027, 2, 28), BirthdayCalc.occurrence(p28, 2027))
        assertEquals(LocalDate.of(2027, 3, 1), BirthdayCalc.occurrence(p1, 2027))
        assertEquals(LocalDate.of(2028, 2, 29), BirthdayCalc.occurrence(p28, 2028))
    }

    @Test fun ages() {
        val p = person(15, 3, year = 1990)
        assertEquals(35, BirthdayCalc.currentAge(p, LocalDate.of(2026, 3, 14)))
        assertEquals(36, BirthdayCalc.currentAge(p, LocalDate.of(2026, 3, 15)))
        assertEquals(36, BirthdayCalc.turningAge(p, LocalDate.of(2026, 3, 14)))
        assertEquals(36, BirthdayCalc.turningAge(p, LocalDate.of(2026, 3, 15)))
        assertEquals(37, BirthdayCalc.turningAge(p, LocalDate.of(2026, 3, 16)))
        assertNull(BirthdayCalc.turningAge(person(15, 3), LocalDate.of(2026, 3, 14)))
    }

    @Test fun dateValidation() {
        assertTrue(BirthdayCalc.isValidDate(29, 2, null))
        assertTrue(BirthdayCalc.isValidDate(29, 2, 2000))
        assertFalse(BirthdayCalc.isValidDate(29, 2, 2001))
        assertFalse(BirthdayCalc.isValidDate(31, 4, null))
        assertFalse(BirthdayCalc.isValidDate(0, 4, null))
        assertFalse(BirthdayCalc.isValidDate(10, 13, null))
    }
}

class ReminderEngineTest {
    private val defaults = SettingsSnapshot() // 1 gün önce + doğum günü sabahı, 09:00

    @Test fun defaultSettings_twoRemindersAroundBirthday() {
        val list = ReminderEngine.between(
            listOf(person(15, 3)), defaults,
            LocalDateTime.of(2026, 3, 13, 0, 0), LocalDateTime.of(2026, 3, 16, 0, 0)
        )
        assertEquals(
            listOf(LocalDateTime.of(2026, 3, 14, 9, 0), LocalDateTime.of(2026, 3, 15, 9, 0)),
            list.map { it.triggerAt }
        )
        assertEquals(listOf(1, 0), list.map { it.daysBefore })
    }

    @Test fun windowIsExclusiveAtStartAndInclusiveAtEnd() {
        val at = LocalDateTime.of(2026, 3, 15, 9, 0)
        val p = listOf(person(15, 3))
        assertEquals(0, ReminderEngine.between(p, defaults, at, at.plusHours(1)).size)
        assertEquals(1, ReminderEngine.between(p, defaults, at.minusMinutes(1), at).size)
    }

    @Test fun nextTrigger_rollsToNextYear() {
        val next = ReminderEngine.next(listOf(person(15, 3)), defaults, LocalDateTime.of(2026, 3, 15, 9, 0))
        assertEquals(LocalDateTime.of(2027, 3, 14, 9, 0), next)
    }

    @Test fun januaryBirthday_dayBeforeIsInPreviousDecember() {
        val next = ReminderEngine.next(listOf(person(1, 1)), defaults, LocalDateTime.of(2026, 6, 1, 0, 0))
        assertEquals(LocalDateTime.of(2026, 12, 31, 9, 0), next)
    }

    @Test fun sevenAndThreeDays_whenEnabled() {
        val s = defaults.copy(remind7 = true, remind3 = true, remind1 = false, remind0 = false, hour = 8, minute = 30)
        val list = ReminderEngine.between(
            listOf(person(15, 3)), s,
            LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 3, 31, 0, 0)
        )
        assertEquals(
            listOf(LocalDateTime.of(2026, 3, 8, 8, 30), LocalDateTime.of(2026, 3, 12, 8, 30)),
            list.map { it.triggerAt }
        )
    }

    @Test fun disabledPerson_hasNoReminders() {
        val list = ReminderEngine.between(
            listOf(person(15, 3, enabled = false)), defaults,
            LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 0, 0)
        )
        assertTrue(list.isEmpty())
    }

    @Test fun customReminder_usesPersonDayAndTime() {
        val p = person(15, 3).copy(customEnabled = true, customDaysBefore = 14, customHour = 18, customMinute = 45)
        val list = ReminderEngine.between(
            listOf(p), SettingsSnapshot(remind1 = false, remind0 = false),
            LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 3, 31, 0, 0)
        )
        assertEquals(listOf(LocalDateTime.of(2026, 3, 1, 18, 45)), list.map { it.triggerAt })
    }

    @Test fun feb29_nonLeapYear_followsChosenDay() {
        val s = SettingsSnapshot(remind1 = false, remind0 = true)
        val a = ReminderEngine.next(listOf(person(29, 2, mode = Person.FEB29_FEB28)), s, LocalDateTime.of(2027, 1, 1, 0, 0))
        val b = ReminderEngine.next(listOf(person(29, 2, mode = Person.FEB29_MARCH1)), s, LocalDateTime.of(2027, 1, 1, 0, 0))
        val c = ReminderEngine.next(listOf(person(29, 2)), s, LocalDateTime.of(2027, 3, 2, 0, 0))
        assertEquals(LocalDateTime.of(2027, 2, 28, 9, 0), a)
        assertEquals(LocalDateTime.of(2027, 3, 1, 9, 0), b)
        assertEquals(LocalDateTime.of(2028, 2, 29, 9, 0), c)
    }

    @Test fun manyYears_noReminderIsMissedOrDuplicated() {
        // 10 yıl boyunca her yıl tam olarak 2 hatırlatma (1 gün önce + gün sabahı) üretilmeli.
        val list = ReminderEngine.between(
            listOf(person(10, 7)), defaults,
            LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2035, 12, 31, 23, 59)
        )
        assertEquals(20, list.size)
        assertEquals(list.map { it.triggerAt }.distinct().size, list.size)
    }
}

class TurkishTextTest {
    @Test fun genitive() {
        assertEquals("Ayşe Yılmaz'ın", TurkishText.genitive("Ayşe Yılmaz"))
        assertEquals("Ayşe'nin", TurkishText.genitive("Ayşe"))
        assertEquals("Ali'nin", TurkishText.genitive("Ali"))
        assertEquals("Mehmet'in", TurkishText.genitive("Mehmet"))
        assertEquals("Orhan Pamuk'un", TurkishText.genitive("Orhan Pamuk"))
        assertEquals("Gül'ün", TurkishText.genitive("Gül"))
        assertEquals("Ömer'in", TurkishText.genitive("Ömer"))
        assertEquals("Zeynep Su'nun", TurkishText.genitive("Zeynep Su"))
        assertEquals("Murat Kaya'nın", TurkishText.genitive("Murat Kaya"))
    }

    @Test fun foldIgnoresCaseAndTurkishLetters() {
        assertEquals(TurkishText.fold("ŞÜKRÜ IŞIK"), TurkishText.fold("sukru isik"))
        assertEquals("ayse", TurkishText.fold("Ayşe"))
    }

    @Test fun notificationTextMatchesRequestedExample() {
        val text = com.dogumgunu.asistanim.notify.NotificationHelper.messageFor(
            person(15, 3), 0, LocalDate.of(2026, 3, 15)
        )
        assertEquals("🎂 Bugün Ayşe Yılmaz'ın doğum günü! Kutlamayı unutma.", text)
    }
}

class ContactsAndPhoneTest {
    @Test fun parseBirthdayFormats() {
        assertEquals(Triple(12, 5, 1990), ContactsImporter.parseBirthday("1990-05-12"))
        assertEquals(Triple(12, 5, null), ContactsImporter.parseBirthday("--05-12"))
        assertEquals(Triple(12, 5, null), ContactsImporter.parseBirthday("1604-05-12"))
        assertEquals(Triple(12, 5, 1990), ContactsImporter.parseBirthday("1990-05-12T00:00:00Z"))
        assertNull(ContactsImporter.parseBirthday("12 Mayıs"))
        assertNull(ContactsImporter.parseBirthday("1990-02-31"))
        assertNull(ContactsImporter.parseBirthday(null))
    }

    @Test fun splitName() {
        assertEquals("Ayşe" to "Yılmaz", ContactsImporter.splitName("Ayşe Yılmaz"))
        assertEquals("Ayşe Nur" to "Yılmaz", ContactsImporter.splitName("Ayşe Nur Yılmaz"))
        assertEquals("Anne" to "", ContactsImporter.splitName("Anne"))
    }

    @Test fun whatsAppNumbers() {
        assertEquals("905321234567", PhoneUtil.toWhatsApp("0532 123 45 67"))
        assertEquals("905321234567", PhoneUtil.toWhatsApp("532 123 45 67"))
        assertEquals("905321234567", PhoneUtil.toWhatsApp("+90 532 123 45 67"))
        assertEquals("905321234567", PhoneUtil.toWhatsApp("00905321234567"))
        assertEquals("491701234567", PhoneUtil.toWhatsApp("+49 170 1234567"))
        assertNull(PhoneUtil.toWhatsApp(""))
        assertNull(PhoneUtil.toWhatsApp("abc"))
    }
}
