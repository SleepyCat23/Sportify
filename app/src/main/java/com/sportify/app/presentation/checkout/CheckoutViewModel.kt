package com.sportify.app.presentation.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.Booking
import com.sportify.app.domain.model.PaymentSession
import com.sportify.app.domain.repository.BookingRepository
import com.sportify.app.domain.repository.PaymentRepository
import com.stripe.android.paymentsheet.PaymentSheetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUi(
    val booking: Booking? = null,
    val busy: Boolean = false,
    val session: PaymentSession? = null,
    val error: String? = null,
    val notice: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bookings: BookingRepository,
    private val payments: PaymentRepository
) : ViewModel() {

    private val bookingId: String = checkNotNull(savedStateHandle["bookingId"])
    private val local = MutableStateFlow(CheckoutUi())

    val ui: StateFlow<CheckoutUi> = combine(local, bookings.observeBooking(bookingId)) { l, b -> l.copy(booking = b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CheckoutUi())

    fun pay() {
        viewModelScope.launch {
            local.update { it.copy(busy = true, error = null, notice = null) }
            payments.createPaymentSession(bookingId)
                .onSuccess { s -> local.update { it.copy(session = s) } }
                .onFailure { e -> local.update { it.copy(busy = false, error = e.message ?: "Could not start payment") } }
        }
    }

    fun onSheetPresented() = local.update { it.copy(session = null) }

    fun onPaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> viewModelScope.launch {
                // Fast path: server verifies the PaymentIntent with Stripe and confirms the booking.
                // The Stripe webhook is the safety net if this call fails.
                payments.confirmPayment(bookingId)
                    .onFailure {
                        local.update { it.copy(notice = "Payment received. Your booking will be confirmed in a moment.") }
                    }
                local.update { it.copy(busy = false) }
            }
            is PaymentSheetResult.Canceled -> local.update { it.copy(busy = false) }
            is PaymentSheetResult.Failed -> local.update {
                it.copy(busy = false, error = result.error.localizedMessage ?: "Payment failed")
            }
        }
    }
}
