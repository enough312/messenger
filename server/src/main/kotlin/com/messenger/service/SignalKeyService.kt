package com.messenger.service

import com.messenger.repository.UserRepository

class SignalKeyService(
    private val userRepository: UserRepository,
) {
    fun upload(userId: String, identityKey: String, signedPreKey: String, oneTimePreKeys: String) {
        userRepository.saveSignalBundle(userId, identityKey, signedPreKey, oneTimePreKeys)
    }

    fun getBundle(userId: String): Map<String, String> {
        return userRepository.getSignalBundle(userId) ?: throw ServiceException("Signal bundle not found", 404)
    }
}
