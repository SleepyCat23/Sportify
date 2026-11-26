package com.sportify.app.fakes

import com.sportify.app.domain.model.*
import com.sportify.app.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class FakeBookingRepository : BookingRepository {
    data class Created(val venueId: String, val courtId: String, val date: LocalDate, val startHour: Int, val duration: Int)

    val bookings = MutableStateFlow<List<Booking>>(emptyList())
    val created = mutableListOf<Created>()
    val cancelled = mutableListOf<String>()
    var createError: Throwable? = null
    var cancelError: Throwable? = null

    override suspend fun createPendingBooking(
        venue: Venue, court: Court, date: LocalDate, startHour: Int, durationHours: Int
    ): Result<String> {
        createError?.let { return Result.failure(it) }
        created += Created(venue.id, court.id, date, startHour, durationHours)
        return Result.success("booking-${created.size}")
    }

    override fun observeMyBookings(): Flow<List<Booking>> = bookings

    override fun observeBooking(id: String): Flow<Booking?> =
        bookings.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun cancelBooking(booking: Booking): Result<Unit> {
        cancelError?.let { return Result.failure(it) }
        cancelled += booking.id
        bookings.update { list ->
            list.map { if (it.id == booking.id) it.copy(status = BookingStatus.CANCELLED.name) else it }
        }
        return Result.success(Unit)
    }
}
