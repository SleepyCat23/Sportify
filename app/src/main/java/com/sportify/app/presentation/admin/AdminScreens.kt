package com.sportify.app.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportify.app.domain.model.*
import com.sportify.app.presentation.common.*
import com.sportify.app.presentation.theme.Green
import com.sportify.app.presentation.theme.Orange

private val num = KeyboardOptions(keyboardType = KeyboardType.Number)

@Composable
private fun Header(title: String, subtitle: String, message: String?, busy: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

// ---------------------------------------------------------------- UC-13 / UC-14: venues

@Composable
fun AdminVenuesScreen(vm: AdminViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Venue?>(null) }
    var pricing by remember { mutableStateOf<Court?>(null) }
    var hours by remember { mutableStateOf<Venue?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Header("Manage venues", "Add venues, change prices and hours, or remove a venue.", ui.message, ui.busy)
                PrimaryButton("Add venue", { adding = true }, Modifier.fillMaxWidth())
            }
        }
        items(ui.venues, key = { it.venue.id }) { av ->
            val v = av.venue
            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(v.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Rating(v.rating)
                    }
                    Text("${v.city} · ${v.address}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { v.sports.forEach { TagChip(sportLabel(it)) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Open %02d:00 – %02d:00".format(v.openHour, v.closeHour))
                        LinkText("Edit hours", { hours = v })
                    }
                    HorizontalDivider()
                    av.courts.forEach { c ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${c.name} · ${formatMoney(c.pricePerHour, c.currency)}/h")
                            LinkText("Edit price", { pricing = c })
                        }
                    }
                    OutlineButton("Delete venue", { deleting = v }, Modifier.fillMaxWidth(), height = 44.dp)
                }
            }
        }
    }

    deleting?.let { v ->
        AlertDialog(
            onDismissRequest = { deleting = null }, title = { Text("Delete ${v.name}?") },
            text = { Text("Upcoming bookings will be cancelled and refunded. Courts and reviews are removed. This can't be undone.") },
            confirmButton = { TextButton({ vm.deleteVenue(v.id); deleting = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } }
        )
    }
    pricing?.let { c ->
        var text by remember(c.id) { mutableStateOf(c.pricePerHour.toLong().toString()) }
        AlertDialog(
            onDismissRequest = { pricing = null }, title = { Text(c.name) },
            text = { OutlinedTextField(text, { text = it.filter(Char::isDigit) }, label = { Text("Price per hour (${c.currency})") }, singleLine = true, keyboardOptions = num) },
            confirmButton = { TextButton({ vm.setPrice(c.id, text.toDoubleOrNull() ?: 0.0); pricing = null }, enabled = text.isNotEmpty()) { Text("Save") } },
            dismissButton = { TextButton({ pricing = null }) { Text("Cancel") } }
        )
    }
    hours?.let { v ->
        var open by remember(v.id) { mutableStateOf(v.openHour.toString()) }
        var close by remember(v.id) { mutableStateOf(v.closeHour.toString()) }
        AlertDialog(
            onDismissRequest = { hours = null }, title = { Text("Opening hours") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(open, { open = it.filter(Char::isDigit).take(2) }, label = { Text("Opens") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                    OutlinedTextField(close, { close = it.filter(Char::isDigit).take(2) }, label = { Text("Closes") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                }
            },
            confirmButton = { TextButton({ vm.setHours(v.id, open.toIntOrNull() ?: -1, close.toIntOrNull() ?: -1); hours = null }) { Text("Save") } },
            dismissButton = { TextButton({ hours = null }) { Text("Cancel") } }
        )
    }
    if (adding) AddVenueDialog(onDismiss = { adding = false }) { n, c, a, d, s, cn, p, o, cl ->
        vm.addVenue(n, c, a, d, s, cn, p, o, cl, ui.venues.firstNotNullOfOrNull { it.courts.firstOrNull()?.currency } ?: "USD")
        adding = false
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddVenueDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Set<SportType>, String, Double, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var court by remember { mutableStateOf("Court 1") }
    var price by remember { mutableStateOf("20") }
    var open by remember { mutableStateOf("8") }
    var close by remember { mutableStateOf("22") }
    var sports by remember { mutableStateOf(setOf<SportType>()) }
    val valid = name.isNotBlank() && city.isNotBlank() && sports.isNotEmpty() && (price.toDoubleOrNull() ?: 0.0) > 0

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Add venue") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(city, { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                SectionLabel("Sports")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SportType.entries.forEach { s ->
                        OptionChip(s.label, s in sports, { sports = if (s in sports) sports - s else sports + s })
                    }
                }
                OutlinedTextField(court, { court = it }, label = { Text("First court name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Price per hour") }, singleLine = true, keyboardOptions = num, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(open, { open = it.filter(Char::isDigit).take(2) }, label = { Text("Opens") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                    OutlinedTextField(close, { close = it.filter(Char::isDigit).take(2) }, label = { Text("Closes") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onAdd(name, city, address, desc, sports, court, price.toDouble(), open.toIntOrNull() ?: 8, close.toIntOrNull() ?: 22)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------- UC-15: transactions & refunds

@Composable
fun AdminTransactionsScreen(vm: AdminViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val currency = ui.bookings.firstOrNull()?.currency ?: "USD"

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Header("Transactions", "Every booking and its payment status.", ui.message, ui.busy)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Paid revenue" to formatMoney(ui.paidRevenue, currency),
                        "Refunded" to ui.refundedCount.toString()).forEach { (k, v) ->
                        SportifyCard(Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(v, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green)
                            }
                        }
                    }
                }
            }
        }
        items(ui.bookings, key = { it.id }) { b ->
            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(b.venueName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusBadge(
                            when { b.paymentStatus == "REFUNDED" -> "Refunded"; b.bookingStatus == BookingStatus.CONFIRMED -> "Confirmed"
                                b.bookingStatus == BookingStatus.CANCELLED -> "Cancelled"; else -> "Awaiting payment" },
                            when { b.bookingStatus == BookingStatus.CONFIRMED -> BadgeTone.Success
                                b.bookingStatus == BookingStatus.PENDING_PAYMENT -> BadgeTone.Warning; else -> BadgeTone.Neutral }
                        )
                    }
                    Text("${b.courtName} · ${b.date} · ${b.timeLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("User …${b.userId.takeLast(6)} · payment ${b.paymentStatus.lowercase()}", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoney(b.totalAmount, b.currency), fontWeight = FontWeight.Bold, color = Green)
                        if (b.paymentStatus == "PAID") OutlineButton("Refund", { vm.refund(b.id) }, height = 40.dp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- review moderation

@Composable
fun AdminReviewsScreen(vm: AdminViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("Reviews", "Remove reviews that break the rules.", ui.message, ui.busy) }
        if (ui.reviews.isEmpty()) item { Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(ui.reviews, key = { it.id }) { r ->
            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.venueName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Row { repeat(5) { i -> Icon(if (i < r.rating) Icons.Default.Star else Icons.Default.StarBorder, null, Modifier.size(16.dp), tint = Orange) } }
                    }
                    Text("${r.userName}: ${r.comment.ifBlank { "No comment" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlineButton("Delete review", { vm.deleteReview(r.id) }, Modifier.fillMaxWidth(), height = 40.dp)
                }
            }
        }
    }
}
