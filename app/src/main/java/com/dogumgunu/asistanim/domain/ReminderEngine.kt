package com.dogumgunu.asistanim.domain

import com.dogumgunu.asistanim.data.Person
import java.time.LocalDate
import java.time.LocalDateTime

/** Genel ayarların bildirimle ilgili kısmı. */
data class SettingsSnapshot(
    /** 0 = sistem, 1 = açık, 2 = koyu */
    val themeMode: Int = 0,
    val remind7: Boolean = false,
    val remind3: Boolean = false,
    val remind1: Boolean = true,
    val remind0: Boolean = true,
    val hour: Int = 9,
    val minute: Int = 0
)

data class ReminderSpec(val daysBefore: Int, val hour: Int, val minute: Int)

data class Reminder(
    val person: Person,
    val daysBefore: Int,
    val birthday: LocalDate,
    val triggerAt: LocalDateTime
)

/**
 * Hangi bildirimin ne zaman çalacağını hesaplar. Tamamen saf (pure) fonksiyonlardır;
 * alarm kurma ve bildirim gösterme işleri notify paketindedir.
 */
object ReminderEngine {

    fun specsFor(p: Person, s: SettingsSnapshot): List<ReminderSpec> {
        if (!p.remindersEnabled) return emptyList()
        val list = mutableListOf<ReminderSpec>()
        if (s.remind7) list.add(ReminderSpec(7, s.hour, s.minute))
        if (s.remind3) list.add(ReminderSpec(3, s.hour, s.minute))
        if (s.remind1) list.add(ReminderSpec(1, s.hour, s.minute))
        if (s.remind0) list.add(ReminderSpec(0, s.hour, s.minute))
        if (p.customEnabled) {
            list.add(ReminderSpec(p.customDaysBefore.coerceIn(0, 365), p.customHour, p.customMinute))
        }
        return list.distinct()
    }

    /** (after, upTo] aralığına düşen tüm hatırlatmalar, zamana göre sıralı. */
    fun between(
        people: List<Person>,
        s: SettingsSnapshot,
        afterExclusive: LocalDateTime,
        upToInclusive: LocalDateTime
    ): List<Reminder> {
        if (!upToInclusive.isAfter(afterExclusive)) return emptyList()
        val result = mutableListOf<Reminder>()
        for (p in people) {
            val specs = specsFor(p, s)
            if (specs.isEmpty()) continue
            for (year in afterExclusive.year..(upToInclusive.year + 1)) {
                val birthday = BirthdayCalc.occurrence(p, year)
                for (spec in specs) {
                    val trigger = birthday.minusDays(spec.daysBefore.toLong())
                        .atTime(spec.hour, spec.minute)
                    if (trigger.isAfter(afterExclusive) && !trigger.isAfter(upToInclusive)) {
                        result.add(Reminder(p, spec.daysBefore, birthday, trigger))
                    }
                }
            }
        }
        return result
            .distinctBy { Triple(it.person.id, it.birthday, it.triggerAt) }
            .sortedBy { it.triggerAt }
    }

    /** [after] anından sonraki ilk hatırlatma zamanı (yoksa null). */
    fun next(people: List<Person>, s: SettingsSnapshot, after: LocalDateTime): LocalDateTime? =
        between(people, s, after, after.plusDays(400)).firstOrNull()?.triggerAt
}
