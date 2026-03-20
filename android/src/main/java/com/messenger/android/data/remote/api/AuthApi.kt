package com.messenger.android.data.remote.api

import com.messenger.shared.dto.ForgotPasswordRequest
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.RefreshRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.ResetPasswordRequest
import com.messenger.shared.dto.TokenResponse
import com.messenger.shared.dto.VerificationResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): VerificationResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): VerificationResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): VerificationResponse
}
