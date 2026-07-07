package com.animevost.sdk.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnimeDetailsParserTest {

    private val parser = AnimeDetailsParser()

    @Test
    fun `parses anime details page`() {
        val details = parser.parse(
            html = """
                <html>
                  <head>
                    <meta property="og:url" content="https://animevost.org/tip/tv/3970-test.html" />
                  </head>
                  <body>
                    <div class="shortstory">
                      <div class="shortstoryHead">
                        <h1>
                          Забывчивая святая дева неосознанно изливает свою силу / Mujikaku Seijo wa Kyou mo Muishiki ni Chikara wo Tare Nagasu [1-2 из 12+]
                        </h1>
                      </div>
                      <div class="staticInfo">
                        <div class="staticInfoLeft">
                          <span class="staticInfoLeftData">7 июль 2026</span>
                        </div>
                        <div class="staticInfoRight">
                          <span class="staticInfoRightSmotr">88220</span>
                          <span><a id="dle-comm-link" href="/tip/tv/3970-test.html#comment">43</a></span>
                        </div>
                      </div>
                      <div class="shortstoryContent">
                        <img class="imgRadius" src="/uploads/posts/2026-06/1782839996_1.jpg?v2" />
                        <h4 itemprop="name">The Oblivious Saint Can't Contain Her Power</h4>
                        <p><strong>Год выхода: </strong>2026</p>
                        <p><strong>Жанр: </strong>фэнтези, романтика</p>
                        <p><strong>Тип: </strong>ТВ</p>
                        <p><strong>Количество серий: </strong>12+ (25 мин.)</p>
                        <p><strong>Режиссёр: </strong><span itemprop="director"><a href="/xfsearch/director/">Носитани Мицутака</a></span></p>
                        <div>
                          <strong>Рейтинг: </strong>
                          <li class="current-rating" style="width:60%;">60</li>
                          <span>(Голосов: <span id="vote-num-id-3970">134</span>)</span>
                        </div>
                        <p>
                          <strong>Описание: </strong>
                          <span itemprop="description">Первый абзац.<br><br>Второй абзац.</span>
                        </p>
                      </div>
                      <script>
                        var data = {"1 серия":"100443228","2 серия":"703736351",};
                      </script>
                      <div class="shortstoryFuter">
                        <span><strong>Категории:</strong>
                          <i>
                            <a href="https://animevost.org/tip/tv/">ТВ</a>,
                            <a href="https://animevost.org/ongoing/">Онгоинги</a>,
                            <a href="https://animevost.org/god/2026/">2026</a>
                          </i>
                        </span>
                      </div>
                    </div>
                  </body>
                </html>
            """.trimIndent(),
        )

        assertEquals(3970, details.id)
        assertEquals("https://animevost.org/tip/tv/3970-test.html", details.url)
        assertEquals("Забывчивая святая дева неосознанно изливает свою силу", details.title)
        assertEquals("Mujikaku Seijo wa Kyou mo Muishiki ni Chikara wo Tare Nagasu", details.originalTitle)
        assertEquals("1-2 из 12+", details.episodeInfo)
        assertEquals("The Oblivious Saint Can't Contain Her Power", details.alternativeTitle)
        assertEquals("https://animevost.org/uploads/posts/2026-06/1782839996_1.jpg?v2", details.posterUrl)
        assertEquals("7 июль 2026", details.publishedDate)
        assertEquals(88220, details.viewCount)
        assertEquals(43, details.commentCount)
        assertEquals("2026", details.year)
        assertEquals(listOf("фэнтези", "романтика"), details.genres)
        assertEquals("ТВ", details.type)
        assertEquals("12+ (25 мин.)", details.episodeCount)
        assertEquals("Носитани Мицутака", details.director)
        assertEquals(3.0, details.rating)
        assertEquals(134, details.voteCount)
        assertEquals("Первый абзац. Второй абзац.", details.description)
        assertEquals(listOf("ТВ", "Онгоинги", "2026"), details.categories.map { it.title })
        assertEquals(listOf("1 серия", "2 серия"), details.episodes.map { it.name })
        assertEquals(listOf("100443228", "703736351"), details.episodes.map { it.videoId })
        assertEquals(listOf(1, 2), details.episodes.map { it.number })
        assertEquals("https://media.aniland.org/img/100443228.jpg", details.episodes.first().thumbnailUrl)
    }

    @Test
    fun `handles page without optional metadata`() {
        val details = parser.parse(
            html = """
                <html>
                  <body>
                    <div class="shortstory">
                      <div class="shortstoryHead"><h1>Только название</h1></div>
                    </div>
                  </body>
                </html>
            """.trimIndent(),
            pageUrl = "https://animevost.org/tip/ova/12-test.html",
        )

        assertEquals(12, details.id)
        assertEquals("Только название", details.title)
        assertNull(details.originalTitle)
        assertNull(details.episodeInfo)
        assertNull(details.posterUrl)
        assertEquals(emptyList(), details.episodes)
    }
}
