@file:OptIn(ExperimentalMaterial3Api::class)

package com.dogumgunu.asistanim.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dogumgunu.asistanim.data.Person
import com.dogumgunu.asistanim.domain.BirthdayCalc
import com.dogumgunu.asistanim.util.PhotoStore
import com.dogumgunu.asistanim.util.TurkishText
import java.time.LocalDate

/** Fotoğraf varsa onu, yoksa baş harfleri gösteren yuvarlak avatar. */
@Composable
fun Avatar(person: Person, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bitmap = remember(person.photo) { PhotoStore.load(ctx, person.photo)?.asImageBitmap() }
    val initials = remember(person.firstName, person.lastName) {
        val a = person.firstName.trim().take(1).uppercase(TurkishText.TR)
        val b = person.lastName.trim().take(1).uppercase(TurkishText.TR)
        (a + b).ifEmpty { "?" }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = person.fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.6f).sp
            )
        }
    }
}

fun daysText(days: Int): String = when (days) {
    0 -> "Bugün 🎉"
    1 -> "Yarın"
    else -> "$days gün"
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

/** Listede bir kişi satırı: fotoğraf, ad, tarih ve doğum gününe kalan gün. */
@Composable
fun PersonRow(person: Person, today: LocalDate, onClick: () -> Unit) {
    val days = BirthdayCalc.daysUntil(person, today)
    val age = BirthdayCalc.turningAge(person, today)
    val next = BirthdayCalc.next(person, today)
    val ageText = when {
        age == null || age <= 0 -> ""
        days == 0 -> " • $age yaşına giriyor"
        else -> " • $age yaşına girecek"
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(person, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = TurkishText.dateText(next) + ageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Text(
                    text = daysText(days),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (days <= 7) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (days > 1) {
                    Text(
                        text = "kaldı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
