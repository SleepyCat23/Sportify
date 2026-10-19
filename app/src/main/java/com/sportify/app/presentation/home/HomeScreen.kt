package com.sportify.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sportify.app.domain.model.SportType
import com.sportify.app.domain.model.Venue
import com.sportify.app.presentation.common.CenterLoading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onVenueClick: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Sportify", fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.query, onValueChange = vm::onQuery, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search venues or city") },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(state.sport == null, { vm.onSport(null) }, { Text("All") }) }
                    items(SportType.entries.toList()) { s ->
                        FilterChip(state.sport == s, { vm.onSport(if (state.sport == s) null else s) }, { Text(s.label) })
                    }
                }
            }
            when {
                state.loading -> item { CenterLoading(Modifier.height(200.dp)) }
                state.venues.isEmpty() -> item {
                    Text("No venues found", Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> items(state.venues, key = { it.id }) { VenueCard(it) { onVenueClick(it.id) } }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = venue.imageUrl, contentDescription = venue.name, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(venue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Text(" %.1f".format(venue.rating), style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${venue.city} · ${venue.address}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                venue.sports.mapNotNull { runCatching { SportType.valueOf(it) }.getOrNull() }.forEach {
                    AssistChip(onClick = {}, label = { Text(it.label) })
                }
            }
        }
    }
}
