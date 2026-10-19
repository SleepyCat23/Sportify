package com.sportify.app.presentation.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.Booking
import com.sportify.app.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingsUi(val loading: Boolean = true, val bookings: List<Booking> = emptyList(), val error: String? = null)

@HiltViewModel
class BookingsViewModel @Inject constructor(private val repo: BookingRepository) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)

    val ui: StateFlow<BookingsUi> = combine(repo.observeMyBookings(), error) { list, e ->
        BookingsUi(loading = false, bookings = list, error = e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookingsUi())

    fun cancel(b: Booking) = viewModelScope.launch {
        repo.cancelBooking(b).onFailure { error.value = it.message }
    }
}
