package com.sportify.app.presentation.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportify.app.domain.model.Booking
import com.sportify.app.domain.model.BookingStatus
import com.sportify.app.presentation.common.*

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

    Scaffold { pad ->
        Column(Modifier.padding(pad)) {
            Text("My bookings", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TabRow(
                selectedTabIndex = tab, containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Upcoming", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                Tab(tab == 1, { tab = 1 }, text = { Text("Past", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ErrorText(ui.error, Modifier.padding(horizontal = 16.dp))
            when {
                ui.loading -> CenterLoading()
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    val (label, tone) = when {
        b.bookingStatus == BookingStatus.CANCELLED ->
            (if (b.paymentStatus == "REFUNDED") "Refunded" else "Cancelled") to BadgeTone.Neutral
        b.isHoldExpired -> "Expired" to BadgeTone.Neutral
        b.bookingStatus == BookingStatus.PENDING_PAYMENT -> "Awaiting payment" to BadgeTone.Warning
        b.endMillis < System.currentTimeMillis() -> "Completed" to BadgeTone.Neutral
        else -> "Confirmed" to BadgeTone.Success
    }
    SportifyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(b.venueName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false))
                StatusBadge(label, tone)
            }
            Text("${b.courtName} · ${b.date} · ${b.timeLabel}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatMoney(b.totalAmount, b.currency), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (isUpcoming) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (b.bookingStatus == BookingStatus.PENDING_PAYMENT) PrimaryButton("Complete payment", onPay, height = 40.dp)
                    OutlineButton("Cancel", onCancel, height = 40.dp)
                }
            }
        }
    }
}
