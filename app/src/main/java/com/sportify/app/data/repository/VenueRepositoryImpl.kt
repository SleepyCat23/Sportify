package com.sportify.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.sportify.app.domain.model.Court
import com.sportify.app.domain.model.Venue
import com.sportify.app.domain.repository.VenueRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenueRepositoryImpl @Inject constructor(private val db: FirebaseFirestore) : VenueRepository {

    override fun observeVenues(): Flow<List<Venue>> =
        db.collection("venues").orderBy("name").asFlow(Venue::class.java)

    override fun observeVenue(id: String): Flow<Venue?> =
        db.collection("venues").document(id).asFlow(Venue::class.java)

    override fun observeCourts(venueId: String): Flow<List<Court>> =
        db.collection("courts").whereEqualTo("venueId", venueId).asFlow(Court::class.java)

    /** Hours already confirmed or held (non-expired pending) for a court on a date. */
    override fun observeBookedHours(courtId: String, date: String): Flow<Set<Int>> = callbackFlow {
        val reg = db.collection("slots")
            .whereEqualTo("courtId", courtId)
            .whereEqualTo("date", date)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptySet()); return@addSnapshotListener }
                val now = Date()
                val hours = snap?.documents.orEmpty().mapNotNull { d ->
                    val status = d.getString("status")
                    val expires = d.getTimestamp("expiresAt")?.toDate()
                    val stale = status == "PENDING" && (expires == null || expires.before(now))
                    if (stale) null else d.getLong("hour")?.toInt()
                }.toSet()
                trySend(hours)
            }
        awaitClose { reg.remove() }
    }
}
