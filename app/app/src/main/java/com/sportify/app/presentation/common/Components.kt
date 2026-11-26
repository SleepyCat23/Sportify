package com.sportify.app.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sportify.app.domain.model.SportType
import com.sportify.app.presentation.theme.*
import java.text.NumberFormat
import java.util.Currency

@Composable
fun CenterLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message != null) Text(
        message, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium, modifier = modifier.padding(vertical = 4.dp)
    )
}

fun formatMoney(amount: Double, currency: String): String = runCatching {
    NumberFormat.getCurrencyInstance().apply {
        this.currency = Currency.getInstance(currency)
        if (currency == "IDR") maximumFractionDigits = 0
    }.format(amount)
}.getOrElse { "$currency %.2f".format(amount) }

fun sportLabel(name: String): String =
    runCatching { SportType.valueOf(name).label }.getOrDefault(name)

/** White, softly bordered card with rounded corners (flat, no shadow). */
@Composable
fun SportifyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(containerColor = Color.White)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    else Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
}

/** Selectable chip used for sports, courts, dates, durations and time slots. */
@Composable
fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val container = when { !enabled -> DisabledBg; selected -> Mint; else -> Color.White }
    val content = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = 0.4f)
    val border = when { !enabled -> Color.Transparent; selected -> scheme.primary; else -> scheme.outlineVariant }
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(10.dp),
        color = container, contentColor = content, border = BorderStroke(1.dp, border)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
fun TagChip(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = TagBg, contentColor = Ink) {
        Text(text, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

enum class BadgeTone { Success, Warning, Neutral }

@Composable
fun StatusBadge(text: String, tone: BadgeTone) {
    val (bg, fg) = when (tone) {
        BadgeTone.Success -> Mint to OnMint
        BadgeTone.Warning -> Peach to PeachText
        BadgeTone.Neutral -> DisabledBg to Muted
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg, contentColor = fg) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun Rating(value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = Orange)
        Text(" %.1f".format(value), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable(onClick = onClick))
}

@Composable
fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
fun PrimaryButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, loading: Boolean = false, height: Dp = 52.dp
) {
    Button(
        onClick = { if (!loading) onClick() }, enabled = enabled,
        shape = RoundedCornerShape(12.dp), modifier = modifier.height(height)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary)
        else Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun OutlineButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, height: Dp = 52.dp
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(height)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}
