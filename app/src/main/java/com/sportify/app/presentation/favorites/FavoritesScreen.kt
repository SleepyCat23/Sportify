package com.sportify.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
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
import com.sportify.app.domain.model.Venue
import com.sportify.app.presentation.common.CenterLoading
import com.sportify.app.presentation.common.ErrorText
import com.sportify.app.presentation.common.Rating
import com.sportify.app.presentation.common.SportifyCard
import com.sportify.app.presentation.common.sportLabel

@Composable
fun FavoritesRoute(onVenueClick: (String) -> Unit, vm: FavoritesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    FavoritesScreen(ui, onVenueClick, vm::toggle)
}

@Composable
private fun FavoritesScreen(
    ui: FavoritesUiState,
    onVenueClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Favorite venues", Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            ErrorText(ui.error, Modifier.padding(horizontal = 16.dp))
            when {
                ui.loading -> CenterLoading()
                ui.venues.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorite venues yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.venues, key = { it.id }) { venue ->
                        FavoriteVenueCard(venue, onClick = { onVenueClick(venue.id) },
                            onToggle = { onToggleFavorite(venue.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteVenueCard(venue: Venue, onClick: () -> Unit, onToggle: () -> Unit) {
    SportifyCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Box {
            AsyncImage(venue.imageUrl, venue.name, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.surfaceVariant))
            IconButton(onClick = onToggle, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Favorite, contentDescription = "Remove ${venue.name} from favorites",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(venue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Rating(venue.rating)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" ${venue.city} · ${venue.address}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(venue.sports.joinToString(" · ") { sportLabel(it) }, style = MaterialTheme.typography.bodySmall)
        }
    }
}
