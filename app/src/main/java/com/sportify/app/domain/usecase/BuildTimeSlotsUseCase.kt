package com.sportify.app.domain.usecase

import com.sportify.app.domain.model.TimeSlot
import com.sportify.app.domain.model.Venue
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Pure business rule: which start times can be booked for a given duration. */
class BuildTimeSlotsUseCase @Inject constructor() {
    operator fun invoke(
        venue: Venue,
        date: LocalDate,
        duration: Int,
        bookedHours: Set<Int>,
        now: LocalDateTime = LocalDateTime.now()
    ): List<TimeSlot> =
        (venue.openHour..(venue.closeHour - duration)).map { start ->
            val inPast = date.isBefore(now.toLocalDate()) ||
                (date == now.toLocalDate() && start <= now.hour)
            val overlaps = (start until start + duration).any { it in bookedHours }
            TimeSlot(start, available = !inPast && !overlaps)
        }
}
