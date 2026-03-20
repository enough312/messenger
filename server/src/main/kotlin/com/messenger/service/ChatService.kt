package com.messenger.service

import com.messenger.repository.ChatRepository
import com.messenger.repository.UserRepository
import com.messenger.shared.dto.AddChatMemberRequest
import com.messenger.shared.dto.CreateChannelRequest
import com.messenger.shared.dto.CreateGroupChatRequest
import com.messenger.shared.dto.CreatePrivateChatRequest
import com.messenger.shared.dto.UpdateChatMemberRoleRequest
import com.messenger.shared.dto.UpdateChatRequest
import com.messenger.shared.model.Chat
import com.messenger.shared.model.ChatMember

class ChatService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) {
    fun list(userId: String): List<Chat> = chatRepository.listChats(userId).map { enrich(userId, it) }

    fun createPrivate(userId: String, request: CreatePrivateChatRequest): Chat =
        enrich(userId, chatRepository.createPrivateChat(userId, request.peerUserId))

    fun createGroup(userId: String, request: CreateGroupChatRequest): Chat = chatRepository.createGroupChat(userId, request)

    fun createChannel(userId: String, request: CreateChannelRequest): Chat = chatRepository.createChannel(userId, request)

    fun get(chatId: String, userId: String): Chat =
        (chatRepository.getChat(chatId, userId)?.let { enrich(userId, it) }) ?: throw ServiceException("Chat not found", 404)

    fun update(chatId: String, request: UpdateChatRequest): Chat = chatRepository.updateChat(chatId, request) ?: throw ServiceException("Chat not found", 404)

    fun deleteOrLeave(chatId: String, userId: String) {
        if (!chatRepository.deleteOrLeave(chatId, userId)) throw ServiceException("Chat not found", 404)
    }

    fun members(chatId: String): List<ChatMember> = chatRepository.listMembers(chatId)

    fun addMember(chatId: String, request: AddChatMemberRequest) {
        if (!chatRepository.addMember(chatId, request.userId, request.role)) throw ServiceException("Member already exists", 409)
    }

    fun removeMember(chatId: String, userId: String) {
        if (!chatRepository.removeMember(chatId, userId)) throw ServiceException("Member not found", 404)
    }

    fun updateMemberRole(chatId: String, userId: String, request: UpdateChatMemberRoleRequest) {
        if (!chatRepository.updateMemberRole(chatId, userId, request)) throw ServiceException("Member not found", 404)
    }

    fun memberIds(chatId: String): List<String> = chatRepository.getChatMemberIds(chatId)

    private fun enrich(viewerId: String, chat: Chat): Chat {
        if (chat.type.name != "PRIVATE" || !chat.name.isNullOrBlank()) return chat
        val peer = chatRepository.getChatMemberIds(chat.id)
            .firstOrNull { it != viewerId }
            ?.let(userRepository::findById)
            ?: return chat.copy(name = "Private chat")
        return chat.copy(
            name = peer.displayName.ifBlank { "@${peer.username}" },
            avatarUrl = chat.avatarUrl ?: peer.avatarUrl,
        )
    }
}
