package com.sportify.app.presentation.venue

import androidx.lifecycle.SavedStateHandle
import com.sportify.app.domain.model.SlotUnavailableException
import com.sportify.app.domain.usecase.BuildTimeSlotsUseCase
import com.sportify.app.fakes.FakeBookingRepository
import com.sportify.app.fakes.FakeVenueRepository
import com.sportify.app.testutil.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class VenueDetailViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val venues = FakeVenueRepository()
    private val bookings = FakeBookingRepository()
    private val tomorrow = LocalDate.now().plusDays(1)

    private fun TestScope.createVm(): VenueDetailViewModel {
        venues.venues.value = listOf(venue(open = 7, close = 22))
        venues.courts.value = listOf(court())
        val vm = VenueDetailViewModel(
            SavedStateHandle(mapOf("venueId" to "v1")), venues, bookings, BuildTimeSlotsUseCase()
        )
        collectIn(vm.venue); collectIn(vm.courts); collectIn(vm.slots)
        runCurrent()
        vm.selectCourt("c1"); vm.selectDate(tomorrow)
        runCurrent()
        return vm
    }

    @Test
    fun `slots cover opening hours until closing minus duration`() = runTest {
        val slots = createVm().slots.value

        assertEquals(7, slots.first().hour)
        assertEquals(21, slots.last().hour)
        assertTrue(slots.all { it.available })
    }

    @Test
    fun `hours that are already booked are not available`() = runTest {
        val vm = createVm()

        venues.bookedHours.value = setOf(10)
        runCurrent()

        val byHour = vm.slots.value.associate { it.hour to it.available }
        assertFalse(byHour.getValue(10))
        assertTrue(byHour.getValue(9))
    }

    @Test
    fun `a longer duration also blocks the start time just before a booked hour`() = runTest {
        val vm = createVm()
        venues.bookedHours.value = setOf(10)

        vm.selectDuration(2); runCurrent()

        val byHour = vm.slots.value.associate { it.hour to it.available }
        assertFalse(byHour.getValue(9))  // 9-11 overlaps 10
        assertTrue(byHour.getValue(8))   // 8-10 does not
    }

    @Test
    fun `changing the date clears the chosen start time`() = runTest {
        val vm = createVm()
        vm.selectHour(18)
        assertEquals(18, vm.form.value.startHour)

        vm.selectDate(tomorrow.plusDays(1))

        assertNull(vm.form.value.startHour)
    }

    @Test
    fun `book creates a pending booking for the chosen slot and returns its id`() = runTest {
        val vm = createVm()
        vm.selectHour(18)
        var newId: String? = null

        vm.book { newId = it }
        runCurrent()

        val created = bookings.created.single()
        assertEquals("c1", created.courtId)
        assertEquals(tomorrow, created.date)
        assertEquals(18, created.startHour)
        assertEquals("booking-1", newId)
        assertFalse(vm.form.value.submitting)
    }

    @Test
    fun `book failure shows the message and clears the start time`() = runTest {
        val vm = createVm()
        vm.selectHour(18)
        bookings.createError = SlotUnavailableException()

        vm.book { fail("must not navigate when booking fails") }
        runCurrent()

        assertEquals(SlotUnavailableException().message, vm.form.value.error)
        assertNull(vm.form.value.startHour)
    }

    @Test
    fun `book does nothing until a start time is chosen`() = runTest {
        val vm = createVm()

        vm.book { fail("no start time chosen") }
        runCurrent()

        assertTrue(bookings.created.isEmpty())
    }
}
