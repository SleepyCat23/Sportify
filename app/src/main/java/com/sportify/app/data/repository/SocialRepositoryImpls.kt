package com.sportify.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.sportify.app.domain.model.*
import com.sportify.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private fun FirebaseAuth.requireUid(): String = currentUser?.uid ?: error("Not signed in")

private suspend fun FirebaseFirestore.displayName(uid: String): String =
    collection("users").document(uid).get().await().getString("name").orEmpty().ifBlank { "Player" }

private fun List<Review>.newestFirst() = sortedByDescending { it.createdAt?.seconds ?: 0L }

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore, private val auth: FirebaseAuth
) : ReviewRepository {

    override fun observeReviews(venueId: String): Flow<List<Review>> =
        db.collection("reviews").whereEqualTo("venueId", venueId)
            .asFlow(Review::class.java).map { it.newestFirst() }

    override fun canReview(venueId: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return flowOf(false)
        return db.collection("bookings")
            .whereEqualTo("userId", uid)
            .whereEqualTo("venueId", venueId)
            .whereEqualTo("paymentStatus", "PAID")
            .limit(1)
            .asFlow(Booking::class.java)
            .map { it.isNotEmpty() }
    }

    override suspend fun addReview(venueId: String, venueName: String, rating: Int, comment: String): Result<Unit> =
        runCatching {
            val uid = auth.requireUid()
            require(rating in 1..5) { "Pick a rating from 1 to 5" }
            db.collection("reviews").document("${venueId}_$uid").set(mapOf(
                "venueId" to venueId, "venueName" to venueName,
                "userId" to uid, "userName" to db.displayName(uid),
                "rating" to rating, "comment" to comment,
                "createdAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
}

@Singleton
class OpenPlayRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore, private val auth: FirebaseAuth
) : OpenPlayRepository {
    private val col get() = db.collection("openPlays")

    override fun observeOpenPlays(): Flow<List<OpenPlay>> =
        col.asFlow(OpenPlay::class.java).map { l -> l.sortedWith(compareBy({ it.date }, { it.startHour })) }

    override suspend fun create(
        title: String, sport: String, venueName: String, date: String, startHour: Int, maxPlayers: Int
    ): Result<Unit> = runCatching {
        val uid = auth.requireUid()
        val name = db.displayName(uid)
        col.add(mapOf(
            "hostId" to uid, "hostName" to name, "title" to title, "sport" to sport,
            "venueName" to venueName, "date" to date, "startHour" to startHour,
            "maxPlayers" to maxPlayers, "members" to listOf(uid),
            "requests" to emptyList<String>(), "names" to mapOf(uid to name),
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        Unit
    }

    override suspend fun requestJoin(playId: String): Result<Unit> = runCatching {
        val uid = auth.requireUid()
        col.document(playId).update(mapOf(
            "requests" to FieldValue.arrayUnion(uid), "names.$uid" to db.displayName(uid)
        )).await()
        Unit
    }

    override suspend fun leave(playId: String): Result<Unit> = runCatching {
        val uid = auth.requireUid()
        col.document(playId).update(mapOf(
            "requests" to FieldValue.arrayRemove(uid), "members" to FieldValue.arrayRemove(uid)
        )).await()
        Unit
    }

    override suspend fun decide(playId: String, uid: String, approve: Boolean): Result<Unit> = runCatching {
        val ref = col.document(playId)
        db.runTransaction { tx ->
            val play = tx.get(ref).toObject(OpenPlay::class.java) ?: return@runTransaction null
            val canJoin = approve && !play.isFull
            val updates = mutableMapOf<String, Any>("requests" to FieldValue.arrayRemove(uid))
            if (canJoin) updates["members"] = FieldValue.arrayUnion(uid)
            tx.update(ref, updates)
            null
        }.await()
        Unit
    }

    override suspend fun delete(playId: String): Result<Unit> = runCatching {
        col.document(playId).delete().await(); Unit
    }
}

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore, private val auth: FirebaseAuth
) : MatchRepository {

    override fun observeMyPlayer(): Flow<Player?> {
        val uid = auth.currentUser?.uid ?: return flowOf(null)
        return db.collection("players").document(uid).asFlow(Player::class.java)
    }

    override suspend fun saveMyPlayer(level: String, sport: String): Result<Unit> = runCatching {
        val uid = auth.requireUid()
        val user = db.collection("users").document(uid).get().await()
        db.collection("players").document(uid).set(mapOf(
            "name" to user.getString("name").orEmpty().ifBlank { "Player" },
            "photoUrl" to user.getString("photoUrl").orEmpty(),
            "level" to level, "sport" to sport
        )).await()
        Unit
    }

    override fun observePlayers(): Flow<List<Player>> {
        val uid = auth.currentUser?.uid
        return db.collection("players").asFlow(Player::class.java).map { l -> l.filter { it.uid != uid } }
    }

    override fun observeInvites(): Flow<List<Invite>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        val received = db.collection("invites").whereEqualTo("toUid", uid).asFlow(Invite::class.java)
        val sent = db.collection("invites").whereEqualTo("fromUid", uid).asFlow(Invite::class.java)
        return combine(received, sent) { a, b -> (a + b).sortedByDescending { it.createdAt?.seconds ?: 0L } }
    }

    override suspend fun invite(to: Player): Result<Unit> = runCatching {
        val uid = auth.requireUid()
        db.collection("invites").add(mapOf(
            "fromUid" to uid, "fromName" to db.displayName(uid),
            "toUid" to to.uid, "toName" to to.name, "sport" to to.sport,
            "status" to "PENDING", "createdAt" to FieldValue.serverTimestamp()
        )).await()
        Unit
    }

    override suspend fun respond(invite: Invite, accept: Boolean): Result<Unit> = runCatching {
        db.collection("invites").document(invite.id)
            .update("status", if (accept) "ACCEPTED" else "DECLINED").await()
        Unit
    }

    override fun observeMyResults(): Flow<List<MatchResult>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return db.collection("matches").whereEqualTo("userId", uid).asFlow(MatchResult::class.java)
            .map { l -> l.sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE } }
    }

    override suspend fun addResult(opponent: String, sport: String, myScore: Int, theirScore: Int): Result<Unit> =
        runCatching {
            require(myScore != theirScore) { "A match needs a winner" }
            db.collection("matches").add(mapOf(
                "userId" to auth.requireUid(), "opponent" to opponent, "sport" to sport,
                "myScore" to myScore, "theirScore" to theirScore,
                "createdAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
}

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore, private val functions: FirebaseFunctions
) : AdminRepository {

    override fun observeAllBookings(): Flow<List<Booking>> =
        db.collection("bookings").asFlow(Booking::class.java)
            .map { l -> l.sortedByDescending { it.createdAt?.seconds ?: 0L } }

    override fun observeAllReviews(): Flow<List<Review>> =
        db.collection("reviews").asFlow(Review::class.java).map { it.newestFirst() }

    override suspend fun addVenue(venue: Venue, firstCourt: Court): Result<Unit> = runCatching {
        val ref = db.collection("venues").document()
        val batch = db.batch()
        batch.set(ref, mapOf(
            "name" to venue.name, "description" to venue.description, "address" to venue.address,
            "city" to venue.city, "imageUrl" to venue.imageUrl, "sports" to venue.sports,
            "rating" to 0.0, "reviewCount" to 0,
            "openHour" to venue.openHour, "closeHour" to venue.closeHour
        ))
        batch.set(db.collection("courts").document("${ref.id}_c1"), mapOf(
            "venueId" to ref.id, "name" to firstCourt.name, "sport" to firstCourt.sport,
            "pricePerHour" to firstCourt.pricePerHour, "currency" to firstCourt.currency
        ))
        batch.commit().await()
        Unit
    }

    override suspend fun deleteVenue(venueId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val upcoming = db.collection("bookings").whereEqualTo("venueId", venueId).get().await()
            .toObjects(Booking::class.java)
            .filter { it.status != BookingStatus.CANCELLED.name && it.endMillis > now }
        upcoming.forEach { b ->
            if (b.paymentStatus == "PAID") refund(b.id).getOrThrow()
            else db.collection("bookings").document(b.id).update("status", BookingStatus.CANCELLED.name).await()
        }
        val batch = db.batch()
        db.collection("courts").whereEqualTo("venueId", venueId).get().await().documents
            .forEach { batch.delete(it.reference) }
        db.collection("reviews").whereEqualTo("venueId", venueId).get().await().documents
            .forEach { batch.delete(it.reference) }
        batch.delete(db.collection("venues").document(venueId))
        batch.commit().await()
        Unit
    }

    override suspend fun updateCourtPrice(courtId: String, price: Double): Result<Unit> = runCatching {
        require(price > 0) { "Price must be greater than zero" }
        db.collection("courts").document(courtId).update("pricePerHour", price).await(); Unit
    }

    override suspend fun updateHours(venueId: String, openHour: Int, closeHour: Int): Result<Unit> = runCatching {
        require(openHour in 0..23 && closeHour in 1..24 && openHour < closeHour) { "Invalid opening hours" }
        db.collection("venues").document(venueId)
            .update(mapOf("openHour" to openHour, "closeHour" to closeHour)).await()
        Unit
    }

    /** Server-side: issues the Stripe refund, cancels the booking and frees its slots. */
    override suspend fun refund(bookingId: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("adminRefund").call(mapOf("bookingId" to bookingId)).await(); Unit
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> = runCatching {
        db.collection("reviews").document(reviewId).delete().await(); Unit
    }
}
