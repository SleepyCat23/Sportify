package com.sportify.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sportify.app.domain.model.*
import com.sportify.app.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BookingRepository {

    companion object { const val HOLD_MILLIS = 10 * 60 * 1000L }

    private fun slotId(courtId: String, date: String, hour: Int) = "${courtId}_${date}_$hour"

    /**
     * Atomically reserves every hourly slot (deterministic doc ids => no double booking)
     * and creates a PENDING_PAYMENT booking that holds the slots for 10 minutes.
     */
    override suspend fun createPendingBooking(
        venue: Venue, court: Court, date: LocalDate, startHour: Int, durationHours: Int
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val dateStr = date.toString()
        val bookingRef = db.collection("bookings").document()
        val expires = Timestamp(Date(System.currentTimeMillis() + HOLD_MILLIS))
        val startInstant = date.atTime(startHour, 0).atZone(ZoneId.systemDefault()).toInstant()
        val slotRefs = (startHour until startHour + durationHours).map {
            it to db.collection("slots").document(slotId(court.id, dateStr, it))
        }

        try {
            db.runTransaction { tx ->
                // all reads first
                val now = Date()
                slotRefs.forEach { (_, ref) ->
                    val snap = tx.get(ref)
                    if (snap.exists()) {
                        val reusable = snap.getString("status") == "PENDING" &&
                            snap.getTimestamp("expiresAt")?.toDate()?.before(now) == true
                        if (!reusable) throw SlotUnavailableException()
                    }
                }
                slotRefs.forEach { (hour, ref) ->
                    tx.set(ref, mapOf(
                        "courtId" to court.id, "date" to dateStr, "hour" to hour,
                        "bookingId" to bookingRef.id, "userId" to uid,
                        "status" to "PENDING", "expiresAt" to expires
                    ))
                }
                tx.set(bookingRef, mapOf(
                    "userId" to uid, "venueId" to venue.id, "venueName" to venue.name,
                    "courtId" to court.id, "courtName" to court.name, "sport" to court.sport,
                    "date" to dateStr, "startHour" to startHour, "durationHours" to durationHours,
                    "totalAmount" to court.pricePerHour * durationHours, "currency" to court.currency,
                    "status" to BookingStatus.PENDING_PAYMENT.name, "paymentStatus" to "UNPAID",
                    "paymentIntentId" to "", "reminderSent" to false,
                    "startTime" to Timestamp(startInstant.epochSecond, 0),
                    "expiresAt" to expires, "createdAt" to FieldValue.serverTimestamp()
                ))
                null
            }.await()
        } catch (e: Exception) {
            if (generateSequence<Throwable>(e) { it.cause }.any { it is SlotUnavailableException })
                throw SlotUnavailableException()
            throw e
        }
        bookingRef.id
    }

    override fun observeMyBookings(): Flow<List<Booking>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return db.collection("bookings").whereEqualTo("userId", uid)
            .asFlow(Booking::class.java)
            .map { list -> list.sortedByDescending { it.startTime?.seconds ?: 0L } }
    }

    override fun observeBooking(id: String): Flow<Booking?> =
        db.collection("bookings").document(id).asFlow(Booking::class.java)

    /** Cancels and frees the slots. Refunds (if paid) are handled server-side by a Cloud Function. */
    override suspend fun cancelBooking(booking: Booking): Result<Unit> = runCatching {
        val batch = db.batch()
        batch.update(db.collection("bookings").document(booking.id), "status", BookingStatus.CANCELLED.name)
        (booking.startHour until booking.startHour + booking.durationHours).forEach {
            batch.delete(db.collection("slots").document(slotId(booking.courtId, booking.date, it)))
        }
        batch.commit().await()
        Unit
    }
}
