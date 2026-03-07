package com.retailiq.datasage.ui.auth

import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.AuthApiService
import com.retailiq.datasage.data.api.AuthTokens
import com.retailiq.datasage.data.api.ForgotPasswordRequest
import com.retailiq.datasage.data.api.LoginRequest
import com.retailiq.datasage.data.api.LogoutRequest
import com.retailiq.datasage.data.api.OtpVerifyRequest
import com.retailiq.datasage.data.api.RefreshRequest
import com.retailiq.datasage.data.api.RegisterRequest
import com.retailiq.datasage.data.api.ResetPasswordRequest
import com.retailiq.datasage.data.api.SimpleMessage
import com.retailiq.datasage.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    // ─── hasToken ────────────────────────────────────────────────────────────

    @Test
    fun hasToken_returnsTrueWhenTokenExists() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore(access = "token")))
        assertTrue(vm.hasToken())
    }

    @Test
    fun hasToken_returnsFalseWhenNoToken() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore()))
        assertFalse(vm.hasToken())
    }

    // ─── validateSession ─────────────────────────────────────────────────────

    @Test
    fun validateSession_delegatesToRepositoryAndRefreshesTokens() = runTest {
        val tokenStore = FakeTokenStore(access = "old", refresh = "old-ref")
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), tokenStore))

        val result = vm.validateSession()

        assertTrue(result)
        assertEquals("access-2", tokenStore.access)
    }

    @Test
    fun validateSession_returnsFalseWhenNoTokens() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore()))

        assertFalse(vm.validateSession())
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    fun login_setsSuccessStateAndCallsOnSuccess() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore()))
        var receivedRole: String? = null

        vm.login("9999999999", "secret") { role -> receivedRole = role }
        advanceUntilIdle()

        assertEquals("owner", receivedRole)
        assertTrue(vm.uiState.value is AuthUiState.Success)
    }

    @Test
    fun login_setsErrorStateOnFailure() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(loginSuccess = false), FakeTokenStore()))

        vm.login("9999999999", "wrong") { }
        advanceUntilIdle()

        assertTrue(vm.uiState.value is AuthUiState.Error)
    }

    // ─── verifyOtp (auto-login) ──────────────────────────────────────────────

    @Test
    fun verifyOtp_savesTokensAndCallsOnSuccessWithRole() = runTest {
        val tokenStore = FakeTokenStore()
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), tokenStore))
        var receivedRole: String? = null

        vm.verifyOtp("9999999999", "123456") { role -> receivedRole = role }
        advanceUntilIdle()

        assertEquals("owner", receivedRole)
        assertEquals("access-otp", tokenStore.access)
        assertEquals("refresh-otp", tokenStore.refresh)
        assertTrue(vm.uiState.value is AuthUiState.Success)
    }

    @Test
    fun verifyOtp_setsErrorStateOnFailure() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(verifyOtpSuccess = false), FakeTokenStore()))

        vm.verifyOtp("9999999999", "000000") { }
        advanceUntilIdle()

        assertTrue(vm.uiState.value is AuthUiState.Error)
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    fun register_setsSuccessStateAndCallsOnSuccess() = runTest {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore()))
        var called = false

        vm.register("Test", "9999999999", "test@example.com", "Store", "password1") { called = true }
        advanceUntilIdle()

        assertTrue(called)
        assertTrue(vm.uiState.value is AuthUiState.Success)
    }

    // ─── role / setupComplete / logout ────────────────────────────────────────

    @Test
    fun role_returnsValueFromTokenStore() {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore(_role = "owner")))
        assertEquals("owner", vm.role())
    }

    @Test
    fun isSetupComplete_returnsValueFromTokenStore() {
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), FakeTokenStore(setup = true)))
        assertTrue(vm.isSetupComplete())
    }

    @Test
    fun logout_clearsTokens() {
        val tokenStore = FakeTokenStore(access = "a", refresh = "r")
        val vm = AuthViewModel(AuthRepository(FakeAuthApi(), tokenStore))

        vm.logout()

        assertFalse(vm.hasToken())
    }

    // ─── Fakes ───────────────────────────────────────────────────────────────

    private class FakeAuthApi(
        private val loginSuccess: Boolean = true,
        private val verifyOtpSuccess: Boolean = true
    ) : AuthApiService {
        override suspend fun register(request: RegisterRequest) = ApiResponse(true, SimpleMessage("OTP sent"), null, null)

        override suspend fun verifyOtp(request: OtpVerifyRequest): ApiResponse<AuthTokens> =
            if (verifyOtpSuccess) ApiResponse(true, AuthTokens("access-otp", "refresh-otp", 1, "owner", 1), null, null)
            else ApiResponse(false, null, ApiError("INVALID_OTP", "Invalid or expired OTP"), null)

        override suspend fun login(request: LoginRequest): ApiResponse<AuthTokens> =
            if (loginSuccess) ApiResponse(true, AuthTokens("a", "b", 1, "owner", 1), null, null)
            else ApiResponse(false, null, ApiError("INVALID_CREDENTIALS", "Invalid credentials"), null)

        override suspend fun refresh(request: RefreshRequest) = ApiResponse(true, AuthTokens("access-2", "refresh-2", 1, "owner", 1), null, null)
        override suspend fun logout(request: LogoutRequest?) = ApiResponse(true, SimpleMessage("ok"), null, null)
        override suspend fun forgotPassword(request: ForgotPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null)
        override suspend fun resetPassword(request: ResetPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null)
    }

    private data class FakeTokenStore(
        var access: String? = null,
        var refresh: String? = null,
        var _role: String = "staff",
        var setup: Boolean = false
    ) : TokenStore {
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken; _role = "owner" }
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun getRole(): String = _role
        override fun isChainOwner(): Boolean = false
        override fun getChainGroupId(): String? = null
        override fun isSetupComplete(): Boolean = setup
        override fun markSetupComplete() { setup = true }
        override fun clearTokens() { access = null; refresh = null }
    }
}
