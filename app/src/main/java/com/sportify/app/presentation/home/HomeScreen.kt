package com.sportify.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sportify.app.domain.model.SportType
import com.sportify.app.presentation.common.*
import com.sportify.app.presentation.theme.Green

@Composable
fun HomeScreen(onVenueClick: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold { pad ->
        LazyColumn(
            Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Find a court to play on", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Book courts and pitches in seconds.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Search venues or city", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.query, onValueChange = vm::onQuery, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text("Search venues or city") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OptionChip("All", state.sport == null, { vm.onSport(null) }) }
                    items(SportType.entries.toList()) { s ->
                        OptionChip(s.label, state.sport == s, { vm.onSport(if (state.sport == s) null else s) })
                    }
                }
            }
            when {
                state.loading -> item { CenterLoading(Modifier.height(200.dp)) }
                state.venues.isEmpty() -> item {
                    Text("No venues found", Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> items(state.venues, key = { it.venue.id }) { VenueCard(it) { onVenueClick(it.venue.id) } }
            }
        }
    }
}

@Composable
private fun VenueCard(item: VenueItem, onClick: () -> Unit) {
    val venue = item.venue
    SportifyCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Box {
            AsyncImage(
                model = venue.imageUrl, contentDescription = venue.name, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(170.dp).background(Green)
            )
            Box(Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))
            ))
            Text(
                sportLabel(venue.sports.firstOrNull().orEmpty()), color = Color.White,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(venue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Rating(venue.rating)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${venue.city} · ${venue.address}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                venue.sports.forEach { TagChip(sportLabel(it)) }
            }
            item.minPrice?.let { price ->
                Text(
                    buildAnnotatedString {
                        append("From ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                            append(formatMoney(price, item.currency))
                        }
                        append(" / hour")
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
