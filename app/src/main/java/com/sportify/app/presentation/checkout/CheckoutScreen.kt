package com.sportify.app.presentation.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportify.app.domain.model.BookingStatus
import com.sportify.app.presentation.common.CenterLoading
import com.sportify.app.presentation.common.ErrorText
import com.sportify.app.presentation.common.formatMoney
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(onDone: () -> Unit, onBack: () -> Unit, vm: CheckoutViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheet = rememberPaymentSheet { result -> vm.onPaymentResult(result) }

    // Present Stripe's PaymentSheet once the server returned a client secret.
    LaunchedEffect(ui.session) {
        ui.session?.let { s ->
            PaymentConfiguration.init(context, s.publishableKey)
            sheet.presentWithPaymentIntent(
                s.clientSecret,
                PaymentSheet.Configuration(merchantDisplayName = "Sportify")
            )
            vm.onSheetPresented()
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Checkout") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { pad ->
        val b = ui.booking
        if (b == null) { CenterLoading(Modifier.padding(pad)); return@Scaffold }
        val confirmed = b.bookingStatus == BookingStatus.CONFIRMED

        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (confirmed) {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Booking confirmed!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(b.venueName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row2("Court", b.courtName)
                    Row2("Date", b.date)
                    Row2("Time", b.timeLabel)
                    HorizontalDivider()
                    Row2("Total", formatMoney(b.totalAmount, b.currency), bold = true)
                }
            }
            ErrorText(ui.error)
            ui.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            when {
                confirmed -> Button(onDone, Modifier.fillMaxWidth().height(52.dp)) { Text("View my bookings") }
                b.bookingStatus == BookingStatus.CANCELLED || b.isHoldExpired -> {
                    Text("This reservation has expired. Please choose a new time slot.",
                        color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("Back") }
                }
                else -> {
                    Text("Your slot is held for a few minutes. Pay securely with card or wallet.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(vm::pay, Modifier.fillMaxWidth().height(52.dp), enabled = !ui.busy) {
                        if (ui.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Pay ${formatMoney(b.totalAmount, b.currency)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun Row2(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}
