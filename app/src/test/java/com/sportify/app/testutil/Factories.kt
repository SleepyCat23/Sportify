package com.sportify.app.testutil

import com.google.firebase.Timestamp
import com.sportify.app.domain.model.*
import java.util.Date

fun venue(
    id: String = "v1", name: String = "Ace Tennis Club", city: String = "Bandung",
    sports: List<String> = listOf("TENNIS"), open: Int = 7, close: Int = 22
) = Venue(id = id, name = name, city = city, address = "Jl. Dago 12", sports = sports,
    rating = 4.7, openHour = open, closeHour = close)

fun court(id: String = "c1", venueId: String = "v1", price: Double = 250_000.0, name: String = "Court 1") =
    Court(id = id, venueId = venueId, name = name, sport = "TENNIS", pricePerHour = price, currency = "IDR")

private const val DAY = 24 * 3_600_000L

fun booking(
    id: String = "b1",
    status: BookingStatus = BookingStatus.PENDING_PAYMENT,
    paymentStatus: String = "UNPAID",
    startsInMillis: Long = 2 * DAY,
    holdExpiresInMillis: Long? = 10 * 60_000L
) = Booking(
    id = id, userId = "u1", venueId = "v1", venueName = "Ace Tennis Club",
    courtId = "c1", courtName = "Court 1", sport = "TENNIS", date = "2026-11-20",
    startHour = 18, durationHours = 2, totalAmount = 500_000.0, currency = "IDR",
    status = status.name, paymentStatus = paymentStatus,
    startTime = Timestamp(Date(System.currentTimeMillis() + startsInMillis)),
    expiresAt = holdExpiresInMillis?.let { Timestamp(Date(System.currentTimeMillis() + it)) }
)
