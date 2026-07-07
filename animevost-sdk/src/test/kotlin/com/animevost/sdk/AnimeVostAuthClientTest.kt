package com.animevost.sdk

import com.animevost.sdk.config.AnimeVostConfig
import com.animevost.sdk.error.AnimeVostAuthException
import com.animevost.sdk.http.AnimeVostHttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimeVostAuthClientTest {

    @Test
    fun `login posts DLE form and returns session from cookies`() = runBlocking {
        val httpClient = FakeAuthHttpClient(
            response = "<html>ok</html>",
            cookiesAfterPost = mapOf(
                "dle_user_id" to "42",
                "dle_password" to "password_hash",
                "dle_hash" to "session_hash",
            ),
        )
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val session = client.login(username = "test_user", password = "secret")

        assertEquals(listOf("https://example.test/animevost/index.php?do=login"), httpClient.postedUrls)
        assertEquals(
            mapOf(
                "login_name" to "test_user",
                "login_password" to "secret",
                "login" to "submit",
            ),
            httpClient.postedForms.single(),
        )
        assertEquals(42, session.userId)
        assertEquals("test_user", session.username)
        assertTrue(client.isLoggedIn())
        assertEquals(session, client.currentSession())
    }

    @Test
    fun `login rejects auth error response`() = runBlocking {
        val client = AnimeVostClient(
            httpClient = FakeAuthHttpClient(response = "<div class=\"berrors\">Ошибка авторизации</div>"),
        )

        assertFailsWith<AnimeVostAuthException> {
            client.login(username = "bad", password = "wrong")
        }
    }

    @Test
    fun `login rejects missing auth cookies`() = runBlocking {
        val client = AnimeVostClient(
            httpClient = FakeAuthHttpClient(response = "<html>ok</html>"),
        )

        assertFailsWith<AnimeVostAuthException> {
            client.login(username = "bad", password = "wrong")
        }
        assertFalse(client.isLoggedIn())
    }

    @Test
    fun `logout calls endpoint and clears local session`() = runBlocking {
        val httpClient = FakeAuthHttpClient(
            response = "<html>ok</html>",
            initialCookies = mapOf(
                "dle_user_id" to "42",
                "dle_password" to "password_hash",
                "dle_hash" to "session_hash",
            ),
        )
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        assertTrue(client.isLoggedIn())

        client.logout()

        assertEquals(listOf("https://example.test/animevost/index.php?action=logout"), httpClient.requestedUrls)
        assertTrue(httpClient.cookiesCleared)
        assertFalse(client.isLoggedIn())
    }

    @Test
    fun `getProfile fetches user page and parses profile`() = runBlocking {
        val httpClient = FakeAuthHttpClient(
            response = """
                <html>
                  <body>
                    <form id="userinfo">
                      <input name="id" value="42" />
                      <input name="fullname" value="Tester Name" />
                    </form>
                  </body>
                </html>
            """.trimIndent(),
        )
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val profile = client.getProfile("test_user")

        assertEquals(listOf("https://example.test/animevost/user/test_user/"), httpClient.requestedUrls)
        assertEquals(42, profile.userId)
        assertEquals("test_user", profile.username)
        assertEquals("Tester Name", profile.fullName)
    }

    private class FakeAuthHttpClient(
        private val response: String,
        initialCookies: Map<String, String> = emptyMap(),
        private val cookiesAfterPost: Map<String, String> = emptyMap(),
    ) : AnimeVostHttpClient {
        val requestedUrls = mutableListOf<String>()
        val postedUrls = mutableListOf<String>()
        val postedForms = mutableListOf<Map<String, String>>()
        var cookiesCleared = false
        private val cookies = initialCookies.toMutableMap()

        override suspend fun get(url: String, headers: Map<String, String>): String {
            requestedUrls += url
            return response
        }

        override suspend fun post(
            url: String,
            form: Map<String, String>,
            headers: Map<String, String>,
        ): String {
            postedUrls += url
            postedForms += form
            cookies += cookiesAfterPost
            return response
        }

        override fun getCookie(name: String): String? =
            cookies[name]

        override fun clearCookies() {
            cookies.clear()
            cookiesCleared = true
        }
    }
}
