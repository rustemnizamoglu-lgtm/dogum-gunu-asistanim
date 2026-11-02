@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dogumgunu.asistanim.data.Category
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import com.dogumgunu.asistanim.util.TurkishText
import java.time.LocalDate

@Composable
fun HomeTab(
    people: List<Person>,
    loaded: Boolean,
    today: LocalDate,
    query: String,
    onQuery: (String) -> Unit,
    category: String?,
    onCategory: (String?) -> Unit,
    onOpen: (Person) -> Unit,
    onImportContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!loaded) {
        Column(modifier.fillMaxSize()) {}
        return
    }
    if (people.isEmpty()) {
        EmptyState(onImportContacts, modifier)
        return
    }

    val filtered = remember(people, query, category) {
        val q = TurkishText.fold(query.trim())
        people.filter { p ->
            (category == null || p.category == category) &&
                (q.isEmpty() || TurkishText.fold(p.fullName).contains(q))
        }
    }
    val byNext = remember(filtered, today) { filtered.sortedBy { BirthdayCalc.daysUntil(it, today) } }
    val todayList = remember(byNext, today) { byNext.filter { BirthdayCalc.daysUntil(it, today) == 0 } }
    val weekList = remember(byNext, today) {
        byNext.filter { BirthdayCalc.daysUntil(it, today) in 1..7 }
    }
    val monthList = remember(filtered, today) {
        filtered.filter { it.month == today.monthValue }.sortedBy { it.day }
    }
    val nextOverall = remember(people, today) {
        people.minByOrNull { BirthdayCalc.daysUntil(it, today) }
    }
    val searching = query.isNotBlank()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("İsimle ara") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Aramayı temizle")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        item(key = "chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = category == null,
                    onClick = { onCategory(null) },
                    label = { Text("Hepsi") }
                )
                Category.all.forEach { key ->
                    FilterChip(
                        selected = category == key,
                        onClick = { onCategory(if (category == key) null else key) },
                        label = { Text(Category.label(key)) }
                    )
                }
            }
        }

        if (searching) {
            item(key = "search_title") { SectionTitle("Arama sonuçları (${byNext.size})") }
            if (byNext.isEmpty()) {
                item(key = "search_empty") {
                    Text(
                        "Bu isimle kayıt bulunamadı.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(byNext, key = { "q_${it.id}" }) { p -> PersonRow(p, today) { onOpen(p) } }
        } else {
            if (nextOverall != null && category == null) {
                item(key = "next_card") { NextCard(nextOverall, today) { onOpen(nextOverall) } }
            }
            section("t", "🎉 Bugün doğum günü olanlar", todayList, today, onOpen)
            section("w", "📅 Önümüzdeki 7 gün", weekList, today, onOpen)
            section("m", "🗓️ ${TurkishText.months[today.monthValue - 1]} ayında doğanlar", monthList, today, onOpen)
            section("a", "👥 Tüm kayıtlı kişiler", byNext, today, onOpen)
            if (byNext.isEmpty()) {
                item(key = "empty_filter") {
                    Text(
                        "Bu kategoride kayıtlı kişi yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun LazyListScope.section(
    prefix: String,
    title: String,
    list: List<Person>,
    today: LocalDate,
    onOpen: (Person) -> Unit
) {
    if (list.isEmpty()) return
    item(key = "${prefix}_header") { SectionTitle("$title (${list.size})") }
    items(list, key = { "${prefix}_${it.id}" }) { p -> PersonRow(p, today) { onOpen(p) } }
}

@Composable
private fun NextCard(person: Person, today: LocalDate, onClick: () -> Unit) {
    val days = BirthdayCalc.daysUntil(person, today)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(person, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Sıradaki doğum günü",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    person.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = when (days) {
                        0 -> "Bugün! 🎂"
                        1 -> "Yarın"
                        else -> "$days gün kaldı"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onImportContacts: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎂", fontSize = 64.sp)
        Spacer(Modifier.padding(8.dp))
        Text(
            "Henüz kimse eklenmedi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.padding(4.dp))
        Text(
            "Sağ alttaki + düğmesine dokunarak ilk doğum gününü ekle ya da telefon rehberinden aktar.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.padding(12.dp))
        Button(onClick = onImportContacts) { Text("Rehberden içe aktar") }
    }
}
