package com.dogumgunu.asistanim

import android.app.Application
import com.dogumgunu.asistanim.data.AppDatabase
import com.dogumgunu.asistanim.notify.NotificationHelper
import com.dogumgunu.asistanim.notify.ReminderScheduler
import com.dogumgunu.asistanim.util.PhotoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BirthdayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        ReminderScheduler.ensureWorker(this)
        CoroutineScope(Dispatchers.Default).launch {
            // Her açılışta zinciri kontrol et: kaçan bildirim varsa gösterir, sıradaki alarmı kurar.
            ReminderScheduler.fireDue(this@BirthdayApp)
            // Artık hiçbir kişiye bağlı olmayan eski fotoğrafları temizle.
            val keep = AppDatabase.get(this@BirthdayApp).personDao().getAll()
                .mapNotNull { it.photo }.toSet()
            PhotoStore.cleanOrphans(this@BirthdayApp, keep)
        }
    }
}
