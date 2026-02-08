package com.example.kosmos.features.auth.presentation

import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.data.repository.UserRepository
import com.example.kosmos.testutil.TestDispatcherRule
import com.example.kosmos.testutil.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AuthViewModel.
 * Uses RobolectricTestRunner so android.util.Patterns is initialized.
 * Uses UnconfinedTestDispatcher so coroutines run synchronously.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuthViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: AuthViewModel

    private val testUser = TestFixtures.user(id = "user-1", email = "user@test.com")

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        every { authRepository.userFlow } returns MutableStateFlow(null)
        every { authRepository.getCurrentUser() } returns null
        every { authRepository.isUserLoggedIn() } returns false
        every { authRepository.getSavedEmail() } returns ""
        every { authRepository.isRememberMeEnabled() } returns false

        viewModel = AuthViewModel(authRepository, userRepository)
    }

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial state - isLoggedIn is false`() {
        assertThat(viewModel.uiState.value.isLoggedIn).isFalse()
    }

    @Test
    fun `initial state - error is null`() {
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test
    fun `initial state - currentUser is null`() {
        assertThat(viewModel.uiState.value.currentUser).isNull()
    }

    @Test
    fun `init - user already logged in sets isLoggedIn to true`() {
        every { authRepository.isUserLoggedIn() } returns true
        every { authRepository.getCurrentUser() } returns testUser
        every { authRepository.userFlow } returns MutableStateFlow(testUser)

        val vm = AuthViewModel(authRepository, userRepository)
        assertThat(vm.uiState.value.isLoggedIn).isTrue()
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    fun `login success - sets isLoggedIn true and currentUser`() = runTest {
        coEvery { authRepository.signInWithEmailAndPassword(any(), any(), any()) } returns
            Result.success(testUser)

        viewModel.login("user@test.com", "Password1")

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isTrue()
        assertThat(state.currentUser).isEqualTo(testUser)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `login blank email - sets error and does not call repository`() = runTest {
        viewModel.login("", "Password1")

        assertThat(viewModel.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { authRepository.signInWithEmailAndPassword(any(), any(), any()) }
    }

    @Test
    fun `login blank password - sets error and does not call repository`() = runTest {
        viewModel.login("user@test.com", "")

        assertThat(viewModel.uiState.value.error).isNotNull()
        coVerify(exactly = 0) { authRepository.signInWithEmailAndPassword(any(), any(), any()) }
    }

    @Test
    fun `login repository failure - sets error message`() = runTest {
        coEvery { authRepository.signInWithEmailAndPassword(any(), any(), any()) } returns
            Result.failure(Exception("Invalid credentials"))

        viewModel.login("user@test.com", "Password1")

        assertThat(viewModel.uiState.value.error).isNotNull()
        assertThat(viewModel.uiState.value.isLoggedIn).isFalse()
    }

    @Test
    fun `login - isLoading becomes false after completion`() = runTest {
        coEvery { authRepository.signInWithEmailAndPassword(any(), any(), any()) } returns
            Result.success(testUser)

        viewModel.login("user@test.com", "Password1")

        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Test
    fun `logout - resets state to default`() = runTest {
        // First log in
        coEvery { authRepository.signInWithEmailAndPassword(any(), any(), any()) } returns
            Result.success(testUser)
        viewModel.login("user@test.com", "Password1")

        // Now logout
        coEvery { authRepository.signOut() } returns Result.success(Unit)
        viewModel.logout()

        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isFalse()
        assertThat(state.currentUser).isNull()
        assertThat(state.error).isNull()
    }

    // ─── checkUsernameAvailability ────────────────────────────────────────────

    @Test
    fun `checkUsernameAvailability - less than 3 chars does not call repo`() = runTest {
        viewModel.checkUsernameAvailability("ab")

        coVerify(exactly = 0) { userRepository.checkUsernameExists(any()) }
        assertThat(viewModel.uiState.value.isUsernameAvailable).isNull()
    }

    @Test
    fun `checkUsernameAvailability - username taken sets isUsernameAvailable false`() = runTest {
        coEvery { userRepository.checkUsernameExists("taken") } returns true

        viewModel.checkUsernameAvailability("taken")
        dispatcherRule.scheduler.advanceTimeBy(600)

        assertThat(viewModel.uiState.value.isUsernameAvailable).isFalse()
    }

    @Test
    fun `checkUsernameAvailability - exception sets isUsernameAvailable null`() = runTest {
        coEvery { userRepository.checkUsernameExists(any()) } throws Exception("Network error")

        viewModel.checkUsernameAvailability("available")
        dispatcherRule.scheduler.advanceTimeBy(600)

        assertThat(viewModel.uiState.value.isUsernameAvailable).isNull()
    }

    // ─── sendPasswordResetEmail ───────────────────────────────────────────────

    @Test
    fun `sendPasswordResetEmail - blank sets passwordResetError`() {
        viewModel.sendPasswordResetEmail("")

        assertThat(viewModel.uiState.value.passwordResetError).isNotNull()
    }

    @Test
    fun `sendPasswordResetEmail - invalid email sets passwordResetError`() {
        viewModel.sendPasswordResetEmail("not-an-email")

        assertThat(viewModel.uiState.value.passwordResetError).isNotNull()
    }

    @Test
    fun `sendPasswordResetEmail - success sets passwordResetSent true`() = runTest {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

        viewModel.sendPasswordResetEmail("user@test.com")

        assertThat(viewModel.uiState.value.passwordResetSent).isTrue()
        assertThat(viewModel.uiState.value.passwordResetError).isNull()
    }

    // ─── clearError ───────────────────────────────────────────────────────────

    @Test
    fun `clearError - clears error field`() = runTest {
        // Trigger an error
        viewModel.login("", "Password1")
        assertThat(viewModel.uiState.value.error).isNotNull()

        viewModel.clearError()

        assertThat(viewModel.uiState.value.error).isNull()
    }
}
