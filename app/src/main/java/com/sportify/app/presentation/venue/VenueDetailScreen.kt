package com.sportify.app.presentation.venue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sportify.app.presentation.common.ErrorText
import com.sportify.app.presentation.common.formatMoney
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VenueDetailScreen(
    onBack: () -> Unit,
    onBookingCreated: (String) -> Unit,
    vm: VenueDetailViewModel = hiltViewModel()
) {
    val venue by vm.venue.collectAsStateWithLifecycle()
    val courts by vm.courts.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    val slots by vm.slots.collectAsStateWithLifecycle()
    val court = courts.firstOrNull { it.id == form.courtId }
    val total = (court?.pricePerHour ?: 0.0) * form.duration

    LaunchedEffect(courts) {
        if (form.courtId == null) courts.firstOrNull()?.let { vm.selectCourt(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(venue?.name.orEmpty()) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.labelMedium)
                        Text(formatMoney(total, court?.currency ?: "USD"),
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { vm.book(onBookingCreated) },
                        enabled = form.startHour != null && !form.submitting,
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (form.submitting) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Book & pay")
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                AsyncImage(venue?.imageUrl, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp))
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(venue?.address.orEmpty() + ", " + venue?.city.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Text(venue?.description.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Section("Court") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(courts, key = { it.id }) { c ->
                        FilterChip(c.id == form.courtId, { vm.selectCourt(c.id) },
                            { Text("${c.name} · ${formatMoney(c.pricePerHour, c.currency)}/h") })
                    }
                }
            } }
            item { Section("Date") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items((0L..13L).map { LocalDate.now().plusDays(it) }) { d ->
                        FilterChip(d == form.date, { vm.selectDate(d) }, {
                            Text("${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}")
                        })
                    }
                }
            } }
            item { Section("Duration") {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { h ->
                        FilterChip(h == form.duration, { vm.selectDuration(h) }, { Text("$h h") })
                    }
                }
            } }
            item { Section("Start time") {
                if (slots.isEmpty()) Text("No slots available", Modifier.padding(horizontal = 16.dp))
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { s ->
                        FilterChip(s.hour == form.startHour, { vm.selectHour(s.hour) },
                            { Text("%02d:00".format(s.hour)) }, enabled = s.available)
                    }
                }
                ErrorText(form.error, Modifier.padding(horizontal = 16.dp))
            } }
            item {
                Text("Your slot is held for 10 minutes while you complete payment.",
                    Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}
