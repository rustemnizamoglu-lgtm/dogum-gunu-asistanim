@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dogumgunu.asistanim.data.Category
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import com.dogumgunu.asistanim.util.PhotoStore
import com.dogumgunu.asistanim.util.TurkishText
import java.time.LocalDate

@Composable
fun EditScreen(
    existing: Person?,
    snackbar: SnackbarHostState,
    onSave: (Person) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    var first by rememberSaveable { mutableStateOf(existing?.firstName ?: "") }
    var last by rememberSaveable { mutableStateOf(existing?.lastName ?: "") }
    var dayText by rememberSaveable { mutableStateOf(existing?.day?.toString() ?: "") }
    var month by rememberSaveable { mutableIntStateOf(existing?.month ?: 0) }
    var yearText by rememberSaveable { mutableStateOf(existing?.year?.toString() ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Category.FRIEND) }
    var phone by rememberSaveable { mutableStateOf(existing?.phone ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var photo by rememberSaveable { mutableStateOf(existing?.photo) }
    var feb29Mode by rememberSaveable { mutableIntStateOf(existing?.feb29Mode ?: Person.FEB29_FEB28) }
    var remindersOn by rememberSaveable { mutableStateOf(existing?.remindersEnabled ?: true) }
    var customOn by rememberSaveable { mutableStateOf(existing?.customEnabled ?: false) }
    var customDaysText by rememberSaveable { mutableStateOf((existing?.customDaysBefore ?: 0).toString()) }
    var customHour by rememberSaveable { mutableIntStateOf(existing?.customHour ?: 9) }
    var customMinute by rememberSaveable { mutableIntStateOf(existing?.customMinute ?: 0) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var monthMenu by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val saved = PhotoStore.saveFromUri(ctx, uri)
            if (saved != null) photo = saved else error = "Fotoğraf okunamadı, başka bir fotoğraf deneyin."
        }
    }

    fun trySave() {
        val firstName = first.trim()
        val day = dayText.toIntOrNull()
        val year = yearText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val thisYear = LocalDate.now().year
        val customDays = customDaysText.toIntOrNull()
        error = when {
            firstName.isEmpty() -> "Lütfen adı yaz."
            day == null -> "Lütfen doğum gününün gününü yaz (1-31)."
            month == 0 -> "Lütfen doğum ayını seç."
            yearText.isNotBlank() && (year == null || year !in 1900..thisYear) ->
                "Doğum yılı 1900 ile $thisYear arasında olmalı. Bilmiyorsan boş bırak."
            !BirthdayCalc.isValidDate(day ?: 0, month, year) ->
                "Bu tarih geçerli değil. ${TurkishText.months[month - 1]} ayı için günü kontrol et."
            customOn && (customDays == null || customDays !in 0..365) ->
                "Özel hatırlatma için gün sayısı 0 ile 365 arasında olmalı."
            else -> null
        }
        if (error != null) return
        onSave(
            Person(
                id = existing?.id ?: 0L,
                firstName = firstName,
                lastName = last.trim(),
                day = day!!,
                month = month,
                year = year,
                category = category,
                phone = phone.trim(),
                note = note.trim(),
                photo = photo,
                feb29Mode = feb29Mode,
                remindersEnabled = remindersOn,
                customEnabled = customOn,
                customDaysBefore = customDays ?: 0,
                customHour = customHour,
                customMinute = customMinute
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Yeni kişi" else "Kişiyi düzenle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = { TextButton(onClick = { trySave() }) { Text("Kaydet") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fotoğraf
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = (existing ?: Person(firstName = first, day = 1, month = 1))
                    .copy(firstName = first, lastName = last, photo = photo)
                Avatar(preview, 88.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (photo == null) "Fotoğraf ekle" else "Fotoğrafı değiştir") }
                    if (photo != null) {
                        TextButton(onClick = { photo = null }) { Text("Fotoğrafı kaldır") }
                    }
                }
            }

            OutlinedTextField(
                value = first,
                onValueChange = { first = it; error = null },
                label = { Text("Ad *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = last,
                onValueChange = { last = it },
                label = { Text("Soyad") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Doğum tarihi *", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { v -> dayText = v.filter { it.isDigit() }.take(2); error = null },
                    label = { Text("Gün") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f)
                )
                Column(modifier = Modifier.weight(1.5f)) {
                    OutlinedButton(
                        onClick = { monthMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) { Text(if (month == 0) "Ay seç" else TurkishText.months[month - 1], maxLines = 1) }
                    DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                        TurkishText.months.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { month = index + 1; monthMenu = false; error = null }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { v -> yearText = v.filter { it.isDigit() }.take(4); error = null },
                    label = { Text("Yıl") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Doğum yılını bilmiyorsan Yıl kutusunu boş bırak; yine de her yıl hatırlatılır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (dayText == "29" && month == 2) {
                Text("29 Şubat — artık olmayan yıllarda hangi gün hatırlatılsın?", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = feb29Mode == Person.FEB29_FEB28, onClick = { feb29Mode = Person.FEB29_FEB28 })
                    Text("28 Şubat")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = feb29Mode == Person.FEB29_MARCH1, onClick = { feb29Mode = Person.FEB29_MARCH1 })
                    Text("1 Mart")
                }
            }

            Text("Kategori", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.all.forEach { key ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(Category.label(key)) }
                    )
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefon numarası") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not (örn. hediye fikri)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Hatırlatma", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bu kişi için hatırlat", modifier = Modifier.weight(1f))
                Switch(checked = remindersOn, onCheckedChange = { remindersOn = it })
            }
            if (remindersOn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bu kişiye özel ek hatırlatma", modifier = Modifier.weight(1f))
                    Switch(checked = customOn, onCheckedChange = { customOn = it })
                }
                if (customOn) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customDaysText,
                            onValueChange = { v -> customDaysText = v.filter { it.isDigit() }.take(3) },
                            label = { Text("Kaç gün önce") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    ctx,
                                    { _, h, m -> customHour = h; customMinute = m },
                                    customHour,
                                    customMinute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.height(56.dp)
                        ) { Text("Saat %02d:%02d".format(customHour, customMinute)) }
                    }
                    Text(
                        "0 yazarsan doğum günü sabahı hatırlatılır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { trySave() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Kaydet")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
