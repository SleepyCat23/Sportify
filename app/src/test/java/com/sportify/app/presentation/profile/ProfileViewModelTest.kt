package com.sportify.app.presentation.profile

import com.sportify.app.domain.model.User
import com.sportify.app.fakes.FakeAuthRepository
import com.sportify.app.testutil.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeAuthRepository()

    private fun TestScope.createVm(): ProfileViewModel {
        val vm = ProfileViewModel(repo)
        collectIn(vm.ui)
        runCurrent()
        return vm
    }

    @Test
    fun `exposes the signed in user's profile`() = runTest {
        repo.profile.value = User(uid = "u1", name = "Budi", email = "budi@mail.com")

        assertEquals("Budi", createVm().ui.value.user?.name)
    }

    @Test
    fun `saving a name trims it and confirms`() = runTest {
        val vm = createVm()

        vm.saveName("  Budi Santoso  "); runCurrent()

        assertEquals(listOf("Budi Santoso"), repo.nameCalls)
        assertEquals("Profile updated", vm.ui.value.message)
    }

    @Test
    fun `photo upload failure shows the error message`() = runTest {
        repo.photoResult = Result.failure(Exception("Upload failed"))
        val vm = createVm()

        vm.uploadPhoto("content://media/picker/1"); runCurrent()

        assertEquals(listOf("content://media/picker/1"), repo.photoCalls)
        assertEquals("Upload failed", vm.ui.value.message)
    }

    @Test
    fun `sign out is passed to the repository`() = runTest {
        createVm().signOut()

        assertEquals(1, repo.signOutCalls)
    }
}
