package com.sportify.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.SportType
import com.sportify.app.domain.model.Venue
import com.sportify.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class VenueItem(val venue: Venue, val minPrice: Double?, val currency: String)

data class HomeState(
    val query: String = "", val sport: SportType? = null,
    val venues: List<VenueItem> = emptyList(), val loading: Boolean = true,
    // filters: city, minimum rating, maximum "from" price per hour
    val city: String? = null, val minRating: Double = 0.0, val maxPrice: Double? = null,
    // options offered by the filter menus (derived from the catalogue)
    val cities: List<String> = emptyList(), val prices: List<Double> = emptyList(),
    val currency: String = "USD"
)

private data class Filters(val city: String? = null, val minRating: Double = 0.0, val maxPrice: Double? = null)

@HiltViewModel
class HomeViewModel @Inject constructor(venueRepo: VenueRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sport = MutableStateFlow<SportType?>(null)
    private val filters = MutableStateFlow(Filters())

    val state: StateFlow<HomeState> =
        combine(venueRepo.observeVenues(), venueRepo.observeAllCourts(), query, sport, filters) { all, courts, q, s, f ->
            val cheapest = courts.groupBy { it.venueId }.mapValues { (_, list) -> list.minBy { it.pricePerHour } }
            val items = all.filter { v ->
                (s == null || s.name in v.sports) &&
                    (q.isBlank() || v.name.contains(q, true) || v.city.contains(q, true)) &&
                    (f.city == null || v.city.equals(f.city, true)) &&
                    v.rating >= f.minRating &&
                    (f.maxPrice == null || (cheapest[v.id]?.pricePerHour ?: Double.MAX_VALUE) <= f.maxPrice)
            }.map { v ->
                val c = cheapest[v.id]
                VenueItem(v, c?.pricePerHour, c?.currency ?: "USD")
            }
            HomeState(
                query = q, sport = s, venues = items, loading = false,
                city = f.city, minRating = f.minRating, maxPrice = f.maxPrice,
                cities = all.map { it.city }.distinct().sorted(),
                prices = cheapest.values.map { it.pricePerHour }.distinct().sorted(),
                currency = courts.firstOrNull()?.currency ?: "USD"
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun onQuery(q: String) { query.value = q }
    fun onSport(s: SportType?) { sport.value = s }
    fun onCity(c: String?) = filters.update { it.copy(city = c) }
    fun onMinRating(r: Double) = filters.update { it.copy(minRating = r) }
    fun onMaxPrice(p: Double?) = filters.update { it.copy(maxPrice = p) }
}
