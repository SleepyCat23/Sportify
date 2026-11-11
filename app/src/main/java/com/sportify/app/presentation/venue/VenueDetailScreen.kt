package com.sportify.app.presentation.venue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sportify.app.presentation.common.*
import com.sportify.app.presentation.theme.Green
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
                title = {},
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)).background(Green)) {
                    AsyncImage(venue?.imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.matchParentSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))
                    ))
                    Text(
                        venue?.sports?.joinToString(" · ") { sportLabel(it) }.orEmpty(),
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    )
                }
            }
            item {
                venue?.let { v ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top) {
                            Text(v.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            Rating(v.rating)
                        }
                        Text("${v.address}, ${v.city} · Open %02d:00 – %02d:00".format(v.openHour, v.closeHour),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v.description)
                    }
                }
            }
            item { Section("Court") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(courts, key = { it.id }) { c ->
                        OptionChip("${c.name} · ${formatMoney(c.pricePerHour, c.currency)}/h", c.id == form.courtId, { vm.selectCourt(c.id) })
                    }
                }
            } }
            item { Section("Date") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0L..13L).map { LocalDate.now().plusDays(it) }) { d ->
                        OptionChip(
                            "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}",
                            d == form.date, { vm.selectDate(d) }
                        )
                    }
                }
            } }
            item { Section("Duration") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { h -> OptionChip("$h h", h == form.duration, { vm.selectDuration(h) }) }
                }
            } }
            item { Section("Start time") {
                if (slots.isEmpty()) Text("No slots available")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { s ->
                        OptionChip("%02d:00".format(s.hour), s.hour == form.startHour, { vm.selectHour(s.hour) }, enabled = s.available)
                    }
                }
            } }
            item {
                SportifyCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InfoRow("Court", court?.name ?: "—")
                        InfoRow("Date", form.date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())))
                        InfoRow("Time", form.startHour?.let { "%02d:00 – %02d:00".format(it, it + form.duration) } ?: "—")
                        InfoRow("Rate", court?.let { "${formatMoney(it.pricePerHour, it.currency)} / h" } ?: "—")
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        InfoRow("Total", formatMoney(total, court?.currency ?: "USD"), bold = true)
                        ErrorText(form.error)
                        PrimaryButton(
                            "Book & pay", { vm.book(onBookingCreated) }, Modifier.fillMaxWidth(),
                            enabled = form.startHour != null, loading = form.submitting
                        )
                        Text("Your slot is held for 10 minutes while you complete payment.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        content()
    }
}
