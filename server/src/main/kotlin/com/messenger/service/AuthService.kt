package com.messenger.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.messenger.config.AppConfig
import com.messenger.repository.UserRepository
import com.messenger.shared.dto.ForgotPasswordRequest
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.LogoutRequest
import com.messenger.shared.dto.RefreshRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.ResetPasswordRequest
import com.messenger.shared.dto.TokenResponse
import com.messenger.shared.dto.VerifyEmailRequest
import com.messenger.shared.dto.VerificationResponse
import com.messenger.shared.model.User
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.time.Instant
import java.util.Date
import java.util.UUID

class AuthService(
    private val config: AppConfig,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val twoFactorService: TwoFactorService,
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val random = SecureRandom()

    fun register(request: RegisterRequest): VerificationResponse {
        if (userRepository.findByEmail(request.email) != null) {
            throw ServiceException("User with this email already exists", 409)
        }
        val hash = BCrypt.hashpw(request.password, BCrypt.gensalt())
        userRepository.createUser(request, hash)
        userRepository.markVerified(request.email)
        return VerificationResponse(true, "Registration successful. You can log in now.")
    }

    fun login(request: LoginRequest, ipAddress: String?, userAgent: String?): TokenResponse {
        val (user, hash) = userRepository.findCredentialsByEmail(request.email)
            ?: throw ServiceException("Invalid credentials", 401)
        if (!BCrypt.checkpw(request.password, hash)) {
            throw ServiceException("Invalid credentials", 401)
        }

        val (twoFactorSecret, enabled) = userRepository.getTwoFactorSecret(user.id)
        if (enabled) {
            val code = request.twoFactorCode ?: throw ServiceException("Two-factor code is required", 401)
            if (twoFactorSecret == null || !twoFactorService.verify(twoFactorSecret, code)) {
                throw ServiceException("Invalid two-factor code", 401)
            }
        }

        val accessToken = createAccessToken(user)
        val refreshToken = UUID.randomUUID().toString()
        userRepository.createSession(user.id, request, refreshToken, ipAddress, userAgent, config.jwtRefreshTtlDays)
        return TokenResponse(accessToken, refreshToken, config.jwtAccessTtlMinutes * 60)
    }

    fun refresh(request: RefreshRequest): TokenResponse {
        val (session, user) = userRepository.findSessionByRefreshToken(request.refreshToken)
            ?: throw ServiceException("Refresh token is invalid", 401)
        if (session.expiresAt < Instant.now().toEpochMilli()) {
            userRepository.invalidateSession(request.refreshToken)
            throw ServiceException("Refresh token expired", 401)
        }
        val accessToken = createAccessToken(user)
        return TokenResponse(accessToken, request.refreshToken, config.jwtAccessTtlMinutes * 60)
    }

    fun logout(request: LogoutRequest): VerificationResponse {
        userRepository.invalidateSession(request.refreshToken)
        return VerificationResponse(true, "Logged out")
    }

    fun verifyEmail(request: VerifyEmailRequest): VerificationResponse {
        val consumed = userRepository.consumeEmailVerification(request.email, request.code)
        if (!consumed) throw ServiceException("Verification code is invalid or expired", 400)
        userRepository.markVerified(request.email)
        return VerificationResponse(true, "Email verified")
    }

    fun forgotPassword(request: ForgotPasswordRequest): VerificationResponse {
        val user = userRepository.findByEmail(request.email) ?: return VerificationResponse(true, "If the account exists, reset instructions were sent")
        val code = generateCode()
        userRepository.createPasswordReset(user.email, code, ttlMillis = 15 * 60 * 1000)
        emailService.sendPasswordResetEmail(user.email, code)
        return VerificationResponse(true, "If the account exists, reset instructions were sent")
    }

    fun resetPassword(request: ResetPasswordRequest): VerificationResponse {
        val consumed = userRepository.consumePasswordReset(request.email, request.code)
        if (!consumed) throw ServiceException("Reset code is invalid or expired", 400)
        userRepository.updatePassword(request.email, BCrypt.hashpw(request.newPassword, BCrypt.gensalt()))
        return VerificationResponse(true, "Password was updated")
    }

    fun setupTwoFactor(userId: String): Map<String, String> {
        val secret = twoFactorService.generateSecret()
        userRepository.saveTwoFactorSecret(userId, secret, enabled = false)
        return mapOf("secret" to secret, "issuer" to config.totpIssuer)
    }

    fun enableTwoFactor(userId: String, code: String): VerificationResponse {
        val (secret, _) = userRepository.getTwoFactorSecret(userId)
        if (secret == null || !twoFactorService.verify(secret, code)) {
            throw ServiceException("Invalid TOTP code", 400)
        }
        userRepository.saveTwoFactorSecret(userId, secret, enabled = true)
        return VerificationResponse(true, "Two-factor authentication enabled")
    }

    fun createAccessToken(user: User): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withSubject(user.id)
            .withClaim("email", user.email)
            .withClaim("username", user.username)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(config.jwtAccessTtlMinutes * 60)))
            .sign(algorithm)
    }

    fun verifier() = JWT.require(algorithm)
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()

    private fun generateCode(): String {
        val value = random.nextInt(900_000) + 100_000
        return value.toString()
    }
}
