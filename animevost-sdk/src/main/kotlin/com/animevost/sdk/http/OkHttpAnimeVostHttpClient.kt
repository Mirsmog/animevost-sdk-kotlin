package com.animevost.sdk.http

import com.animevost.sdk.error.AnimeVostHttpException
import com.animevost.sdk.error.AnimeVostNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpAnimeVostHttpClient(
    private val client: OkHttpClient = defaultClient(),
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
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
    }
}
