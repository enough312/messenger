package com.messenger.android.domain.usecase

import com.messenger.android.data.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(chatId: String, content: String) = chatRepository.send(chatId, content)
}
