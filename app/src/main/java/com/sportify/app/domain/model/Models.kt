package com.sportify.app.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

enum class SportType(val label: String) {
    TENNIS("Tennis"), PADEL("Padel"), BASKETBALL("Basketball"),
    FOOTBALL("Football"), BADMINTON("Badminton"), VOLLEYBALL("Volleyball")
}

enum class BookingStatus { PENDING_PAYMENT, CONFIRMED, CANCELLED }

data class User(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val fcmToken: String = ""
)

data class Venue(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val imageUrl: String = "",
    val sports: List<String> = emptyList(),   // SportType names
    val rating: Double = 0.0,
    val openHour: Int = 8,
    val closeHour: Int = 22
)

data class Court(
    @DocumentId val id: String = "",
    val venueId: String = "",
    val name: String = "",
    val sport: String = "",
    val pricePerHour: Double = 0.0,
    val currency: String = "USD"
)

data class Booking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val venueId: String = "",
    val venueName: String = "",
    val courtId: String = "",
    val courtName: String = "",
    val sport: String = "",
    val date: String = "",              // yyyy-MM-dd
    val startHour: Int = 0,
    val durationHours: Int = 1,
    val totalAmount: Double = 0.0,
    val currency: String = "USD",
    val status: String = BookingStatus.PENDING_PAYMENT.name,
    val paymentStatus: String = "UNPAID", // UNPAID | PAID | REFUNDED
    val paymentIntentId: String = "",
    val startTime: Timestamp? = null,
    val expiresAt: Timestamp? = null,    // payment hold deadline
    val createdAt: Timestamp? = null
) {
    @get:Exclude
    val bookingStatus: BookingStatus
        get() = runCatching { BookingStatus.valueOf(status) }.getOrDefault(BookingStatus.PENDING_PAYMENT)

    @get:Exclude
    val isHoldExpired: Boolean
        get() = bookingStatus == BookingStatus.PENDING_PAYMENT &&
            (expiresAt?.toDate()?.before(Date()) ?: true)

    @get:Exclude
    val endMillis: Long
        get() = (startTime?.toDate()?.time ?: 0L) + durationHours * 3_600_000L

    @get:Exclude
    val timeLabel: String
        get() = "%02d:00 – %02d:00".format(startHour, startHour + durationHours)
}

data class TimeSlot(val hour: Int, val available: Boolean)

data class PaymentSession(val clientSecret: String, val publishableKey: String)

class SlotUnavailableException : Exception("That time slot was just taken. Please pick another.")
