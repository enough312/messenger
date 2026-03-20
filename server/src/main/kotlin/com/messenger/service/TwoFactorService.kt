package com.messenger.service

import com.messenger.config.AppConfig
import com.warrenstrange.googleauth.GoogleAuthenticator
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig

class TwoFactorService(
    config: AppConfig,
) {
    private val authenticator = GoogleAuthenticator(
        GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .build(),
    )

    fun generateSecret(): String = authenticator.createCredentials().key

    fun verify(secret: String, code: String): Boolean {
        val parsed = code.toIntOrNull() ?: return false
        return authenticator.authorize(secret, parsed)
    }
}
