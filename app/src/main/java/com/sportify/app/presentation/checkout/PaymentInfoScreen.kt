package com.sportify.app.presentation.checkout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportify.app.R
import com.sportify.app.presentation.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentInfoScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: CheckoutViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val booking = ui.booking
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Information") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        if (booking == null) {
            CenterLoading(Modifier.padding(pad))
        } else {
            Column(
                modifier = Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SportifyCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Booking Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        InfoRow("Venue", booking.venueName)
                        InfoRow("Court", booking.courtName)
                        InfoRow("Date", booking.date)
                        InfoRow("Time", booking.timeLabel)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InfoRow("Total Amount", formatMoney(booking.totalAmount, booking.currency), bold = true)
                    }
                }

                Text(
                    text = "Scan the QRIS below or transfer to the BCA account to pay.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // QRIS Image (Local Resource: app/src/main/res/drawable/payment_qris.jpg)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.payment_qris),
                            contentDescription = "Payment QRIS",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Copyable Bank Info
                OutlinedCard(
                    onClick = { clipboardManager.setText(AnnotatedString("7475280695")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BCA Account (Tap to copy)", style = MaterialTheme.typography.labelSmall)
                            Text("7475280695", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("EZEKIEL AMADEUS KAMLESH", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                }

                Text(
                    text = "Once payment is made, our team will verify it and confirm your booking.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                PrimaryButton(
                    text = "I've made the payment",
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
