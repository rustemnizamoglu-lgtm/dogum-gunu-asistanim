package com.dogumgunu.asistanim.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarmı çaldığında çalışır: bildirimleri gösterir, sıradaki alarmı kurar. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderScheduler.fireDue(app)
            } finally {
                pending.finish()
            }
        }
    }
}
