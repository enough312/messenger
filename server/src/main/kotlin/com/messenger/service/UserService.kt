package com.messenger.service

import com.messenger.repository.UserRepository
import com.messenger.shared.dto.UpdateProfileRequest
import com.messenger.shared.model.User

class UserService(
    private val userRepository: UserRepository,
) {
    fun me(userId: String): User = userRepository.findById(userId) ?: throw ServiceException("User not found", 404)

    fun updateProfile(userId: String, request: UpdateProfileRequest): User = userRepository.updateProfile(userId, request)

    fun deleteAccount(userId: String) {
        val deleted = userRepository.deleteAccount(userId)
        if (deleted == 0) throw ServiceException("User not found", 404)
    }

    fun search(query: String): List<User> = userRepository.search(query)

    fun getById(id: String): User = userRepository.findById(id) ?: throw ServiceException("User not found", 404)

    fun contacts(userId: String): List<User> = userRepository.listContacts(userId)

    fun addContact(userId: String, contactId: String) {
        userRepository.addContact(userId, contactId)
    }

    fun removeContact(userId: String, contactId: String) {
        userRepository.deleteContact(userId, contactId)
    }

    fun block(userId: String, blockedUserId: String) {
        userRepository.blockUser(userId, blockedUserId)
    }

    fun unblock(userId: String, blockedUserId: String) {
        userRepository.unblockUser(userId, blockedUserId)
    }

    fun setOnline(userId: String) {
        userRepository.updatePresence(userId, "online")
    }

    fun setOffline(userId: String) {
        userRepository.updatePresence(userId, "offline")
    }
}
