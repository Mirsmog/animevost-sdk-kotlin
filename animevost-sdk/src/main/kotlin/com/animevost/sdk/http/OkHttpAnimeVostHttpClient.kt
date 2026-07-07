package com.animevost.sdk.http

import com.animevost.sdk.error.AnimeVostHttpException
import com.animevost.sdk.error.AnimeVostNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpAnimeVostHttpClient(
    private val cookieStore: AnimeVostCookieStore = InMemoryAnimeVostCookieStore(),
    private val client: OkHttpClient = defaultClient(cookieStore),
) : AnimeVostHttpClient {

    override suspend fun get(url: String, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .apply {
                    headers.forEach { (name, value) -> header(name, value) }
                }
                .build()

            execute(request)
        }

    override suspend fun post(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String>,
    ): String =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder()
                .apply { form.forEach { (name, value) -> add(name, value) } }
                .build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .apply {
                    headers.forEach { (name, value) -> header(name, value) }
                }
                .build()

            execute(request)
        }

    override suspend fun postMultipart(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String>,
    ): String =
        withContext(Dispatchers.IO) {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .apply { form.forEach { (name, value) -> addFormDataPart(name, value) } }
                .build()
            val request = Request.Builder()
                .url(url)
                .post(body)
                .apply {
                    headers.forEach { (name, value) -> header(name, value) }
                }
                .build()

            execute(request)
        }

    override fun getCookie(name: String): String? =
        cookieStore.get(name)

    override fun clearCookies() {
        cookieStore.clear()
    }

    private fun execute(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw AnimeVostHttpException(response.code, response.message)
                }
                return response.body?.string().orEmpty()
            }
        } catch (exception: IOException) {
            throw AnimeVostNetworkException(exception)
        }
    }

    private companion object {
        fun defaultClient(cookieStore: AnimeVostCookieStore): OkHttpClient =
            OkHttpClient.Builder()
                .cookieJar(AnimeVostCookieJar(cookieStore))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
    }
}
