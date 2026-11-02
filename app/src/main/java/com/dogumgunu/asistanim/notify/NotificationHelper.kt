package com.dogumgunu.asistanim.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dogumgunu.asistanim.MainActivity
import com.dogumgunu.asistanim.R
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.Reminder
import com.dogumgunu.asistanim.util.TurkishText

object NotificationHelper {
    const val CHANNEL_ID = "dogum_gunleri"
    const val EXTRA_PERSON_ID = "kisi_id"
    private const val TITLE = "Doğum Günü Asistanım"

    fun createChannel(ctx: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Doğum günü hatırlatmaları",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Yaklaşan doğum günlerini hatırlatır."
            enableVibration(true)
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Bildirim gösterilebilir mi? (Android 13+ izni ve sistem ayarı) */
    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    /** Bildirim metni. Örn: "🎂 Bugün Ayşe Yılmaz'ın doğum günü! Kutlamayı unutma." */
    fun messageFor(person: Person, daysBefore: Int, birthday: java.time.LocalDate): String {
        val who = TurkishText.genitive(person.fullName)
        val age = person.year?.let { birthday.year - it }
        return when (daysBefore) {
            0 -> {
                val extra = if (age != null && age > 0) " $age yaşına giriyor." else ""
                "🎂 Bugün $who doğum günü! Kutlamayı unutma.$extra"
            }
            1 -> "🎈 Yarın $who doğum günü. Hazırlıklı ol!"
            else -> "📅 $who doğum gününe $daysBefore gün kaldı (${TurkishText.dateText(birthday)})."
        }
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, reminder: Reminder) {
        if (!canNotify(ctx)) return
        val text = messageFor(reminder.person, reminder.daysBefore, reminder.birthday)
        post(
            ctx,
            id = (reminder.person.id * 100 + reminder.daysBefore).toInt(),
            personId = reminder.person.id,
            text = text
        )
    }

    /** Ayarlar ekranındaki "deneme bildirimi" düğmesi için. Gerçek bir kişiye bağlı değildir. */
    @SuppressLint("MissingPermission")
    fun showTest(ctx: Context): Boolean {
        if (!canNotify(ctx)) return false
        post(ctx, id = 999_999, personId = -1L, text = "🎂 Deneme bildirimi: bildirimler bu telefonda çalışıyor.")
        return true
    }

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, id: Int, personId: Long, text: String) {
        val tap = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (personId >= 0) putExtra(EXTRA_PERSON_ID, personId)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            id,
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_cake)
            .setContentTitle(TITLE)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, notification)
        } catch (e: SecurityException) {
            // İzin sonradan geri alındıysa sessizce geç.
        }
    }
}
