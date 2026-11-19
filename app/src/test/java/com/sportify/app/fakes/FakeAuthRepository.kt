package com.sportify.app.fakes

import com.sportify.app.domain.model.User
import com.sportify.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    val signedIn = MutableStateFlow(false)
    val profile = MutableStateFlow<User?>(null)

    var signInResult: Result<Unit> = Result.success(Unit)
    var signUpResult: Result<Unit> = Result.success(Unit)
    var googleResult: Result<Unit> = Result.success(Unit)
    var resetResult: Result<Unit> = Result.success(Unit)
    var nameResult: Result<Unit> = Result.success(Unit)
    var photoResult: Result<Unit> = Result.success(Unit)

    val signInCalls = mutableListOf<Pair<String, String>>()
    val signUpCalls = mutableListOf<Triple<String, String, String>>()
    val nameCalls = mutableListOf<String>()
    val photoCalls = mutableListOf<String>()
    var signOutCalls = 0

    override val authState: Flow<Boolean> = signedIn
    override val currentUid: String? get() = if (signedIn.value) "uid-1" else null

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        signInCalls += email to password
        return signInResult
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> {
        signUpCalls += Triple(name, email, password)
        return signUpResult
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = googleResult
    override suspend fun sendPasswordReset(email: String): Result<Unit> = resetResult
    override fun signOut() { signOutCalls++ }
    override fun observeProfile(): Flow<User?> = profile

    override suspend fun updateName(name: String): Result<Unit> {
        nameCalls += name
        return nameResult
    }

    override suspend fun uploadProfilePhoto(uri: String): Result<Unit> {
        photoCalls += uri
        return photoResult
    }

    override suspend fun registerFcmToken(): Result<Unit> = Result.success(Unit)
}
