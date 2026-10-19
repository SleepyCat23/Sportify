package com.sportify.app.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.sportify.app.domain.model.User
import com.sportify.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val messaging: FirebaseMessaging
) : AuthRepository {

    private val users get() = db.collection("users")

    override val authState: Flow<Boolean> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    override val currentUid: String? get() = auth.currentUser?.uid

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> = runCatching {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Account could not be created")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        ensureUserDoc(user.uid, name, email.trim(), "")
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(cred).await().user ?: error("Google sign-in failed")
        ensureUserDoc(user.uid, user.displayName.orEmpty(), user.email.orEmpty(), user.photoUrl?.toString().orEmpty())
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeProfile(): Flow<User?> = authState.flatMapLatest { signedIn ->
        val uid = auth.currentUser?.uid
        if (!signedIn || uid == null) flowOf(null) else users.document(uid).asFlow(User::class.java)
    }

    override suspend fun updateName(name: String): Result<Unit> = runCatching {
        val uid = currentUid ?: error("Not signed in")
        users.document(uid).set(mapOf("name" to name), SetOptions.merge()).await()
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())?.await()
        Unit
    }

    override suspend fun uploadProfilePhoto(uri: Uri): Result<Unit> = runCatching {
        val uid = currentUid ?: error("Not signed in")
        val ref = storage.reference.child("profile_photos/$uid.jpg")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        users.document(uid).set(mapOf("photoUrl" to url), SetOptions.merge()).await()
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(url)).build())?.await()
        Unit
    }

    override suspend fun registerFcmToken(): Result<Unit> = runCatching {
        val uid = currentUid ?: return@runCatching
        val token = messaging.token.await()
        users.document(uid).set(mapOf("fcmToken" to token), SetOptions.merge()).await()
        Unit
    }

    private suspend fun ensureUserDoc(uid: String, name: String, email: String, photo: String) {
        val ref = users.document(uid)
        if (!ref.get().await().exists()) {
            ref.set(
                mapOf(
                    "name" to name, "email" to email, "photoUrl" to photo,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
