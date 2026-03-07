package com.retailiq.datasage.data.repository

import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.AuthApiService
import com.retailiq.datasage.data.api.ForgotPasswordRequest
import com.retailiq.datasage.data.api.LoginRequest
import com.retailiq.datasage.data.api.NetworkResult
import com.retailiq.datasage.data.api.OtpVerifyRequest
import com.retailiq.datasage.data.api.RefreshRequest
import com.retailiq.datasage.data.api.RegisterRequest
import com.retailiq.datasage.data.api.ResetPasswordRequest
import com.retailiq.datasage.data.api.toUserMessage
import java.net.SocketTimeoutException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore
) {
    suspend fun login(mobile: String, password: String): NetworkResult<String> = safeCall {
        val response = authApi.login(LoginRequest(mobile, password))
        val tokens = response.data ?: return@safeCall NetworkResult.Error(422, response.error.toUserMessage())
        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
        NetworkResult.Success(tokens.role)
    }

    suspend fun register(fullName: String, mobile: String, email: String, store: String, password: String): NetworkResult<Unit> = safeCall {
        val response = authApi.register(RegisterRequest(fullName = fullName, mobileNumber = mobile, email = email, password = password, storeName = store))
        if (!response.success) NetworkResult.Error(422, response.error.toUserMessage()) else NetworkResult.Success(Unit)
    }

    suspend fun verifyOtp(mobile: String, otp: String): NetworkResult<String> = safeCall {
        val response = authApi.verifyOtp(OtpVerifyRequest(mobile, otp))
        val tokens = response.data ?: return@safeCall NetworkResult.Error(422, response.error.toUserMessage())
        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
        NetworkResult.Success(tokens.role)
    }

    suspend fun forgotPassword(mobile: String): NetworkResult<Unit> = safeCall {
        val response = authApi.forgotPassword(ForgotPasswordRequest(mobile))
        if (!response.success) NetworkResult.Error(422, response.error.toUserMessage()) else NetworkResult.Success(Unit)
    }

    suspend fun resetPassword(token: String, newPassword: String): NetworkResult<Unit> = safeCall {
        val response = authApi.resetPassword(ResetPasswordRequest(token, newPassword))
        if (!response.success) NetworkResult.Error(422, response.error.toUserMessage()) else NetworkResult.Success(Unit)
    }

    suspend fun refreshTokens(): Boolean {
        val refresh = tokenStore.getRefreshToken() ?: return false
        return runCatching {
            val res = authApi.refresh(RefreshRequest(refresh)).data ?: return false
            tokenStore.saveTokens(res.accessToken, res.refreshToken)
            true
        }.getOrElse { false }
    }

    fun hasValidSession(): Boolean = tokenStore.getAccessToken() != null

    suspend fun validateSession(): Boolean {
        if (tokenStore.getAccessToken() == null) return false
        if (tokenStore.getRefreshToken() == null) return false
        return refreshTokens()
    }
    fun isSetupComplete(): Boolean = tokenStore.isSetupComplete()
    fun markSetupComplete() = tokenStore.markSetupComplete()
    fun getRole(): String = tokenStore.getRole()
    fun isChainOwner(): Boolean = tokenStore.isChainOwner()
    fun logout() = tokenStore.clearTokens()

    private suspend fun <T> safeCall(block: suspend () -> NetworkResult<T>): NetworkResult<T> = try {
        block()
    } catch (_: SocketTimeoutException) {
        NetworkResult.Error(408, "Request timed out. Please try again.")
    } catch (ex: Exception) {
        NetworkResult.Error(500, ex.message ?: "Unexpected error")
    }
}
