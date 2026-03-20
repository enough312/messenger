package com.messenger.android.presentation.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messenger.android.domain.usecase.GetMessagesUseCase
import com.messenger.android.domain.usecase.SendMessageUseCase
import com.messenger.shared.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {
    private val chatId: String = savedStateHandle["chatId"].orEmpty()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _messages.value = runCatching { getMessagesUseCase(chatId) }.getOrDefault(emptyList())
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            val created = runCatching { sendMessageUseCase(chatId, content) }.getOrNull() ?: return@launch
            _messages.value = _messages.value + created
        }
    }
}
