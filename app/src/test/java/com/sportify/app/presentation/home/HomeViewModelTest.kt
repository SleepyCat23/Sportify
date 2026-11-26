package com.sportify.app.presentation.home

import com.sportify.app.domain.model.SportType
import com.sportify.app.fakes.FakeVenueRepository
import com.sportify.app.testutil.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeVenueRepository().apply {
        venues.value = listOf(
            venue(id = "v1", name = "Ace Tennis Club", city = "Bandung", sports = listOf("TENNIS", "PADEL")),
            venue(id = "v2", name = "Slam Dunk Arena", city = "Jakarta", sports = listOf("BASKETBALL")),
            venue(id = "v3", name = "Quiet Court", city = "Bogor", sports = listOf("TENNIS"))
        )
        courts.value = listOf(
            court(id = "c1", venueId = "v1", price = 300_000.0),
            court(id = "c2", venueId = "v1", price = 250_000.0),
            court(id = "c3", venueId = "v2", price = 400_000.0)
        )
    }

    private fun TestScope.createVm(): HomeViewModel {
        val vm = HomeViewModel(repo)
        collectIn(vm.state)
        runCurrent()
        return vm
    }

    @Test
    fun `lists every venue once data has loaded`() = runTest {
        val state = createVm().state.value

        assertFalse(state.loading)
        assertEquals(listOf("v1", "v2", "v3"), state.venues.map { it.venue.id })
    }

    @Test
    fun `each venue shows its cheapest court price`() = runTest {
        val items = createVm().state.value.venues.associateBy { it.venue.id }

        assertEquals(250_000.0, items.getValue("v1").minPrice!!, 0.0)
        assertEquals(400_000.0, items.getValue("v2").minPrice!!, 0.0)
    }

    @Test
    fun `venue without courts has no price`() = runTest {
        val items = createVm().state.value.venues.associateBy { it.venue.id }

        assertNull(items.getValue("v3").minPrice)
    }

    @Test
    fun `sport filter keeps only venues offering that sport`() = runTest {
        val vm = createVm()

        vm.onSport(SportType.TENNIS)
        runCurrent()

        assertEquals(listOf("v1", "v3"), vm.state.value.venues.map { it.venue.id })
    }

    @Test
    fun `selecting no sport shows all venues again`() = runTest {
        val vm = createVm()
        vm.onSport(SportType.BASKETBALL); runCurrent()

        vm.onSport(null); runCurrent()

        assertEquals(3, vm.state.value.venues.size)
    }

    @Test
    fun `search matches venue name or city ignoring case`() = runTest {
        val vm = createVm()

        vm.onQuery("SLAM"); runCurrent()
        assertEquals(listOf("v2"), vm.state.value.venues.map { it.venue.id })

        vm.onQuery("bandung"); runCurrent()
        assertEquals(listOf("v1"), vm.state.value.venues.map { it.venue.id })
    }

    @Test
    fun `no match gives an empty list that is not loading`() = runTest {
        val vm = createVm()

        vm.onQuery("zzz"); runCurrent()

        assertTrue(vm.state.value.venues.isEmpty())
        assertFalse(vm.state.value.loading)
    }
}
