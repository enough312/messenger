package com.messenger.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String,
    val phone: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val twoFactorCode: String? = null,
    val deviceName: String? = null,
    val deviceType: String? = null,
    val pushToken: String? = null,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String,
)

@Serializable
data class EnableTwoFactorRequest(
    val code: String,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class VerificationResponse(
    val success: Boolean,
    val message: String,
)
