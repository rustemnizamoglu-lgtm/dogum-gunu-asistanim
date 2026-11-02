@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import com.dogumgunu.asistanim.util.TurkishText
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarTab(
    people: List<Person>,
    today: LocalDate,
    onOpen: (Person) -> Unit,
    modifier: Modifier = Modifier
) {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var pickedDay by rememberSaveable { mutableIntStateOf(0) }

    val ym = remember(today, monthOffset) { YearMonth.from(today).plusMonths(monthOffset.toLong()) }
    val byDate = remember(people, ym) {
        people.groupBy { BirthdayCalc.occurrence(it, ym.year) }
            .filterKeys { it.year == ym.year && it.monthValue == ym.monthValue }
    }
    val selectedDay = when {
        pickedDay in 1..ym.lengthOfMonth() -> pickedDay
        monthOffset == 0 -> today.dayOfMonth
        else -> 0
    }
    val selectedPeople = if (selectedDay == 0) emptyList() else byDate[ym.atDay(selectedDay)].orEmpty()
    val monthPeople = remember(byDate) {
        byDate.entries.sortedBy { it.key }.flatMap { it.value }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "cal_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { monthOffset--; pickedDay = 0 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki ay")
                }
                Text(
                    "${TurkishText.months[ym.monthValue - 1]} ${ym.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { monthOffset++; pickedDay = 0 }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki ay")
                }
            }
        }
        item(key = "cal_grid") {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    TurkishText.dayNamesShort.forEach { name ->
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val lead = ym.atDay(1).dayOfWeek.value - 1
                val length = ym.lengthOfMonth()
                val weeks = (lead + length + 6) / 7
                for (w in 0 until weeks) {
                    Row(Modifier.fillMaxWidth()) {
                        for (d in 0 until 7) {
                            val dayNum = w * 7 + d - lead + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNum in 1..length) {
                                    val date = ym.atDay(dayNum)
                                    DayCell(
                                        day = dayNum,
                                        count = byDate[date]?.size ?: 0,
                                        isToday = date == today,
                                        isSelected = dayNum == selectedDay,
                                        onClick = { pickedDay = dayNum }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (selectedDay != 0) {
            item(key = "sel_title") {
                SectionTitle("$selectedDay ${TurkishText.months[ym.monthValue - 1]}")
            }
            if (selectedPeople.isEmpty()) {
                item(key = "sel_empty") {
                    Text(
                        "Bu günde kayıtlı doğum günü yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(selectedPeople, key = { "sel_${it.id}" }) { p -> PersonRow(p, today) { onOpen(p) } }
        }
        item(key = "month_title") {
            SectionTitle("${TurkishText.months[ym.monthValue - 1]} ayındaki tüm doğum günleri (${monthPeople.size})")
        }
        if (monthPeople.isEmpty()) {
            item(key = "month_empty") {
                Text(
                    "Bu ay kayıtlı doğum günü yok.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(monthPeople, key = { "mon_${it.id}" }) { p -> PersonRow(p, today) { onOpen(p) } }
    }
}

@Composable
private fun DayCell(day: Int, count: Int, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val bg = when {
        isSelected -> primary
        count > 0 -> MaterialTheme.colorScheme.primaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        count > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bg)
            .then(if (isToday) Modifier.border(BorderStroke(2.dp, primary), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                color = fg,
                fontWeight = if (count > 0 || isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) fg else primary)
                )
            }
        }
    }
}
