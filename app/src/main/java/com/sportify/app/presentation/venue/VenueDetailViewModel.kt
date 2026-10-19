package com.sportify.app.presentation.venue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.Court
import com.sportify.app.domain.model.TimeSlot
import com.sportify.app.domain.repository.BookingRepository
import com.sportify.app.domain.repository.VenueRepository
import com.sportify.app.domain.usecase.BuildTimeSlotsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BookingForm(
    val courtId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val duration: Int = 1,
    val startHour: Int? = null,
    val submitting: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VenueDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val venues: VenueRepository,
    private val bookings: BookingRepository,
    private val buildSlots: BuildTimeSlotsUseCase
) : ViewModel() {

    private val venueId: String = checkNotNull(savedStateHandle["venueId"])
    private val _form = MutableStateFlow(BookingForm())
    val form: StateFlow<BookingForm> = _form

    val venue = venues.observeVenue(venueId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val courts = venues.observeCourts(venueId)
        .map { it.sortedBy(Court::name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val booked = _form.map { it.courtId to it.date }.distinctUntilChanged()
        .flatMapLatest { (court, date) ->
            if (court == null) flowOf(emptySet()) else venues.observeBookedHours(court, date.toString())
        }

    val slots: StateFlow<List<TimeSlot>> = combine(venue, _form, booked) { v, f, b ->
        if (v == null) emptyList() else buildSlots(v, f.date, f.duration, b)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCourt(id: String) = _form.update { it.copy(courtId = id, startHour = null, error = null) }
    fun selectDate(d: LocalDate) = _form.update { it.copy(date = d, startHour = null, error = null) }
    fun selectDuration(h: Int) = _form.update { it.copy(duration = h, startHour = null, error = null) }
    fun selectHour(h: Int) = _form.update { it.copy(startHour = h, error = null) }

    fun book(onCreated: (String) -> Unit) {
        val f = _form.value
        val v = venue.value ?: return
        val court = courts.value.firstOrNull { it.id == f.courtId } ?: return
        val hour = f.startHour ?: return
        viewModelScope.launch {
            _form.update { it.copy(submitting = true, error = null) }
            bookings.createPendingBooking(v, court, f.date, hour, f.duration)
                .onSuccess { id -> _form.update { it.copy(submitting = false) }; onCreated(id) }
                .onFailure { e -> _form.update { it.copy(submitting = false, startHour = null, error = e.message) } }
        }
    }
}
