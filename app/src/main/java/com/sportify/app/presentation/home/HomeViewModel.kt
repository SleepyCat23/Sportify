package com.sportify.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.SportType
import com.sportify.app.domain.model.Venue
import com.sportify.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeState(
    val query: String = "", val sport: SportType? = null,
    val venues: List<Venue> = emptyList(), val loading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(venueRepo: VenueRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sport = MutableStateFlow<SportType?>(null)

    val state: StateFlow<HomeState> = combine(venueRepo.observeVenues(), query, sport) { all, q, s ->
        val filtered = all.filter { v ->
            (s == null || s.name in v.sports) &&
                (q.isBlank() || v.name.contains(q, true) || v.city.contains(q, true))
        }
        HomeState(q, s, filtered, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    fun onQuery(q: String) { query.value = q }
    fun onSport(s: SportType?) { sport.value = s }
}
