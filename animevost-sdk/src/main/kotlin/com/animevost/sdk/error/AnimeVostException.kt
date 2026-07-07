package com.animevost.sdk.error

sealed class AnimeVostException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class AnimeVostNetworkException(
    cause: Throwable,
) : AnimeVostException("Network request failed", cause)

class AnimeVostHttpException(
    val statusCode: Int,
    message: String,
) : AnimeVostException("HTTP $statusCode: $message")
