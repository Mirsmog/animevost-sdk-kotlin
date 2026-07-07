package com.animevost.sdk

import com.animevost.sdk.config.AnimeVostConfig
import com.animevost.sdk.http.AnimeVostHttpClient
import com.animevost.sdk.model.Weekday
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnimeVostClientTest {

    @Test
    fun `getSchedule fetches base page and parses schedule`() = runBlocking {
        val httpClient = FakeHttpClient(
            response = """
                <html>
                  <body>
                    <div id="raspisMon" class="raspis">
                      <a href="/tip/tv/3966-saikyou-degarashi-ouji-no-anyaku-teii-arasoi.html">
                        Тайная битва за престол сильнейшего принца-дуралея ~ (17:30)
                      </a>
                    </div>
                  </body>
                </html>
            """.trimIndent(),
        )
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val schedule = client.getSchedule()

        assertEquals(listOf("https://example.test/animevost/"), httpClient.requestedUrls)
        assertEquals(1, httpClient.requestedHeaders.size)
        assertEquals(Weekday.MONDAY, schedule.single().weekday)
        assertEquals(
            "https://example.test/tip/tv/3966-saikyou-degarashi-ouji-no-anyaku-teii-arasoi.html",
            schedule.single().entries.single().url,
        )
    }

    @Test
    fun `getAnimeList fetches base page by default`() = runBlocking {
        val httpClient = FakeHttpClient(response = animeListHtml())
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val page = client.getAnimeList()

        assertEquals(listOf("https://example.test/animevost/"), httpClient.requestedUrls)
        assertEquals(3970, page.items.single().id)
        assertEquals("Забывчивая святая дева", page.items.single().title)
    }

    @Test
    fun `getAnimeList fetches requested page`() = runBlocking {
        val httpClient = FakeHttpClient(response = animeListHtml())
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        client.getAnimeList(page = 2)

        assertEquals(listOf("https://example.test/animevost/page/2/"), httpClient.requestedUrls)
    }

    @Test
    fun `getAnimeList rejects pages below one`() = runBlocking {
        val client = AnimeVostClient(httpClient = FakeHttpClient(response = animeListHtml()))

        assertFailsWith<IllegalArgumentException> {
            client.getAnimeList(page = 0)
        }
    }

    @Test
    fun `getAnimeDetails fetches detail page and parses response`() = runBlocking {
        val httpClient = FakeHttpClient(response = animeDetailsHtml())
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val details = client.getAnimeDetails("https://example.test/tip/tv/3970-test.html")

        assertEquals(listOf("https://example.test/tip/tv/3970-test.html"), httpClient.requestedUrls)
        assertEquals(3970, details.id)
        assertEquals("Забывчивая святая дева", details.title)
        assertEquals("1 серия", details.episodes.single().name)
    }

    @Test
    fun `getAnimeDetails rejects blank url`() = runBlocking {
        val client = AnimeVostClient(httpClient = FakeHttpClient(response = animeDetailsHtml()))

        assertFailsWith<IllegalArgumentException> {
            client.getAnimeDetails(" ")
        }
    }

    @Test
    fun `getVideoSources fetches getlink endpoint and parses response`() = runBlocking {
        val httpClient = FakeHttpClient(
            response = "https://std.roomfish.ru/100443228.mp4 or https://ram.roomfish.ru/100443228.mp4",
        )
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val sources = client.getVideoSources("100443228")

        assertEquals(listOf("https://example.test/animevost/getlink.php?id=100443228"), httpClient.requestedUrls)
        assertEquals(listOf("std.roomfish.ru", "ram.roomfish.ru"), sources.map { it.host })
    }

    @Test
    fun `getVideoSources rejects blank video id`() = runBlocking {
        val client = AnimeVostClient(httpClient = FakeHttpClient(response = ""))

        assertFailsWith<IllegalArgumentException> {
            client.getVideoSources(" ")
        }
    }

    @Test
    fun `searchAnime posts search form and parses results`() = runBlocking {
        val httpClient = FakeHttpClient(response = animeListHtml())
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val page = client.searchAnime("bleach")

        assertEquals(listOf("https://example.test/animevost/index.php?do=search"), httpClient.postedUrls)
        assertEquals(
            mapOf("subaction" to "search", "story" to "bleach"),
            httpClient.postedForms.single(),
        )
        assertEquals(3970, page.items.single().id)
        assertEquals(1, page.currentPage)
    }

    @Test
    fun `searchAnime sends result offset for requested page`() = runBlocking {
        val httpClient = FakeHttpClient(response = animeListHtml())
        val client = AnimeVostClient(
            config = AnimeVostConfig(baseUrl = "https://example.test/animevost/"),
            httpClient = httpClient,
        )

        val page = client.searchAnime(query = "bleach", page = 2)

        assertEquals("11", httpClient.postedForms.single()["result_from"])
        assertEquals(2, page.currentPage)
    }

    @Test
    fun `searchAnime rejects blank query and pages below one`() = runBlocking {
        val client = AnimeVostClient(httpClient = FakeHttpClient(response = animeListHtml()))

        assertFailsWith<IllegalArgumentException> {
            client.searchAnime(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            client.searchAnime("bleach", page = 0)
        }
    }

    private fun animeListHtml(): String =
        """
            <html>
              <body>
                <div class="shortstory">
                  <div class="shortstoryHead">
                    <h2>
                      <a href="/tip/tv/3970-test.html">
                        Забывчивая святая дева / Mujikaku Seijo [1-2 из 12+]
                      </a>
                    </h2>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

    private fun animeDetailsHtml(): String =
        """
            <html>
              <body>
                <div class="shortstory">
                  <div class="shortstoryHead">
                    <h1>Забывчивая святая дева / Mujikaku Seijo [1 серия]</h1>
                  </div>
                  <script>var data = {"1 серия":"100443228",};</script>
                </div>
              </body>
            </html>
        """.trimIndent()

    private class FakeHttpClient(
        private val response: String,
    ) : AnimeVostHttpClient {
        val requestedUrls = mutableListOf<String>()
        val requestedHeaders = mutableListOf<Map<String, String>>()
        val postedUrls = mutableListOf<String>()
        val postedForms = mutableListOf<Map<String, String>>()
        val postedHeaders = mutableListOf<Map<String, String>>()

        override suspend fun get(url: String, headers: Map<String, String>): String {
            requestedUrls += url
            requestedHeaders += headers
            return response
        }

        override suspend fun post(
            url: String,
            form: Map<String, String>,
            headers: Map<String, String>,
        ): String {
            postedUrls += url
            postedForms += form
            postedHeaders += headers
            return response
        }
    }
}
