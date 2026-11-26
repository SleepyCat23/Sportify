package com.sportify.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUi(val loading: Boolean = false, val error: String? = null, val info: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AuthUi())
    val ui: StateFlow<AuthUi> = _ui

    private fun run(block: suspend () -> Result<Unit>) = viewModelScope.launch {
        _ui.value = AuthUi(loading = true)
        block().onSuccess { _ui.value = AuthUi() }
            .onFailure { _ui.value = AuthUi(error = it.message ?: "Something went wrong") }
    }

    fun signIn(email: String, pw: String) {
        if (email.isBlank() || pw.isBlank()) { _ui.value = AuthUi(error = "Enter email and password"); return }
        run { auth.signIn(email, pw) }
    }

    fun signUp(name: String, email: String, pw: String) {
        when {
            name.isBlank() || email.isBlank() -> _ui.value = AuthUi(error = "Fill in all fields")
            pw.length < 6 -> _ui.value = AuthUi(error = "Password must be at least 6 characters")
            else -> run { auth.signUp(name.trim(), email, pw) }
        }
    }

    fun google(idToken: String) = run { auth.signInWithGoogle(idToken) }

    fun resetPassword(email: String) {
        if (email.isBlank()) { _ui.value = AuthUi(error = "Enter your email first"); return }
        viewModelScope.launch {
            auth.sendPasswordReset(email)
                .onSuccess { _ui.value = AuthUi(info = "Password reset email sent") }
                .onFailure { _ui.value = AuthUi(error = it.message) }
        }
    }

    fun setError(msg: String) = _ui.update { it.copy(error = msg, loading = false) }
}
