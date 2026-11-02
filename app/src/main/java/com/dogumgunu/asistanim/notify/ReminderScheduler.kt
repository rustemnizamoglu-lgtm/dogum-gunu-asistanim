package com.dogumgunu.asistanim.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dogumgunu.asistanim.data.AppDatabase
import com.dogumgunu.asistanim.data.SettingsStore
import com.dogumgunu.asistanim.domain.ReminderEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Hatırlatma sistemi üç katmanlıdır:
 *  1) AlarmManager: bir sonraki hatırlatma anına tek bir alarm kurulur (tam zamanlı izin varsa tam,
 *     yoksa "uykuda da çalışan" yaklaşık alarm). Alarm çalınca [fireDue] bildirimleri gösterir
 *     ve bir sonraki alarmı kurar; böylece zincir her yıl kendiliğinden sürer.
 *  2) WorkManager: 6 saatte bir [fireDue] çalıştırır; alarm herhangi bir nedenle kaçarsa (pil tasarrufu,
 *     üretici kısıtları) kaçan bildirimleri telafi eder.
 *  3) BootReceiver: yeniden başlatma, güncelleme, saat/saat dilimi değişiminde zinciri yeniden kurar.
 *
 * "Son kontrol anı" (lastCheck) saklandığı için aynı bildirim iki kez gösterilmez ve kaçanlar
 * (en fazla 2 gün geriye dönük) telafi edilir.
 */
object ReminderScheduler {
    const val ACTION_ALARM = "com.dogumgunu.asistanim.ACTION_ALARM"
    private const val REQUEST_CODE = 4711
    private const val WORK_NAME = "dogum_gunu_kontrol"
    private const val MAX_CATCH_UP_DAYS = 2L

    // Alarm, WorkManager, uygulama açılışı ve düzenleme işlemleri aynı anda çalışmasın.
    private val mutex = Mutex()

    private fun alarmPendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_ALARM)
        return PendingIntent.getBroadcast(
            ctx,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** 6 saatte bir çalışan güvenlik ağı. Zaten kuruluysa dokunmaz. */
    fun ensureWorker(ctx: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(ctx.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Sıradaki hatırlatma için alarmı (yeniden) kurar. */
    suspend fun scheduleNext(ctx: Context) {
        mutex.withLock { scheduleNextLocked(ctx.applicationContext) }
    }

    /**
     * Zamanı gelmiş (en son kontrolden bu yana) tüm hatırlatmaları gösterir, sonra sıradaki alarmı kurar.
     * Birden çok yerden güvenle çağrılabilir.
     */
    suspend fun fireDue(ctx: Context) {
        mutex.withLock {
            val app = ctx.applicationContext
            val store = SettingsStore(app)
            val now = LocalDateTime.now()

            var last = if (store.lastCheckMillis == 0L) now else toLocal(store.lastCheckMillis)
            val earliest = now.minusDays(MAX_CATCH_UP_DAYS)
            if (last.isBefore(earliest)) last = earliest
            if (last.isAfter(now)) last = now // saat geriye alınmışsa

            val people = AppDatabase.get(app).personDao().getAll()
            val due = ReminderEngine.between(people, store.snapshot(), last, now)
            for (reminder in due) NotificationHelper.show(app, reminder)

            store.lastCheckMillis = System.currentTimeMillis()
            scheduleNextLocked(app)
        }
    }

    private suspend fun scheduleNextLocked(app: Context) {
        val people = AppDatabase.get(app).personDao().getAll()
        val settings = SettingsStore(app).snapshot()
        val am = app.getSystemService(AlarmManager::class.java)
        val pending = alarmPendingIntent(app)
        am.cancel(pending)

        val next = ReminderEngine.next(people, settings, LocalDateTime.now()) ?: return
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (canScheduleExact(app)) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
                return
            } catch (e: SecurityException) {
                // İzin geri alındı: aşağıdaki yaklaşık alarma düş.
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    private fun toLocal(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}
