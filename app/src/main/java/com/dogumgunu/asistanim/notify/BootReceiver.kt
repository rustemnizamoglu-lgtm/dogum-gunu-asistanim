package com.dogumgunu.asistanim.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Telefon yeniden başladığında, uygulama güncellendiğinde, saat/saat dilimi değiştiğinde
 * ya da tam zamanlı alarm izni değiştiğinde hatırlatma zincirini yeniden kurar.
 * Kapalıyken kaçan hatırlatmalar da (en fazla 2 gün) bu sırada telafi edilir.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val handled = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
        if (action !in handled) return

        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                NotificationHelper.createChannel(app)
                ReminderScheduler.ensureWorker(app)
                ReminderScheduler.fireDue(app)
            } finally {
                pending.finish()
            }
        }
    }
}
