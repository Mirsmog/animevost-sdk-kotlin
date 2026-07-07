package com.animevost.sdk.parser

import com.animevost.sdk.model.AnimeCategory
import com.animevost.sdk.model.AnimePage
import com.animevost.sdk.model.AnimePreview
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class AnimeListParser {
    fun parse(html: String, baseUrl: String = "https://animevost.org/"): AnimePage {
        val doc = Jsoup.parse(html, normalizeBaseUrl(baseUrl))
        val currentPage = parseCurrentPage(doc)

        return AnimePage(
            items = doc.select("div.shortstory").mapNotNull(::parsePreview),
            currentPage = currentPage,
            totalPages = maxOf(currentPage, parseTotalPages(doc)),
        )
    }

    private fun parsePreview(story: Element): AnimePreview? {
        val titleLink = story.selectFirst(".shortstoryHead h2 a, .shortstoryHead h1 a")
            ?: return null
        val url = titleLink.absUrl("href")
        if (url.isBlank()) return null
        val id = extractId(url) ?: return null

        val titleParts = AnimeTitleParser.split(titleLink.text())
        val staticInfo = story.selectFirst(".staticInfo")

        return AnimePreview(
            id = id,
            title = titleParts.title,
            originalTitle = titleParts.originalTitle,
            episodeInfo = titleParts.episodeInfo,
            url = url,
            posterUrl = story.selectFirst(".shortstoryContent img")
                ?.absUrl("src")
                ?.takeIf { it.isNotBlank() },
            publishedDate = staticInfo
                ?.selectFirst(".staticInfoLeftData")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            viewCount = parseInt(
                staticInfo?.selectFirst(".staticInfoRightSmotr")?.text(),
            ),
            commentCount = parseInt(
                staticInfo?.selectFirst("""a[href$="#comment"]""")?.text(),
            ),
            rating = parseRating(story),
            voteCount = parseInt(story.selectFirst("""span[id^="vote-num-id-"]""")?.text()),
            categories = parseCategories(story),
        )
    }

    private fun parseCategories(story: Element): List<AnimeCategory> =
        story.select(".shortstoryFuter span a[href]").mapNotNull { link ->
            val title = link.text().trim()
            val url = link.absUrl("href")
            if (title.isBlank() || url.isBlank()) return@mapNotNull null
            AnimeCategory(title = title, url = url)
        }

    private fun parseRating(story: Element): Double? {
        val rating = story.selectFirst(".current-rating") ?: return null
        val percent = ratingWidthRegex.find(rating.attr("style"))
            ?.groupValues
            ?.get(1)
            ?.toDoubleOrNull()
            ?: rating.text().trim().toDoubleOrNull()
            ?: return null
        return percent / 20.0
    }

    private fun parseCurrentPage(doc: Document): Int =
        doc.select(
            ".pnext span.active, " +
                ".dle-pagination span.active, " +
                ".navigation span.dle_active, " +
                ".block_4 span:not(.nav_ext)",
        )
            .firstOrNull()
            ?.text()
            ?.trim()
            ?.toIntOrNull()
            ?: 1

    private fun parseTotalPages(doc: Document): Int =
        doc.select(".pnext a, .dle-pagination a, .navigation a, .block_4 a")
            .mapNotNull { it.text().trim().toIntOrNull() }
            .maxOrNull()
            ?: parseCurrentPage(doc)

    private fun extractId(url: String): Int? =
        idInUrlRegex.find(url)?.groupValues?.get(1)?.toIntOrNull()

    private fun parseInt(text: String?): Int? =
        text?.replace(nonDigitsRegex, "")?.takeIf { it.isNotBlank() }?.toIntOrNull()

    private fun normalizeBaseUrl(baseUrl: String): String =
        baseUrl.trim().trimEnd('/') + "/"

    private companion object {
        val idInUrlRegex = Regex("""/(\d+)-[^/]+\.html(?:[#?].*)?$""")
        val ratingWidthRegex = Regex("""width:\s*(\d+(?:\.\d+)?)%""")
        val nonDigitsRegex = Regex("""[^\d]""")
    }
}
