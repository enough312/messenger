package com.messenger.android.data.remote.api

import com.messenger.shared.dto.ChatListResponse
import com.messenger.shared.dto.CreatePrivateChatRequest
import com.messenger.shared.dto.MessageListResponse
import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.model.Chat
import com.messenger.shared.model.Message
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("chats")
    suspend fun chats(): ChatListResponse

    @POST("chats/private")
    suspend fun createPrivate(@Body request: CreatePrivateChatRequest): Chat

    @GET("chats/{id}/messages")
    suspend fun messages(@Path("id") chatId: String, @Query("cursor") cursor: String? = null): MessageListResponse

    @POST("chats/{id}/messages")
    suspend fun send(@Path("id") chatId: String, @Body request: SendMessageRequest): Message
}
