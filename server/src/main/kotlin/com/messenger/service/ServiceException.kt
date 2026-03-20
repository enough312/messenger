package com.messenger.service

class ServiceException(
    override val message: String,
    val statusCode: Int = 400,
) : RuntimeException(message)
