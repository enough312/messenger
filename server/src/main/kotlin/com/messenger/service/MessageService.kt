package com.messenger.service

import com.messenger.repository.ChatRepository
import com.messenger.repository.MessageRepository
import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.dto.UpdateMessageRequest
import com.messenger.shared.model.Message

class MessageService(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
) {
    fun list(chatId: String, userId: String, cursor: String?, limit: Int): List<Message> {
        ensureMember(chatId, userId)
        return messageRepository.listMessages(chatId, userId, cursor, limit)
    }

    fun send(chatId: String, senderId: String, request: SendMessageRequest): Message {
        ensureMember(chatId, senderId)
        return messageRepository.sendMessage(chatId, senderId, request)
    }

    fun update(messageId: String, request: UpdateMessageRequest): Message {
        return messageRepository.updateMessage(messageId, request) ?: throw ServiceException("Message not found", 404)
    }

    fun delete(messageId: String) {
        if (!messageRepository.deleteMessage(messageId)) throw ServiceException("Message not found", 404)
    }

    fun pin(messageId: String, pinned: Boolean): Message {
        return messageRepository.setPinned(messageId, pinned) ?: throw ServiceException("Message not found", 404)
    }

    fun react(messageId: String, userId: String, emoji: String): Message {
        return messageRepository.addReaction(messageId, userId, emoji) ?: throw ServiceException("Message not found", 404)
    }

    fun read(messageId: String, userId: String) {
        messageRepository.markRead(messageId, userId)
    }

    private fun ensureMember(chatId: String, userId: String) {
        if (!chatRepository.isMember(chatId, userId)) throw ServiceException("Access denied", 403)
    }
}
