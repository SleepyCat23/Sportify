package com.sportify.app.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.Venue
import com.sportify.app.domain.repository.FavoriteRepository
import com.sportify.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val loading: Boolean = true,
    val venues: List<Venue> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    venues: VenueRepository,
    private val favorites: FavoriteRepository
) : ViewModel() {
    val ui: StateFlow<FavoritesUiState> = combine(
        venues.observeVenues(), favorites.observeFavoriteVenueIds()
    ) { allVenues, favoriteIds ->
        FavoritesUiState(loading = false, venues = allVenues.filter { it.id in favoriteIds }, favoriteIds = favoriteIds)
    }.catch { error -> emit(FavoritesUiState(loading = false, error = error.message ?: "Could not load favorites")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun toggle(venueId: String) {
        val isFavorite = venueId in ui.value.favoriteIds
        viewModelScope.launch { favorites.setFavorite(venueId, !isFavorite) }
    }
}
