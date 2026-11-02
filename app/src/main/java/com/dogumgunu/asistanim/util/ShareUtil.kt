package com.dogumgunu.asistanim.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** WhatsApp, arama ve sistem ayar ekranlarını açan yardımcılar (ek izin gerektirmez). */
object ShareUtil {

    fun whatsApp(ctx: Context, phone: String, text: String) {
        val number = PhoneUtil.toWhatsApp(phone)
        val intent = if (number != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${Uri.encode(text)}"))
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            share(ctx, text)
        }
    }

    fun share(ctx: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            ctx.startActivity(Intent.createChooser(send, "Mesajı gönder"))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Paylaşılacak uygulama bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun dial(ctx: Context, phone: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Arama uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openNotificationSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        safeStart(ctx, intent, appDetails(ctx))
    }

    fun openBatterySettings(ctx: Context) {
        safeStart(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appDetails(ctx))
    }

    fun openExactAlarmSettings(ctx: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${ctx.packageName}")
        )
        safeStart(ctx, intent, appDetails(ctx))
    }

    private fun appDetails(ctx: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${ctx.packageName}")
    )

    private fun safeStart(ctx: Context, intent: Intent, fallback: Intent) {
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            try {
                ctx.startActivity(fallback)
            } catch (e2: Exception) {
                Toast.makeText(ctx, "Ayar ekranı açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
