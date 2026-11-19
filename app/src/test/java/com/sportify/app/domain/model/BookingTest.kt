package com.sportify.app.domain.model

import com.sportify.app.testutil.booking
import org.junit.Assert.*
import org.junit.Test

class BookingTest {
    @Test
    fun `time label shows start and end hour`() {
        assertEquals("18:00 – 20:00", Booking(startHour = 18, durationHours = 2).timeLabel)
    }

    @Test
    fun `pending booking whose hold ran out is expired`() {
        assertTrue(booking(holdExpiresInMillis = -1_000).isHoldExpired)
    }

    @Test
    fun `pending booking within its hold is not expired`() {
        assertFalse(booking(holdExpiresInMillis = 60_000).isHoldExpired)
    }

    @Test
    fun `confirmed booking is never reported as expired`() {
        assertFalse(booking(status = BookingStatus.CONFIRMED, holdExpiresInMillis = null).isHoldExpired)
    }

    @Test
    fun `unknown status falls back to pending payment`() {
        assertEquals(BookingStatus.PENDING_PAYMENT, Booking(status = "SOMETHING_ELSE").bookingStatus)
    }
}
