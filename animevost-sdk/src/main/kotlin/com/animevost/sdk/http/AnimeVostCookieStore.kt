package com.animevost.sdk.http

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

interface AnimeVostCookieStore {
    fun save(url: HttpUrl, cookies: List<Cookie>)

    fun load(url: HttpUrl): List<Cookie>

    fun get(name: String): String?

    fun clear()
}

class InMemoryAnimeVostCookieStore : AnimeVostCookieStore {
    private val lock = Any()
    private val cookiesByHost = linkedMapOf<String, MutableList<Cookie>>()

    override fun save(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val hostCookies = cookiesByHost.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { cookie ->
                hostCookies.removeAll { it.name == cookie.name }
                if (cookie.expiresAt > now) {
                    hostCookies += cookie
                }
            }
            hostCookies.removeAll { it.expiresAt <= now }
        }
    }

    override fun load(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val hostCookies = cookiesByHost[url.host] ?: return emptyList()
            hostCookies.removeAll { it.expiresAt <= now }
            return hostCookies.toList()
        }
    }

    override fun get(name: String): String? {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            return cookiesByHost.values
                .asSequence()
                .flatMap { it.asSequence() }
                .firstOrNull { it.name == name && it.expiresAt > now }
                ?.value
                ?.takeIf { it != "deleted" }
        }
    }

    override fun clear() {
        synchronized(lock) {
            cookiesByHost.clear()
        }
    }
}

internal class AnimeVostCookieJar(
    private val store: AnimeVostCookieStore,
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.save(url, cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store.load(url)
}
