package com.messenger.android.data.repository

import com.messenger.android.data.remote.api.AuthApi
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.RegisterRequest
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
) {
    suspend fun login(email: String, password: String) = authApi.login(LoginRequest(email, password))

    suspend fun register(username: String, email: String, password: String, displayName: String) =
        authApi.register(RegisterRequest(username, email, password, displayName))
}
