package com.animevost.sdk

import com.animevost.sdk.config.AnimeVostConfig
import com.animevost.sdk.http.AnimeVostHttpClient
import com.animevost.sdk.http.OkHttpAnimeVostHttpClient
import com.animevost.sdk.model.AnimeDetails
import com.animevost.sdk.model.AnimePage
import com.animevost.sdk.model.ScheduleDay
import com.animevost.sdk.model.VideoSource
import com.animevost.sdk.parser.AnimeDetailsParser
import com.animevost.sdk.parser.AnimeListParser
import com.animevost.sdk.parser.ScheduleParser
import com.animevost.sdk.parser.VideoSourceParser
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AnimeVostClient(
    private val config: AnimeVostConfig = AnimeVostConfig(),
    private val httpClient: AnimeVostHttpClient = OkHttpAnimeVostHttpClient(),
    private val scheduleParser: ScheduleParser = ScheduleParser(),
    private val animeListParser: AnimeListParser = AnimeListParser(),
    private val animeDetailsParser: AnimeDetailsParser = AnimeDetailsParser(),
    private val videoSourceParser: VideoSourceParser = VideoSourceParser(),
) {
    suspend fun getSchedule(): List<ScheduleDay> {
        val html = httpClient.get(
            url = normalizedBaseUrl(),
            headers = requestHeaders(),
        )
        return scheduleParser.parse(html, normalizedBaseUrl())
    }

    suspend fun getAnimeList(page: Int = 1): AnimePage {
        require(page >= 1) { "page must be greater than zero" }

        val baseUrl = normalizedBaseUrl()
        val html = httpClient.get(
            url = if (page == 1) baseUrl else "${baseUrl}page/$page/",
            headers = requestHeaders(),
        )
        return animeListParser.parse(html, baseUrl)
    }

    suspend fun searchAnime(query: String, page: Int = 1): AnimePage {
        require(query.isNotBlank()) { "query must not be blank" }
        require(page >= 1) { "page must be greater than zero" }

        val baseUrl = normalizedBaseUrl()
        val form = buildMap {
            put("subaction", "search")
            put("story", query.trim())
            if (page > 1) {
                put("result_from", ((page - 1) * SEARCH_PAGE_SIZE + 1).toString())
            }
        }
        val html = httpClient.post(
            url = URI(baseUrl).resolve("index.php?do=search").toString(),
            form = form,
            headers = requestHeaders(),
        )
        val parsed = animeListParser.parse(html, baseUrl)
        return parsed.copy(
            currentPage = page,
            totalPages = maxOf(parsed.totalPages, page),
        )
    }

    suspend fun getAnimeDetails(url: String): AnimeDetails {
        require(url.isNotBlank()) { "url must not be blank" }

        val baseUrl = normalizedBaseUrl()
        val requestUrl = URI(baseUrl).resolve(url.trim()).toString()
        val html = httpClient.get(
            url = requestUrl,
            headers = requestHeaders(),
        )
        return animeDetailsParser.parse(
            html = html,
            pageUrl = requestUrl,
            baseUrl = baseUrl,
        )
    }

    suspend fun getVideoSources(videoId: String): List<VideoSource> {
        require(videoId.isNotBlank()) { "videoId must not be blank" }

        val requestUrl = URI(normalizedBaseUrl())
            .resolve("getlink.php?id=${encode(videoId.trim())}")
            .toString()
        val response = httpClient.get(
            url = requestUrl,
            headers = requestHeaders(),
        )
        return videoSourceParser.parse(response)
    }

    private fun normalizedBaseUrl(): String =
        config.baseUrl.trim().trimEnd('/') + "/"

    private fun requestHeaders(): Map<String, String> =
        mapOf("User-Agent" to config.userAgent)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private companion object {
        const val SEARCH_PAGE_SIZE = 10
    }
}
