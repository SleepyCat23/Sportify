package com.sportify.app.presentation.auth

import com.sportify.app.fakes.FakeAuthRepository
import com.sportify.app.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeAuthRepository()
    private lateinit var vm: AuthViewModel

    @Before fun setUp() { vm = AuthViewModel(repo) }

    @Test
    fun `sign in with blank fields shows an error and does not call the repository`() = runTest {
        vm.signIn("", "")
        runCurrent()

        assertEquals("Enter email and password", vm.ui.value.error)
        assertTrue(repo.signInCalls.isEmpty())
    }

    @Test
    fun `successful sign in clears loading and error`() = runTest {
        vm.signIn("budi@mail.com", "secret1")
        runCurrent()

        assertEquals(listOf("budi@mail.com" to "secret1"), repo.signInCalls)
        assertFalse(vm.ui.value.loading)
        assertNull(vm.ui.value.error)
    }

    @Test
    fun `failed sign in shows the message from the repository`() = runTest {
        repo.signInResult = Result.failure(Exception("The password is invalid"))

        vm.signIn("budi@mail.com", "wrong-pass")
        runCurrent()

        assertEquals("The password is invalid", vm.ui.value.error)
        assertFalse(vm.ui.value.loading)
    }

    @Test
    fun `sign up requires a name`() = runTest {
        vm.signUp("", "budi@mail.com", "secret1")
        runCurrent()

        assertEquals("Fill in all fields", vm.ui.value.error)
        assertTrue(repo.signUpCalls.isEmpty())
    }

    @Test
    fun `sign up rejects a password shorter than 6 characters`() = runTest {
        vm.signUp("Budi", "budi@mail.com", "12345")
        runCurrent()

        assertEquals("Password must be at least 6 characters", vm.ui.value.error)
        assertTrue(repo.signUpCalls.isEmpty())
    }

    @Test
    fun `sign up accepts exactly 6 characters and trims the name`() = runTest {
        vm.signUp("  Budi  ", "budi@mail.com", "123456")
        runCurrent()

        assertEquals(Triple("Budi", "budi@mail.com", "123456"), repo.signUpCalls.single())
        assertNull(vm.ui.value.error)
    }

    @Test
    fun `reset password without an email asks for the email`() = runTest {
        vm.resetPassword("")
        runCurrent()

        assertEquals("Enter your email first", vm.ui.value.error)
    }

    @Test
    fun `reset password success shows a confirmation`() = runTest {
        vm.resetPassword("budi@mail.com")
        runCurrent()

        assertEquals("Password reset email sent", vm.ui.value.info)
        assertNull(vm.ui.value.error)
    }
}
