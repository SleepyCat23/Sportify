package com.sportify.app.presentation.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.sportify.app.presentation.common.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.delay

private fun secondsLeft(expiresAt: Long?): Long =
    if (expiresAt == null) 0L else maxOf(0L, (expiresAt - System.currentTimeMillis()) / 1000)

/** Ticks once per second until the hold expires. */
@Composable
private fun rememberSecondsLeft(expiresAt: Long?): Long {
    var remaining by remember(expiresAt) { mutableLongStateOf(secondsLeft(expiresAt)) }
    LaunchedEffect(expiresAt) {
        while (secondsLeft(expiresAt) > 0) {
            remaining = secondsLeft(expiresAt)
            delay(1000)
        }
        remaining = 0
    }
    return remaining
}

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
            sheet.presentWithPaymentIntent(s.clientSecret, PaymentSheet.Configuration(merchantDisplayName = "Sportify"))
            vm.onSheetPresented()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {},
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { pad ->
        val b = ui.booking
        if (b == null) { CenterLoading(Modifier.padding(pad)); return@Scaffold }

        val remaining = rememberSecondsLeft(b.expiresAt?.toDate()?.time)
        val confirmed = b.bookingStatus == BookingStatus.CONFIRMED
        val pending = b.bookingStatus == BookingStatus.PENDING_PAYMENT
        val expired = b.bookingStatus == BookingStatus.CANCELLED || (pending && remaining <= 0)

        Column(
            Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Checkout", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(b.venueName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    InfoRow("Court", b.courtName)
                    InfoRow("Date", b.date)
                    InfoRow("Time", b.timeLabel)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    InfoRow("Total", formatMoney(b.totalAmount, b.currency), bold = true)
                }
            }

            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        confirmed -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Booking confirmed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Text("We've sent you a notification. See you on court!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PrimaryButton("View my bookings", onDone, Modifier.fillMaxWidth())
                        }
                        expired -> {
                            StatusBadge("Reservation expired", BadgeTone.Neutral)
                            Text("This slot is no longer held. Please choose a new time.",
                                color = MaterialTheme.colorScheme.error)
                            OutlineButton("Back", onBack, Modifier.fillMaxWidth())
                        }
                        else -> {
                            StatusBadge("Slot held · %02d:%02d left".format(remaining / 60, remaining % 60), BadgeTone.Warning)
                            Text("Pay securely", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Your slot is held for a few minutes. Pay with card or wallet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ErrorText(ui.error)
                            ui.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                            PrimaryButton(
                                "Pay ${formatMoney(b.totalAmount, b.currency)}", vm::pay,
                                Modifier.fillMaxWidth(), loading = ui.busy
                            )
                            Text("Cancel at least 2 hours before start for an automatic refund.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
