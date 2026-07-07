package com.animevost.sdk.parser

import com.animevost.sdk.model.VideoSource
import java.net.URI

class VideoSourceParser {
    fun parse(response: String): List<VideoSource> =
        sourceUrlRegex.findAll(response)
            .map { it.value.trim() }
            .mapNotNull(::parseSource)
            .toList()

    private fun parseSource(rawUrl: String): VideoSource? {
        if (rawUrl.isBlank()) return null
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https")) return null
        return VideoSource(
            url = rawUrl,
            host = uri.host,
        )
    }

    private companion object {
        val sourceUrlRegex = Regex("""https?://\S+""")
    }
}
