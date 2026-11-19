package com.sportify.app.fakes

import com.sportify.app.domain.model.Court
import com.sportify.app.domain.model.Venue
import com.sportify.app.domain.repository.VenueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for Firestore. Set the public flows to drive any scenario. */
class FakeVenueRepository : VenueRepository {
    val venues = MutableStateFlow<List<Venue>>(emptyList())
    val courts = MutableStateFlow<List<Court>>(emptyList())
    val bookedHours = MutableStateFlow<Set<Int>>(emptySet())

    override fun observeVenues(): Flow<List<Venue>> = venues
    override fun observeVenue(id: String): Flow<Venue?> = venues.map { list -> list.firstOrNull { it.id == id } }
    override fun observeCourts(venueId: String): Flow<List<Court>> = courts.map { list -> list.filter { it.venueId == venueId } }
    override fun observeAllCourts(): Flow<List<Court>> = courts
    override fun observeBookedHours(courtId: String, date: String): Flow<Set<Int>> = bookedHours
}
