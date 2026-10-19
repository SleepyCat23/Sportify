package com.sportify.app.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.User
import com.sportify.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUi(val user: User? = null, val busy: Boolean = false, val message: String? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val local = MutableStateFlow(ProfileUi())

    val ui: StateFlow<ProfileUi> = combine(auth.observeProfile(), local) { u, l -> l.copy(user = u) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUi())

    fun saveName(name: String) = launchBusy("Profile updated") { auth.updateName(name.trim()) }
    fun uploadPhoto(uri: Uri) = launchBusy("Photo updated") { auth.uploadProfilePhoto(uri) }
    fun signOut() = auth.signOut()

    private fun launchBusy(ok: String, block: suspend () -> Result<Unit>) = viewModelScope.launch {
        local.value = ProfileUi(busy = true)
        block().onSuccess { local.value = ProfileUi(message = ok) }
            .onFailure { local.value = ProfileUi(message = it.message ?: "Failed") }
    }
}
