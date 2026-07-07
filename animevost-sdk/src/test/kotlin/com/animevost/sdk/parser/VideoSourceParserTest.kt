package com.animevost.sdk.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoSourceParserTest {

    private val parser = VideoSourceParser()

    @Test
    fun `parses getlink response with primary and fallback urls`() {
        val sources = parser.parse(
            """
                https://std.roomfish.ru/100443228.mp4?md5=token&time=1783448470 or
                https://ram.roomfish.ru/100443228.mp4?md5=token&time=1783448470&ip=127.0.0.1
            """.trimIndent(),
        )

        assertEquals(2, sources.size)
        assertEquals("https://std.roomfish.ru/100443228.mp4?md5=token&time=1783448470", sources[0].url)
        assertEquals("std.roomfish.ru", sources[0].host)
        assertEquals("https://ram.roomfish.ru/100443228.mp4?md5=token&time=1783448470&ip=127.0.0.1", sources[1].url)
        assertEquals("ram.roomfish.ru", sources[1].host)
    }

    @Test
    fun `ignores malformed and empty entries`() {
        val sources = parser.parse("broken or  or https://std.roomfish.ru/1.mp4")

        assertEquals(listOf("https://std.roomfish.ru/1.mp4"), sources.map { it.url })
    }
}
