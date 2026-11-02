@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dogumgunu.asistanim.domain.SettingsSnapshot
import com.dogumgunu.asistanim.notify.NotificationHelper
import com.dogumgunu.asistanim.notify.ReminderScheduler

@Composable
fun SettingsTab(
    settings: SettingsSnapshot,
    permTick: Int,
    peopleCount: Int,
    onChange: ((SettingsSnapshot) -> SettingsSnapshot) -> Unit,
    onNotificationSettings: () -> Unit,
    onBatterySettings: () -> Unit,
    onExactAlarmSettings: () -> Unit,
    onTestNotification: () -> Unit,
    onImportContacts: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    // permTick değişince (uygulamaya her dönüşte) durumlar yeniden okunur.
    val notificationsOk = remember(permTick) { NotificationHelper.canNotify(ctx) }
    val exactOk = remember(permTick) { ReminderScheduler.canScheduleExact(ctx) }
    val batteryOk = remember(permTick) { isIgnoringBatteryOptimizations(ctx) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Bildirim sağlığı ----
        item {
            Section("Bildirim durumu") {
                StatusLine(
                    ok = notificationsOk,
                    okText = "Bildirimler açık ✓",
                    badText = "Bildirimler kapalı — hatırlatma alamazsın!"
                )
                if (!notificationsOk) {
                    Button(onClick = onNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Bildirimleri aç")
                    }
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    StatusLine(
                        ok = exactOk,
                        okText = "Tam zamanlı alarm izni var ✓",
                        badText = "Tam zamanlı alarm izni yok — bildirimler birkaç dakika gecikebilir."
                    )
                    if (!exactOk) {
                        OutlinedButton(onClick = onExactAlarmSettings, modifier = Modifier.fillMaxWidth()) {
                            Text("Tam zamanlı alarma izin ver")
                        }
                    }
                }
                StatusLine(
                    ok = batteryOk,
                    okText = "Pil kısıtlaması yok ✓",
                    badText = "Pil optimizasyonu açık — bazı telefonlar bildirimi geciktirebilir."
                )
                if (!batteryOk) {
                    OutlinedButton(onClick = onBatterySettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Pil optimizasyonu ayarlarını aç")
                    }
                }
                OutlinedButton(onClick = onTestNotification, modifier = Modifier.fillMaxWidth()) {
                    Text("Deneme bildirimi gönder")
                }
            }
        }

        // ---- Hatırlatma zamanları ----
        item {
            Section("Ne zaman hatırlatılsın?") {
                SwitchRow("7 gün önce", settings.remind7) { v -> onChange { it.copy(remind7 = v) } }
                SwitchRow("3 gün önce", settings.remind3) { v -> onChange { it.copy(remind3 = v) } }
                SwitchRow("1 gün önce", settings.remind1) { v -> onChange { it.copy(remind1 = v) } }
                SwitchRow("Doğum günü sabahı", settings.remind0) { v -> onChange { it.copy(remind0 = v) } }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bildirim saati", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        TimePickerDialog(
                            ctx,
                            { _, h, m -> onChange { it.copy(hour = h, minute = m) } },
                            settings.hour,
                            settings.minute,
                            true
                        ).show()
                    }) { Text("%02d:%02d".format(settings.hour, settings.minute)) }
                }
                Text(
                    "Bir kişiye özel gün/saat için kişiyi düzenleyip \"özel hatırlatma\" ekleyebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Tema ----
        item {
            Section("Görünüm") {
                listOf(0 to "Telefonun temasını izle", 1 to "Açık tema", 2 to "Koyu tema").forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.themeMode == value,
                            onClick = { onChange { it.copy(themeMode = value) } }
                        )
                        Text(label)
                    }
                }
            }
        }

        // ---- Veri ----
        item {
            Section("Kişiler ve yedek ($peopleCount kişi kayıtlı)") {
                OutlinedButton(onClick = onImportContacts, modifier = Modifier.fillMaxWidth()) {
                    Text("Telefon rehberinden içe aktar")
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text("Yedek dosyası oluştur")
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Yedek dosyasından geri yükle")
                }
                Text(
                    "Geri yükleme mevcut kayıtlarını silmez; yalnızca eksik kişileri ekler. " +
                        "Yedek dosyasını telefon değiştirirsen kullanmak üzere Drive'a ya da e-postana kaydet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Section("Hakkında") {
                Text("Doğum Günü Asistanım 🎂  •  sürüm 1.0.0", fontWeight = FontWeight.SemiBold)
                Text(
                    "Tüm bilgilerin yalnızca bu telefonda saklanır. Uygulama internet izni istemez, reklam göstermez.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun StatusLine(ok: Boolean, okText: String, badText: String) {
    Text(
        text = if (ok) okText else badText,
        color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

private fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
    val pm = ctx.getSystemService(PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}
