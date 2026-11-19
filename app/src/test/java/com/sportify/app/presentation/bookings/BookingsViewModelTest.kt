package com.sportify.app.presentation.bookings

import com.sportify.app.domain.model.BookingStatus
import com.sportify.app.fakes.FakeBookingRepository
import com.sportify.app.testutil.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BookingsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeBookingRepository()

    private fun TestScope.createVm(): BookingsViewModel {
        val vm = BookingsViewModel(repo)
        collectIn(vm.ui)
        runCurrent()
        return vm
    }

    @Test
    fun `shows the bookings from the repository`() = runTest {
        repo.bookings.value = listOf(booking(id = "b1"), booking(id = "b2"))

        val ui = createVm().ui.value

        assertFalse(ui.loading)
        assertEquals(listOf("b1", "b2"), ui.bookings.map { it.id })
    }

    @Test
    fun `no bookings gives an empty list that is not loading`() = runTest {
        val ui = createVm().ui.value

        assertTrue(ui.bookings.isEmpty())
        assertFalse(ui.loading)
    }

    @Test
    fun `cancel marks the booking as cancelled`() = runTest {
        val b = booking(id = "b1", status = BookingStatus.CONFIRMED, paymentStatus = "PAID")
        repo.bookings.value = listOf(b)
        val vm = createVm()

        vm.cancel(b); runCurrent()

        assertEquals(listOf("b1"), repo.cancelled)
        assertEquals(BookingStatus.CANCELLED.name, vm.ui.value.bookings.single().status)
    }

    @Test
    fun `cancel failure shows the error`() = runTest {
        val b = booking(id = "b1")
        repo.bookings.value = listOf(b)
        repo.cancelError = Exception("Offline")
        val vm = createVm()

        vm.cancel(b); runCurrent()

        assertEquals("Offline", vm.ui.value.error)
    }
}
