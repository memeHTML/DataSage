package com.retailiq.datasage.data.repository

import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.ApiResponse
import com.retailiq.datasage.data.api.AuthApiService
import com.retailiq.datasage.data.api.AuthTokens
import com.retailiq.datasage.data.api.ApiError
import com.retailiq.datasage.data.api.ForgotPasswordRequest
import com.retailiq.datasage.data.api.LoginRequest
import com.retailiq.datasage.data.api.LogoutRequest
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.OtpVerifyRequest
import com.retailiq.datasage.data.api.RefreshRequest
import com.retailiq.datasage.data.api.RegisterRequest
import com.retailiq.datasage.data.api.ResetPasswordRequest
import com.retailiq.datasage.data.api.SimpleMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    fun login_savesTokensAndReturnsRole() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repo.login("9999999999", "secret")

        assertTrue(result is NetworkResult.Success)
        assertEquals("owner", (result as NetworkResult.Success).data)
        assertEquals("access-1", tokenStore.access)
        assertEquals("refresh-1", tokenStore.refresh)
    }

    @Test
    fun login_returnsErrorOnFailure() = runBlocking {
        val api = FakeAuthApi(loginSuccess = false)
        val repo = AuthRepository(api, FakeTokenStore())

        val result = repo.login("9999999999", "wrong")

        assertTrue(result is NetworkResult.Error)
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    fun register_returnsSuccess() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repo.register("Test User", "9999999999", "test@example.com", "My Store", "secret123")

        assertTrue(result is NetworkResult.Success)
        // register should NOT save tokens
        assertNull(tokenStore.access)
    }

    @Test
    fun register_returnsErrorOnFailure() = runBlocking {
        val api = FakeAuthApi(registerSuccess = false)
        val repo = AuthRepository(api, FakeTokenStore())

        val result = repo.register("Test User", "9999999999", "test@example.com", "My Store", "secret123")

        assertTrue(result is NetworkResult.Error)
    }

    // ─── verifyOtp ───────────────────────────────────────────────────────────

    @Test
    fun verifyOtp_savesTokensAndReturnsRole() = runBlocking {
        val tokenStore = FakeTokenStore()
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        val result = repo.verifyOtp("9999999999", "123456")

        assertTrue(result is NetworkResult.Success)
        assertEquals("owner", (result as NetworkResult.Success).data)
        assertEquals("access-otp", tokenStore.access)
        assertEquals("refresh-otp", tokenStore.refresh)
    }

    @Test
    fun verifyOtp_returnsErrorOnFailure() = runBlocking {
        val api = FakeAuthApi(verifyOtpSuccess = false)
        val repo = AuthRepository(api, FakeTokenStore())

        val result = repo.verifyOtp("9999999999", "000000")

        assertTrue(result is NetworkResult.Error)
    }

    // ─── validateSession ─────────────────────────────────────────────────────

    @Test
    fun validateSession_returnsFalse_whenNoAccessToken() = runBlocking {
        val tokenStore = FakeTokenStore(access = null, refresh = "ref")
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        assertFalse(repo.validateSession())
    }

    @Test
    fun validateSession_returnsFalse_whenNoRefreshToken() = runBlocking {
        val tokenStore = FakeTokenStore(access = "acc", refresh = null)
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        assertFalse(repo.validateSession())
    }

    @Test
    fun validateSession_refreshesTokensAndReturnsTrue() = runBlocking {
        val tokenStore = FakeTokenStore(access = "old-access", refresh = "old-refresh")
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        assertTrue(repo.validateSession())
        assertEquals("access-2", tokenStore.access)
        assertEquals("refresh-2", tokenStore.refresh)
    }

    @Test
    fun validateSession_returnsFalse_whenRefreshFails() = runBlocking {
        val api = FakeAuthApi(refreshSuccess = false)
        val tokenStore = FakeTokenStore(access = "old-access", refresh = "old-refresh")
        val repo = AuthRepository(api, tokenStore)

        assertFalse(repo.validateSession())
    }

    // ─── hasValidSession ─────────────────────────────────────────────────────

    @Test
    fun hasValidSession_returnsTrueWhenTokenExists() {
        val repo = AuthRepository(FakeAuthApi(), FakeTokenStore(access = "token"))
        assertTrue(repo.hasValidSession())
    }

    @Test
    fun hasValidSession_returnsFalseWhenNoToken() {
        val repo = AuthRepository(FakeAuthApi(), FakeTokenStore())
        assertFalse(repo.hasValidSession())
    }

    // ─── logout ──────────────────────────────────────────────────────────────

    @Test
    fun logout_clearsTokens() {
        val tokenStore = FakeTokenStore(access = "a", refresh = "r")
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        repo.logout()

        assertNull(tokenStore.access)
        assertNull(tokenStore.refresh)
    }

    // ─── forgotPassword ──────────────────────────────────────────────────────

    @Test
    fun forgotPassword_returnsSuccess() = runBlocking {
        val repo = AuthRepository(FakeAuthApi(), FakeTokenStore())

        val result = repo.forgotPassword("9999999999")

        assertTrue(result is NetworkResult.Success)
    }

    // ─── resetPassword ───────────────────────────────────────────────────────

    @Test
    fun resetPassword_returnsSuccess() = runBlocking {
        val repo = AuthRepository(FakeAuthApi(), FakeTokenStore())

        val result = repo.resetPassword("reset-token", "newPass1")

        assertTrue(result is NetworkResult.Success)
    }

    // ─── refreshTokens ───────────────────────────────────────────────────────

    @Test
    fun refreshTokens_savesNewTokensAndReturnsTrue() = runBlocking {
        val tokenStore = FakeTokenStore(access = "old", refresh = "old-ref")
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        assertTrue(repo.refreshTokens())
        assertEquals("access-2", tokenStore.access)
        assertEquals("refresh-2", tokenStore.refresh)
    }

    @Test
    fun refreshTokens_returnsFalse_whenNoRefreshToken() = runBlocking {
        val tokenStore = FakeTokenStore(access = "old", refresh = null)
        val repo = AuthRepository(FakeAuthApi(), tokenStore)

        assertFalse(repo.refreshTokens())
    }

    // ─── Fakes ───────────────────────────────────────────────────────────────

    private class FakeAuthApi(
        private val loginSuccess: Boolean = true,
        private val registerSuccess: Boolean = true,
        private val verifyOtpSuccess: Boolean = true,
        private val refreshSuccess: Boolean = true
    ) : AuthApiService {
        override suspend fun register(request: RegisterRequest): ApiResponse<SimpleMessage> =
            if (registerSuccess) ApiResponse(true, SimpleMessage("OTP sent successfully."), null, null)
            else ApiResponse(false, null, ApiError("VALIDATION_ERROR", "Invalid"), null)

        override suspend fun verifyOtp(request: OtpVerifyRequest): ApiResponse<AuthTokens> =
            if (verifyOtpSuccess) ApiResponse(true, AuthTokens("access-otp", "refresh-otp", 1, "owner", 1), null, null)
            else ApiResponse(false, null, ApiError("INVALID_OTP", "Invalid or expired OTP."), null)

        override suspend fun login(request: LoginRequest): ApiResponse<AuthTokens> =
            if (loginSuccess) ApiResponse(true, AuthTokens("access-1", "refresh-1", 1, "owner", 1), null, null)
            else ApiResponse(false, null, ApiError("INVALID_CREDENTIALS", "Invalid credentials"), null)

        override suspend fun refresh(request: RefreshRequest): ApiResponse<AuthTokens> =
            if (refreshSuccess) ApiResponse(true, AuthTokens("access-2", "refresh-2", 1, "owner", 1), null, null)
            else ApiResponse(false, null, ApiError("INVALID_TOKEN", "Invalid refresh token"), null)

        override suspend fun logout(request: LogoutRequest?) = ApiResponse(true, SimpleMessage("ok"), null, null)
        override suspend fun forgotPassword(request: ForgotPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null)
        override suspend fun resetPassword(request: ResetPasswordRequest) = ApiResponse(true, SimpleMessage("ok"), null, null)
    }

    private data class FakeTokenStore(
        var access: String? = null,
        var refresh: String? = null,
        var _role: String = "staff",
        var setupComplete: Boolean = false
    ) : TokenStore {
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken; _role = "owner" }
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun getRole(): String = _role
        override fun isChainOwner(): Boolean = false
        override fun getChainGroupId(): String? = null
        override fun isSetupComplete(): Boolean = setupComplete
        override fun markSetupComplete() { setupComplete = true }
        override fun clearTokens() { access = null; refresh = null }
    }
}
