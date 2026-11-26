package com.sportify.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sportify.app.domain.repository.FavoriteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FavoriteRepository {

    private fun favorites() = db.collection("users").document(
        auth.currentUser?.uid ?: error("Not signed in")
    ).collection("favorites")

    override fun observeFavoriteVenueIds(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val registration = db.collection("users").document(uid).collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { it.id }.toSet())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setFavorite(venueId: String, favorite: Boolean): Result<Unit> = runCatching {
        val ref = favorites().document(venueId)
        if (favorite) ref.set(mapOf("venueId" to venueId)).await() else ref.delete().await()
        Unit
    }
}
