package com.dogumgunu.asistanim.data

import android.content.Context
import com.dogumgunu.asistanim.domain.SettingsSnapshot

/** Ayarlar (tema, hatırlatma günleri, saat) cihazdaki SharedPreferences içinde saklanır. */
class SettingsStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)

    fun snapshot() = SettingsSnapshot(
        themeMode = sp.getInt("theme", 0),
        remind7 = sp.getBoolean("r7", false),
        remind3 = sp.getBoolean("r3", false),
        remind1 = sp.getBoolean("r1", true),
        remind0 = sp.getBoolean("r0", true),
        hour = sp.getInt("hour", 9),
        minute = sp.getInt("minute", 0)
    )

    fun save(s: SettingsSnapshot) {
        sp.edit()
            .putInt("theme", s.themeMode)
            .putBoolean("r7", s.remind7)
            .putBoolean("r3", s.remind3)
            .putBoolean("r1", s.remind1)
            .putBoolean("r0", s.remind0)
            .putInt("hour", s.hour)
            .putInt("minute", s.minute)
            .apply()
    }

    /** Bildirimlerin en son kontrol edildiği an (epoch ms). 0 = hiç kontrol edilmedi. */
    var lastCheckMillis: Long
        get() = sp.getLong("last_check", 0L)
        set(value) {
            sp.edit().putLong("last_check", value).apply()
        }

    /** Bildirim izni ilk açılışta bir kez soruldu mu? */
    var askedNotificationPermission: Boolean
        get() = sp.getBoolean("asked_notif", false)
        set(value) {
            sp.edit().putBoolean("asked_notif", value).apply()
        }
}
