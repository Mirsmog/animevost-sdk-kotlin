package com.animevost.sdk

import com.animevost.sdk.config.AnimeVostConfig
import com.animevost.sdk.error.AnimeVostAuthException
import com.animevost.sdk.error.AnimeVostRegistrationException
import com.animevost.sdk.http.AnimeVostHttpClient
import com.animevost.sdk.http.OkHttpAnimeVostHttpClient
import com.animevost.sdk.model.AnimeDetails
import com.animevost.sdk.model.AnimePage
import com.animevost.sdk.model.AnimePreview
import com.animevost.sdk.model.AuthSession
import com.animevost.sdk.model.CommentPage
import com.animevost.sdk.model.CommentSubmissionResult
import com.animevost.sdk.model.CatalogFilter
import com.animevost.sdk.model.FavoriteActionResult
import com.animevost.sdk.model.NavigationData
import com.animevost.sdk.model.RegistrationActivationResult
import com.animevost.sdk.model.RegistrationRequest
import com.animevost.sdk.model.RegistrationResult
import com.animevost.sdk.model.RegistrationStatus
import com.animevost.sdk.model.ScheduleDay
import com.animevost.sdk.model.UserProfile
import com.animevost.sdk.model.UserProfileUpdate
import com.animevost.sdk.model.VideoSource
import com.animevost.sdk.parser.AnimeDetailsParser
import com.animevost.sdk.parser.AnimeListParser
import com.animevost.sdk.parser.CommentsParser
import com.animevost.sdk.parser.FavoritesParser
import com.animevost.sdk.parser.NavigationParser
import com.animevost.sdk.parser.RandomAnimeParser
import com.animevost.sdk.parser.ScheduleParser
import com.animevost.sdk.parser.UserProfileParser
import com.animevost.sdk.parser.VideoSourceParser
import com.google.gson.JsonParser
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
    private val navigationParser: NavigationParser = NavigationParser(),
    private val randomAnimeParser: RandomAnimeParser = RandomAnimeParser(),
    private val userProfileParser: UserProfileParser = UserProfileParser(),
    private val favoritesParser: FavoritesParser = FavoritesParser(),
    private val commentsParser: CommentsParser = CommentsParser(),
) {
    private var currentUsername: String? = null

    suspend fun login(username: String, password: String): AuthSession {
        require(username.isNotBlank()) { "username must not be blank" }
        require(password.isNotBlank()) { "password must not be blank" }

        val response = httpClient.post(
            url = URI(normalizedBaseUrl()).resolve("index.php?do=login").toString(),
            form = mapOf(
                "login_name" to username.trim(),
                "login_password" to password,
                "login" to "submit",
            ),
            headers = requestHeaders(),
        )

        if (response.hasAuthError()) {
            httpClient.clearCookies()
            throw AnimeVostAuthException("Invalid username or password")
        }

        val session = currentSession(username = username.trim())
            ?: throw AnimeVostAuthException("Login did not return auth cookies")
        currentUsername = session.username
        return session
    }

    suspend fun logout() {
        runCatching {
            httpClient.get(
                url = URI(normalizedBaseUrl()).resolve("index.php?action=logout").toString(),
                headers = requestHeaders(),
            )
        }
        httpClient.clearCookies()
        currentUsername = null
    }

    fun isLoggedIn(): Boolean =
        authUserId() != null

    fun currentSession(): AuthSession? =
        currentSession(username = currentUsername)

    suspend fun register(request: RegistrationRequest): RegistrationResult {
        val username = request.username.trim()
        val email = request.email.trim()
        require(username.isNotBlank()) { "username must not be blank" }
        require(request.password.isNotBlank()) { "password must not be blank" }
        require(email.isNotBlank()) { "email must not be blank" }

        val registerUrl = URI(normalizedBaseUrl()).resolve("index.php?do=register").toString()
        httpClient.post(
            url = registerUrl,
            form = mapOf(
                "do" to "register",
                "dle_rules_accept" to "yes",
            ),
            headers = requestHeaders(),
        )
        val response = httpClient.post(
            url = registerUrl,
            form = mapOf(
                "submit_reg" to "submit_reg",
                "do" to "register",
                "name" to username,
                "password1" to request.password,
                "password2" to request.password,
                "email" to email,
            ),
            headers = requestHeaders(),
        )

        if (response.hasRegistrationError()) {
            throw AnimeVostRegistrationException("Registration failed")
        }

        val session = currentSession(username = username)
        if (session != null) {
            currentUsername = username
        }
        return RegistrationResult(
            username = username,
            status = if (session != null) {
                RegistrationStatus.ACTIVE
            } else {
                RegistrationStatus.PENDING_EMAIL_ACTIVATION
            },
            session = session,
        )
    }

    suspend fun activateRegistration(activationUrl: String): RegistrationActivationResult {
        require(activationUrl.isNotBlank()) { "activationUrl must not be blank" }

        val response = httpClient.get(
            url = URI(normalizedBaseUrl()).resolve(activationUrl.trim()).toString(),
            headers = requestHeaders(),
        )
        if (response.hasRegistrationError()) {
            throw AnimeVostRegistrationException("Registration activation failed")
        }
        return RegistrationActivationResult(
            activated = response.hasActivationSuccess(),
        )
    }

    suspend fun getProfile(username: String? = currentUsername): UserProfile {
        val profileUsername = username
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("username must not be blank")

        val profileUrl = profileUrl(profileUsername)
        val html = httpClient.get(
            url = profileUrl,
            headers = requestHeaders(),
        )
        return userProfileParser.parse(html, profileUrl)
    }

    suspend fun updateProfile(
        username: String? = currentUsername,
        update: UserProfileUpdate,
    ): UserProfile {
        val current = getProfile(username)
        if (!current.canEdit) {
            throw AnimeVostAuthException("Profile is not editable")
        }

        val profileUrl = profileUrl(current.username)
        val response = httpClient.postMultipart(
            url = profileUrl,
            form = mapOf(
                "doaction" to "adduserinfo",
                "id" to current.userId.toString(),
                "dle_allow_hash" to current.allowHash.orEmpty(),
                "fullname" to (update.fullName ?: current.fullName).orEmpty(),
                "land" to (update.location ?: current.location).orEmpty(),
                "email" to (update.email ?: current.email).orEmpty(),
                "info" to (update.info ?: current.info).orEmpty(),
            ),
            headers = requestHeaders(),
        )
        return userProfileParser.parse(response, profileUrl)
    }

    suspend fun getFavorites(page: Int = 1): AnimePage {
        requireAuthenticated()
        require(page >= 1) { "page must be greater than zero" }

        val baseUrl = normalizedBaseUrl()
        val favoritesUrl = URI(baseUrl).resolve("favorites/").toString()
        val html = httpClient.get(
            url = if (page == 1) favoritesUrl else "${favoritesUrl}page/$page/",
            headers = requestHeaders(),
        )
        return favoritesParser.parse(html, baseUrl)
    }

    suspend fun addFavorite(newsId: Int): FavoriteActionResult =
        updateFavorite(newsId = newsId, action = "add", isFavorite = true)

    suspend fun removeFavorite(newsId: Int): FavoriteActionResult =
        updateFavorite(newsId = newsId, action = "del", isFavorite = false)

    suspend fun getComments(animeUrl: String, page: Int = 1): CommentPage {
        require(animeUrl.isNotBlank()) { "animeUrl must not be blank" }
        require(page >= 1) { "page must be greater than zero" }

        val baseUrl = normalizedBaseUrl()
        val requestUrl = URI(baseUrl).resolve(animeUrl.trim()).toString()
        if (page > 1) {
            val newsId = extractNewsId(requestUrl)
                ?: throw IllegalArgumentException("animeUrl must contain news id")
            return getComments(newsId = newsId, page = page)
        }

        val html = httpClient.get(
            url = requestUrl,
            headers = requestHeaders(),
        )
        return commentsParser.parsePage(
            html = html,
            pageUrl = requestUrl,
            baseUrl = baseUrl,
        )
    }

    suspend fun getComments(newsId: Int, page: Int = 1): CommentPage {
        require(newsId > 0) { "newsId must be greater than zero" }
        require(page >= 1) { "page must be greater than zero" }

        val baseUrl = normalizedBaseUrl()
        val response = httpClient.get(
            url = commentsAjaxUrl(baseUrl, newsId, page),
            headers = ajaxHeaders(),
        )
        return commentsParser.parseComments(
            html = extractAjaxCommentsHtml(response),
            newsId = newsId,
            currentPage = page,
            baseUrl = baseUrl,
        )
    }

    suspend fun addComment(
        newsId: Int,
        text: String,
        authorName: String? = currentUsername,
    ): CommentSubmissionResult {
        requireAuthenticated()
        require(newsId > 0) { "newsId must be greater than zero" }
        val commentText = text.trim()
        require(commentText.isNotBlank()) { "text must not be blank" }
        val name = authorName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("authorName must not be blank")

        val baseUrl = normalizedBaseUrl()
        val response = httpClient.post(
            url = URI(baseUrl).resolve("engine/ajax/addcomments.php").toString(),
            form = mapOf(
                "post_id" to newsId.toString(),
                "comments" to commentText,
                "name" to name,
                "mail" to "",
                "editor_mode" to "",
                "skin" to DLE_SKIN,
                "sec_code" to "",
                "question_answer" to "",
                "recaptcha_response_field" to "",
                "recaptcha_challenge_field" to "",
                "allow_subscribe" to "0",
            ),
            headers = ajaxHeaders(),
        )
        val comments = commentsParser.parseComments(
            html = extractAjaxCommentsHtml(response),
            newsId = newsId,
            baseUrl = baseUrl,
        ).comments

        return CommentSubmissionResult(
            newsId = newsId,
            comments = comments,
            rawMessage = response.trim().takeIf { comments.isEmpty() && it.isNotBlank() },
        )
    }

    suspend fun getSchedule(): List<ScheduleDay> {
        val html = httpClient.get(
            url = normalizedBaseUrl(),
            headers = requestHeaders(),
        )
        return scheduleParser.parse(html, normalizedBaseUrl())
    }

    suspend fun getAnimeList(
        page: Int = 1,
        filter: CatalogFilter = CatalogFilter(),
    ): AnimePage {
        require(page >= 1) { "page must be greater than zero" }

        val baseUrl = normalizedBaseUrl()
        val catalogUrl = catalogUrl(baseUrl, filter)
        val html = httpClient.get(
            url = if (page == 1) catalogUrl else "${catalogUrl}page/$page/",
            headers = requestHeaders(),
        )
        return animeListParser.parse(html, baseUrl)
    }

    suspend fun getNavigation(): NavigationData {
        val baseUrl = normalizedBaseUrl()
        val html = httpClient.get(
            url = baseUrl,
            headers = requestHeaders(),
        )
        return navigationParser.parse(html, baseUrl)
    }

    suspend fun getRandomAnime(): AnimePreview? {
        val baseUrl = normalizedBaseUrl()
        val html = httpClient.get(
            url = URI(baseUrl).resolve("get_random_post.php").toString(),
            headers = requestHeaders(),
        )
        return randomAnimeParser.parse(html, baseUrl)
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

    private fun catalogUrl(baseUrl: String, filter: CatalogFilter): String {
        val path = filter.path
            ?.trim()
            ?.trimStart('/')
            ?.takeIf { it.isNotBlank() }
            ?: return baseUrl
        return URI(baseUrl).resolve(path).toString().trimEnd('/') + "/"
    }

    private fun profileUrl(username: String): String =
        URI(normalizedBaseUrl())
            .resolve("user/${encodePathSegment(username)}/")
            .toString()

    private fun requestHeaders(): Map<String, String> =
        mapOf("User-Agent" to config.userAgent)

    private fun ajaxHeaders(): Map<String, String> =
        requestHeaders() + mapOf("X-Requested-With" to "XMLHttpRequest")

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun currentSession(username: String?): AuthSession? {
        val userId = authUserId() ?: return null
        return AuthSession(
            userId = userId,
            username = username,
        )
    }

    private fun authUserId(): Int? =
        httpClient.getCookie("dle_user_id")
            ?.takeIf { it != "deleted" }
            ?.toIntOrNull()

    private fun requireAuthenticated() {
        if (!isLoggedIn()) {
            throw AnimeVostAuthException("Authentication required")
        }
    }

    private fun commentsAjaxUrl(baseUrl: String, newsId: Int, page: Int): String =
        URI(baseUrl)
            .resolve("engine/ajax/comments.php?cstart=$page&news_id=$newsId&skin=$DLE_SKIN&massact=disable")
            .toString()

    private fun extractAjaxCommentsHtml(response: String): String {
        val trimmed = response.trim()
        if (!trimmed.startsWith("{")) return response

        return runCatching {
            JsonParser.parseString(trimmed)
                .asJsonObject
                .get("comments")
                ?.asString
                .orEmpty()
        }.getOrDefault(response)
    }

    private fun extractNewsId(value: String): Int? =
        newsIdRegex.find(value)?.groupValues?.get(1)?.toIntOrNull()

    private suspend fun updateFavorite(
        newsId: Int,
        action: String,
        isFavorite: Boolean,
    ): FavoriteActionResult {
        requireAuthenticated()
        require(newsId > 0) { "newsId must be greater than zero" }

        httpClient.get(
            url = URI(normalizedBaseUrl())
                .resolve("index.php?do=favorites&doaction=$action&id=$newsId")
                .toString(),
            headers = requestHeaders(),
        )
        return FavoriteActionResult(
            newsId = newsId,
            isFavorite = isFavorite,
        )
    }

    private fun String.hasAuthError(): Boolean =
        contains("Ошибка авторизации") || contains("berrors")

    private fun String.hasRegistrationError(): Boolean =
        contains("Ошибка") ||
            contains("berrors") ||
            contains("уже используется", ignoreCase = true)

    private fun String.hasActivationSuccess(): Boolean =
        contains("активирован", ignoreCase = true) ||
            contains("активация", ignoreCase = true)

    private companion object {
        const val SEARCH_PAGE_SIZE = 10
        const val DLE_SKIN = "AnimeVostNext5"
        val newsIdRegex = Regex("""/(\d+)-[^/]+\.html(?:[#?].*)?$""")
    }
}
