package com.sportify.app.domain.repository

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
    suspend fun uploadProfilePhoto(uri: String): Result<Unit>
    suspend fun registerFcmToken(): Result<Unit>
}

interface VenueRepository {
    fun observeVenues(): Flow<List<Venue>>
    fun observeVenue(id: String): Flow<Venue?>
    fun observeCourts(venueId: String): Flow<List<Court>>
    fun observeAllCourts(): Flow<List<Court>>
    fun observeBookedHours(courtId: String, date: String): Flow<Set<Int>>
}

interface FavoriteRepository {
    fun observeFavoriteVenueIds(): Flow<Set<String>>
    suspend fun setFavorite(venueId: String, favorite: Boolean): Result<Unit>
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

// ---------------- Social, reviews & admin ----------------

interface ReviewRepository {
    fun observeReviews(venueId: String): Flow<List<Review>>
    suspend fun addReview(venueId: String, venueName: String, rating: Int, comment: String): Result<Unit>
}

interface OpenPlayRepository {
    fun observeOpenPlays(): Flow<List<OpenPlay>>
    suspend fun create(title: String, sport: String, venueName: String, date: String, startHour: Int, maxPlayers: Int): Result<Unit>
    suspend fun requestJoin(playId: String): Result<Unit>
    /** Withdraws a pending request or leaves the game. */
    suspend fun leave(playId: String): Result<Unit>
    /** Host only. Approving a request in a full game is ignored. */
    suspend fun decide(playId: String, uid: String, approve: Boolean): Result<Unit>
    suspend fun delete(playId: String): Result<Unit>
}

interface MatchRepository {
    fun observeMyPlayer(): Flow<Player?>
    suspend fun saveMyPlayer(level: String, sport: String): Result<Unit>
    fun observePlayers(): Flow<List<Player>>
    fun observeInvites(): Flow<List<Invite>>
    suspend fun invite(to: Player): Result<Unit>
    suspend fun respond(invite: Invite, accept: Boolean): Result<Unit>
    fun observeMyResults(): Flow<List<MatchResult>>
    suspend fun addResult(opponent: String, sport: String, myScore: Int, theirScore: Int): Result<Unit>
}

interface AdminRepository {
    fun observeAllBookings(): Flow<List<Booking>>
    fun observeAllReviews(): Flow<List<Review>>
    suspend fun addVenue(venue: Venue, firstCourt: Court): Result<Unit>
    /** Cancels (and refunds, if paid) every upcoming booking, then removes the venue, its courts and reviews. */
    suspend fun deleteVenue(venueId: String): Result<Unit>
    suspend fun updateCourtPrice(courtId: String, price: Double): Result<Unit>
    suspend fun updateHours(venueId: String, openHour: Int, closeHour: Int): Result<Unit>
    suspend fun refund(bookingId: String): Result<Unit>
    suspend fun deleteReview(reviewId: String): Result<Unit>
}
