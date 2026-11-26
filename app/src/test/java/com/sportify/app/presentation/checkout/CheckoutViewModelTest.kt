package com.sportify.app.presentation.checkout

import androidx.lifecycle.SavedStateHandle
import com.sportify.app.fakes.FakeBookingRepository
import com.sportify.app.fakes.FakePaymentRepository
import com.sportify.app.testutil.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CheckoutViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val bookingRepo = FakeBookingRepository().apply { bookings.value = listOf(booking(id = "b1")) }
    private val payments = FakePaymentRepository()

    private fun TestScope.createVm(): CheckoutViewModel {
        val vm = CheckoutViewModel(SavedStateHandle(mapOf("bookingId" to "b1")), bookingRepo, payments)
        collectIn(vm.ui)
        runCurrent()
        return vm
    }

    @Test
    fun `shows the booking that is being paid`() = runTest {
        assertEquals("b1", createVm().ui.value.booking?.id)
    }

    @Test
    fun `pay asks the server for a payment session`() = runTest {
        val vm = createVm()

        vm.pay(); runCurrent()

        assertEquals(listOf("b1"), payments.sessionRequests)
        assertEquals("secret_123", vm.ui.value.session?.clientSecret)
        assertTrue(vm.ui.value.busy)
    }

    @Test
    fun `pay failure shows the error and stops the spinner`() = runTest {
        payments.sessionResult = Result.failure(Exception("Reservation expired. Please book again."))
        val vm = createVm()

        vm.pay(); runCurrent()

        assertEquals("Reservation expired. Please book again.", vm.ui.value.error)
        assertFalse(vm.ui.value.busy)
        assertNull(vm.ui.value.session)
    }

    @Test
    fun `presenting the sheet clears the session so it is not shown twice`() = runTest {
        val vm = createVm()
        vm.pay(); runCurrent()

        vm.onSheetPresented(); runCurrent()

        assertNull(vm.ui.value.session)
    }

    @Test
    fun `completed payment asks the server to confirm the booking`() = runTest {
        val vm = createVm()
        vm.pay(); runCurrent()

        vm.onPaymentCompleted(); runCurrent()

        assertEquals(listOf("b1"), payments.confirmRequests)
        assertFalse(vm.ui.value.busy)
        assertNull(vm.ui.value.error)
    }

    @Test
    fun `confirm failure after paying shows a notice, not an error`() = runTest {
        payments.confirmResult = Result.failure(Exception("network"))
        val vm = createVm()

        vm.onPaymentCompleted(); runCurrent()

        assertNotNull(vm.ui.value.notice)
        assertNull(vm.ui.value.error)
    }

    @Test
    fun `canceled payment stops the spinner without an error`() = runTest {
        val vm = createVm()
        vm.pay(); runCurrent()

        vm.onPaymentCanceled(); runCurrent()

        assertFalse(vm.ui.value.busy)
        assertNull(vm.ui.value.error)
    }

    @Test
    fun `failed payment shows the message from the payment sheet`() = runTest {
        val vm = createVm()

        vm.onPaymentFailed("Your card was declined."); runCurrent()

        assertEquals("Your card was declined.", vm.ui.value.error)
        assertFalse(vm.ui.value.busy)
    }
}
