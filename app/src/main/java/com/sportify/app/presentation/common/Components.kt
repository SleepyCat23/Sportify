package com.sportify.app.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
