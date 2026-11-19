package com.sportify.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.firebase.Timestamp
import com.sportify.app.domain.model.Booking
import com.sportify.app.domain.model.BookingStatus
import com.sportify.app.presentation.bookings.BookingsScreen
import com.sportify.app.presentation.bookings.BookingsUi
import com.sportify.app.presentation.theme.SportifyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.Date

class BookingsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun booking(id: String, status: BookingStatus, paymentStatus: String = "PAID") = Booking(
        id = id, venueName = "Ace Tennis Club", courtName = "Court 1", date = "2026-11-20",
        startHour = 18, durationHours = 2, totalAmount = 500_000.0, currency = "IDR",
        status = status.name, paymentStatus = paymentStatus,
        startTime = Timestamp(Date(System.currentTimeMillis() + 2 * 24 * 3_600_000L)),
        expiresAt = Timestamp(Date(System.currentTimeMillis() + 10 * 60_000L))
    )

    private fun show(
        bookings: List<Booking>,
        onPay: (String) -> Unit = {},
        onCancel: (Booking) -> Unit = {}
    ) = composeRule.setContent {
        SportifyTheme { BookingsScreen(BookingsUi(loading = false, bookings = bookings), onPay, onCancel) }
    }

    @Test
    fun emptyListShowsAPlaceholder() {
        show(emptyList())

        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
    }

    @Test
    fun confirmedBookingShowsVenueStatusAndCancelButton() {
        show(listOf(booking("b1", BookingStatus.CONFIRMED)))

        composeRule.onNodeWithText("Ace Tennis Club").assertIsDisplayed()
        composeRule.onNodeWithText("Confirmed").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun unpaidBookingOffersCompletePaymentAndReportsItsId() {
        var paid: String? = null
        show(listOf(booking("b2", BookingStatus.PENDING_PAYMENT, "UNPAID")), onPay = { paid = it })

        composeRule.onNodeWithText("Awaiting payment").assertIsDisplayed()
        composeRule.onNodeWithText("Complete payment").performClick()

        assertEquals("b2", paid)
    }

    @Test
    fun cancelAsksForConfirmationBeforeCallingBack() {
        var cancelled: Booking? = null
        show(listOf(booking("b1", BookingStatus.CONFIRMED)), onCancel = { cancelled = it })

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Cancel booking?").assertIsDisplayed()
        assertNull(cancelled) // nothing happens until the user confirms

        composeRule.onNodeWithText("Cancel booking").performClick()
        assertEquals("b1", cancelled?.id)
    }

    @Test
    fun keepButtonClosesTheDialogWithoutCancelling() {
        var cancelled: Booking? = null
        show(listOf(booking("b1", BookingStatus.CONFIRMED)), onCancel = { cancelled = it })

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Keep").performClick()

        composeRule.onNodeWithText("Cancel booking?").assertDoesNotExist()
        assertNull(cancelled)
    }

    @Test
    fun cancelledBookingMovesToThePastTab() {
        show(listOf(booking("b1", BookingStatus.CANCELLED)))

        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed() // Upcoming tab
        composeRule.onNodeWithText("Past").performClick()

        composeRule.onNodeWithText("Cancelled").assertIsDisplayed()
    }
}
