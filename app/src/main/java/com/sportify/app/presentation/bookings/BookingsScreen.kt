package com.sportify.app.presentation.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportify.app.domain.model.Booking
import com.sportify.app.domain.model.BookingStatus
import com.sportify.app.presentation.common.CenterLoading
import com.sportify.app.presentation.common.ErrorText
import com.sportify.app.presentation.common.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(onPay: (String) -> Unit, vm: BookingsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var toCancel by remember { mutableStateOf<Booking?>(null) }
    val now = System.currentTimeMillis()

    val upcoming = ui.bookings.filter {
        it.endMillis > now && (it.bookingStatus == BookingStatus.CONFIRMED ||
            (it.bookingStatus == BookingStatus.PENDING_PAYMENT && !it.isHoldExpired))
    }
    val past = ui.bookings - upcoming.toSet()
    val shown = if (tab == 0) upcoming else past

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("My bookings", fontWeight = FontWeight.Bold) }) }) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Upcoming") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Past") })
            }
            ErrorText(ui.error, Modifier.padding(horizontal = 16.dp))
            when {
                ui.loading -> CenterLoading()
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shown, key = { it.id }) { b ->
                        BookingCard(b, isUpcoming = tab == 0, onPay = { onPay(b.id) }, onCancel = { toCancel = b })
                    }
                }
            }
        }
    }

    toCancel?.let { b ->
        AlertDialog(
            onDismissRequest = { toCancel = null },
            title = { Text("Cancel booking?") },
            text = { Text(if (b.paymentStatus == "PAID")
                "Bookings cancelled at least 2 hours before start are refunded automatically." else "This will release your slot.") },
            confirmButton = { TextButton({ vm.cancel(b); toCancel = null }) { Text("Cancel booking") } },
            dismissButton = { TextButton({ toCancel = null }) { Text("Keep") } }
        )
    }
}

@Composable
private fun BookingCard(b: Booking, isUpcoming: Boolean, onPay: () -> Unit, onCancel: () -> Unit) {
    val label = when {
        b.bookingStatus == BookingStatus.CANCELLED -> if (b.paymentStatus == "REFUNDED") "Refunded" else "Cancelled"
        b.isHoldExpired -> "Expired"
        b.bookingStatus == BookingStatus.PENDING_PAYMENT -> "Awaiting payment"
        b.endMillis < System.currentTimeMillis() -> "Completed"
        else -> "Confirmed"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(b.venueName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SuggestionChip(onClick = {}, label = { Text(label) })
            }
            Text("${b.courtName} · ${b.date} · ${b.timeLabel}", style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(b.totalAmount, b.currency), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            if (isUpcoming) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (b.bookingStatus == BookingStatus.PENDING_PAYMENT) Button(onPay) { Text("Complete payment") }
                OutlinedButton(onCancel) { Text("Cancel") }
            }
        }
    }
}
