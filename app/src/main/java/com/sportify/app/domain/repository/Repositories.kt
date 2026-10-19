package com.sportify.app.domain.repository

import android.net.Uri
import com.sportify.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AuthRepository {
    val authState: Flow<Boolean>
    val currentUid: String?
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun signOut()
    fun observeProfile(): Flow<User?>
    suspend fun updateName(name: String): Result<Unit>
    suspend fun uploadProfilePhoto(uri: Uri): Result<Unit>
    suspend fun registerFcmToken(): Result<Unit>
}

interface VenueRepository {
    fun observeVenues(): Flow<List<Venue>>
    fun observeVenue(id: String): Flow<Venue?>
    fun observeCourts(venueId: String): Flow<List<Court>>
    fun observeBookedHours(courtId: String, date: String): Flow<Set<Int>>
}

interface BookingRepository {
    suspend fun createPendingBooking(
        venue: Venue, court: Court, date: LocalDate, startHour: Int, durationHours: Int
    ): Result<String>
    fun observeMyBookings(): Flow<List<Booking>>
    fun observeBooking(id: String): Flow<Booking?>
    suspend fun cancelBooking(booking: Booking): Result<Unit>
}

interface PaymentRepository {
    suspend fun createPaymentSession(bookingId: String): Result<PaymentSession>
    suspend fun confirmPayment(bookingId: String): Result<Unit>
}
