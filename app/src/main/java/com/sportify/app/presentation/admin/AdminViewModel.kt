package com.sportify.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.*
import com.sportify.app.domain.repository.AdminRepository
import com.sportify.app.domain.repository.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminVenue(val venue: Venue, val courts: List<Court>)

data class AdminUi(
    val venues: List<AdminVenue> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null
) {
    val paidRevenue: Double get() = bookings.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
    val refundedCount: Int get() = bookings.count { it.paymentStatus == "REFUNDED" }
}

private data class Local(val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class AdminViewModel @Inject constructor(
    venueRepo: VenueRepository,
    private val admin: AdminRepository
) : ViewModel() {
    private val local = MutableStateFlow(Local())

    private val catalogue = combine(venueRepo.observeVenues(), venueRepo.observeAllCourts()) { v, c ->
        val byVenue = c.groupBy { it.venueId }
        v.map { AdminVenue(it, byVenue[it.id].orEmpty().sortedBy(Court::name)) }
    }

    val ui: StateFlow<AdminUi> = combine(
        catalogue, admin.observeAllBookings(), admin.observeAllReviews(), local
    ) { v, b, r, l -> AdminUi(v, b, r, l.busy, l.message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminUi())

    fun dismissMessage() = local.update { it.copy(message = null) }

    private fun run(ok: String, block: suspend () -> Result<Unit>) = viewModelScope.launch {
        local.value = Local(busy = true)
        block().onSuccess { local.value = Local(message = ok) }
            .onFailure { local.value = Local(message = it.message ?: "Something went wrong") }
    }

    fun addVenue(
        name: String, city: String, address: String, description: String, sports: Set<SportType>,
        courtName: String, price: Double, open: Int, close: Int, currency: String
    ) = run("Venue added") {
        require(open < close) { "Closing hour must be after opening hour" }
        admin.addVenue(
            Venue(name = name.trim(), city = city.trim(), address = address.trim(), description = description.trim(),
                sports = sports.map { it.name }, openHour = open, closeHour = close),
            Court(name = courtName.trim().ifBlank { "Court 1" }, sport = sports.first().name,
                pricePerHour = price, currency = currency)
        )
    }
    fun deleteVenue(id: String) = run("Venue removed · upcoming bookings were cancelled") { admin.deleteVenue(id) }
    fun setPrice(courtId: String, price: Double) = run("Price updated") { admin.updateCourtPrice(courtId, price) }
    fun setHours(venueId: String, open: Int, close: Int) = run("Hours updated") { admin.updateHours(venueId, open, close) }
    fun refund(bookingId: String) = run("Refund issued · slot released") { admin.refund(bookingId) }
    fun deleteReview(id: String) = run("Review deleted") { admin.deleteReview(id) }
}
