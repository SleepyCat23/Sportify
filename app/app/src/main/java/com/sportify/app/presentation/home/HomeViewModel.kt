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
    val venues: List<VenueItem> = emptyList(), val loading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(venueRepo: VenueRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sport = MutableStateFlow<SportType?>(null)

    val state: StateFlow<HomeState> =
        combine(venueRepo.observeVenues(), venueRepo.observeAllCourts(), query, sport) { all, courts, q, s ->
            val cheapest = courts.groupBy { it.venueId }.mapValues { (_, list) -> list.minBy { it.pricePerHour } }
            val items = all.filter { v ->
                (s == null || s.name in v.sports) &&
                    (q.isBlank() || v.name.contains(q, true) || v.city.contains(q, true))
            }.map { v ->
                val c = cheapest[v.id]
                VenueItem(v, c?.pricePerHour, c?.currency ?: "USD")
            }
            HomeState(q, s, items, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun onQuery(q: String) { query.value = q }
    fun onSport(s: SportType?) { sport.value = s }
}
