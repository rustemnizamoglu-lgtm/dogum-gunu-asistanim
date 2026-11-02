package com.dogumgunu.asistanim.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Güvenlik ağı: periyodik olarak kaçan hatırlatmaları telafi eder ve alarmı yeniler. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReminderScheduler.fireDue(applicationContext)
        return Result.success()
    }
}
