package com.walkfree.data

import com.walkfree.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.encoding.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/// HTTP 客户端实现 — Ktor 单例 (HTTP/2, Gzip, 超时, 重试退避)
class HttpClientImpl private constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; coerceInputValues = true }

    val client = HttpClient {
        install(ContentNegotiation) { json(this@HttpClientImpl.json) }
        install(ContentEncoding) { gzip(); deflate() }
        defaultRequest {
            AppConfig.Network.defaultBaseUrl.let { if (it.isNotEmpty()) url(it) }
            contentType(ContentType.Application.Json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = AppConfig.Network.requestTimeoutMs
            connectTimeoutMillis = AppConfig.Network.connectTimeoutMs
            socketTimeoutMillis = AppConfig.Network.socketTimeoutMs
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = AppConfig.Network.maxRetries)
            retryOnException(maxRetries = AppConfig.Network.maxRetries)
            exponentialDelay()
        }
    }

    suspend inline fun <reified T> get(path: String, params: Map<String, String> = emptyMap()): T =
        client.get(path) { params.forEach { (k, v) -> parameter(k, v) } }.body()

    suspend fun warmup() { try { client.get(AppConfig.Network.healthEndpoint).body<Unit>() } catch (_: Exception) { } }

    companion object {
        val shared: HttpClientImpl by lazy { HttpClientImpl() }
    }
}
