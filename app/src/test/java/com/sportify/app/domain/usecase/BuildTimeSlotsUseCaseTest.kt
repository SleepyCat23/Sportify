package com.sportify.app.domain.usecase

import com.sportify.app.testutil.venue
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BuildTimeSlotsUseCaseTest {
    private val build = BuildTimeSlotsUseCase()
    private val v = venue(open = 7, close = 22)
    private val today = LocalDate.of(2026, 11, 14)
    private val now = LocalDateTime.of(2026, 11, 14, 10, 30)

    @Test
    fun `future date offers every hour from opening until closing minus duration`() {
        val slots = build(v, today.plusDays(1), 1, emptySet(), now)
        assertEquals(15, slots.size)
        assertEquals(7, slots.first().hour)
        assertEquals(21, slots.last().hour)
        assertTrue(slots.all { it.available })
    }

    @Test
    fun `last slot ends exactly at closing time`() {
        val slots = build(v, today.plusDays(1), 3, emptySet(), now)
        assertEquals(19, slots.last().hour) // 19:00 + 3h = 22:00
        assertTrue(slots.none { it.hour == 20 })
    }

    @Test
    fun `today hours that already started are not available`() {
        val slots = build(v, today, 1, emptySet(), now).associate { it.hour to it.available }
        assertFalse(slots.getValue(10))
        assertTrue(slots.getValue(11))
    }

    @Test
    fun `past dates have no available slots`() {
        val slots = build(v, today.minusDays(1), 1, emptySet(), now)
        assertTrue(slots.none { it.available })
    }

    @Test
    fun `booked hours block every start time that would overlap them`() {
        val slots = build(v, today.plusDays(1), 2, setOf(13), now).associate { it.hour to it.available }
        assertFalse(slots.getValue(12)) // 12-14 overlaps 13
        assertFalse(slots.getValue(13))
        assertTrue(slots.getValue(11))  // 11-13 ends before 13 starts
        assertTrue(slots.getValue(14))
    }

    @Test
    fun `duration longer than the opening window gives no slots`() {
        val slots = build(venue(open = 8, close = 9), today.plusDays(1), 2, emptySet(), now)
        assertTrue(slots.isEmpty())
    }
}
