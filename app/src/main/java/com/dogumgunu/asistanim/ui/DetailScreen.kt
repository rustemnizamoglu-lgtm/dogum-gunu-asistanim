@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dogumgunu.asistanim.data.Category
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import com.dogumgunu.asistanim.domain.ReminderEngine
import com.dogumgunu.asistanim.domain.SettingsSnapshot
import com.dogumgunu.asistanim.util.ShareUtil
import com.dogumgunu.asistanim.util.TurkishText
import java.time.LocalDate

@Composable
fun DetailScreen(
    person: Person,
    today: LocalDate,
    settings: SettingsSnapshot,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showGreetings by remember { mutableStateOf(false) }

    val days = BirthdayCalc.daysUntil(person, today)
    val next = BirthdayCalc.next(person, today)
    val currentAge = BirthdayCalc.currentAge(person, today)
    val turning = BirthdayCalc.turningAge(person, today)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.fullName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Düzenle") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(person, 120.dp)
            Text(
                person.fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "${Category.label(person.category)} • " +
                    TurkishText.dateText(person.day, person.month, person.year),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (days) {
                            0 -> "🎂 Bugün doğum günü!"
                            1 -> "Yarın doğum günü"
                            else -> "Doğum gününe $days gün kaldı"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Sıradaki doğum günü: ${TurkishText.dateText(next, withYear = true)} " +
                            "(${TurkishText.dayNames[next.dayOfWeek.value - 1]})",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (currentAge != null && turning != null) {
                        Text(
                            "Şu an $currentAge yaşında • ${if (days == 0) "bugün" else "sonraki doğum gününde"} " +
                                "$turning yaşına ${if (days == 0) "giriyor" else "girecek"}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            "Doğum yılı kayıtlı değil, yaş hesaplanamıyor.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (person.month == 2 && person.day == 29) {
                        Text(
                            "Artık olmayan yıllarda ${if (person.feb29Mode == Person.FEB29_MARCH1) "1 Mart" else "28 Şubat"} günü hatırlatılır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Button(
                onClick = { showGreetings = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("🎉 Doğum günü mesajı gönder") }

            if (person.phone.isNotBlank()) {
                OutlinedButton(
                    onClick = { ShareUtil.dial(ctx, person.phone) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ara: ${person.phone}")
                }
            }

            if (person.note.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Not", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(person.note)
                    }
                }
            }

            val specs = ReminderEngine.specsFor(person, settings).sortedByDescending { it.daysBefore }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Hatırlatmalar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (specs.isEmpty()) {
                        Text("Bu kişi için hatırlatma kapalı.")
                    } else {
                        specs.forEach { s ->
                            val whenText = if (s.daysBefore == 0) "Doğum günü sabahı" else "${s.daysBefore} gün önce"
                            Text("• $whenText, %02d:%02d".format(s.hour, s.minute))
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Silinsin mi?") },
            text = { Text("${person.fullName} kaydı kalıcı olarak silinecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } }
        )
    }

    if (showGreetings) {
        GreetingDialog(
            person = person,
            newAge = turning,
            onDismiss = { showGreetings = false },
            onWhatsApp = { text -> showGreetings = false; ShareUtil.whatsApp(ctx, person.phone, text) },
            onShare = { text -> showGreetings = false; ShareUtil.share(ctx, text) }
        )
    }
}

@Composable
private fun GreetingDialog(
    person: Person,
    newAge: Int?,
    onDismiss: () -> Unit,
    onWhatsApp: (String) -> Unit,
    onShare: (String) -> Unit
) {
    val templates = remember(person.id, person.firstName, newAge) { TurkishText.greetings(person, newAge) }
    var message by remember { mutableStateOf(templates.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kutlama mesajı") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Hazır mesajlardan birine dokun, istersen düzenle:",
                    style = MaterialTheme.typography.bodyMedium
                )
                templates.forEach { t ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { message = t },
                        colors = CardDefaults.cardColors(
                            containerColor = if (message == t) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) { Text(t, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium) }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Gönderilecek mesaj") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (PersonHasNoPhone(person)) {
                    Text(
                        "Telefon numarası kayıtlı değil; WhatsApp açılınca kişiyi sen seçeceksin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onShare(message) }) { Text("Başka uygulamayla paylaş") }
            }
        },
        confirmButton = {
            Button(onClick = { onWhatsApp(message) }, enabled = message.isNotBlank()) { Text("WhatsApp") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

private fun PersonHasNoPhone(person: Person) = person.phone.isBlank()
