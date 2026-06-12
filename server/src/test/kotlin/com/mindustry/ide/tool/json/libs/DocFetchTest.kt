package com.mindustry.ide.tool.json.libs

import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocFetchTest {

    @Test
    fun retriesTransientSocketFailure() = runBlocking {
        val fetcher = TestDocFetch(
            responses = mutableListOf(
                { throw SocketException("Connection reset") },
                { FetchResponse(HttpURLConnection.HTTP_OK, "ok") }
            )
        )

        assertEquals("ok", fetcher.fetch(URL("https://example.test/wiki/doc")))
        assertEquals(2, fetcher.fetchCount)
        assertEquals(listOf(0), fetcher.delayAttempts)
    }

    @Test
    fun retriesTransientHttpStatus() = runBlocking {
        val fetcher = TestDocFetch(
            responses = mutableListOf(
                { FetchResponse(HttpURLConnection.HTTP_UNAVAILABLE, null) },
                { FetchResponse(HttpURLConnection.HTTP_OK, "ok") }
            )
        )

        assertEquals("ok", fetcher.fetch(URL("https://example.test/wiki/doc")))
        assertEquals(2, fetcher.fetchCount)
        assertEquals(listOf(0), fetcher.delayAttempts)
    }

    @Test
    fun doesNotRetryNotFound() = runBlocking {
        val fetcher = TestDocFetch(
            responses = mutableListOf(
                { FetchResponse(HttpURLConnection.HTTP_NOT_FOUND, null) },
                { FetchResponse(HttpURLConnection.HTTP_OK, "should-not-run") }
            )
        )

        assertNull(fetcher.fetch(URL("https://example.test/wiki/missing")))
        assertEquals(1, fetcher.fetchCount)
        assertEquals(emptyList(), fetcher.delayAttempts)
    }

    @Test
    fun retryDelayUsesExponentialBackoffWithCap() {
        val fetcher = TestDocFetch(
            config = DocFetchConfig(
                asyncLimit = 1,
                connectTimeoutMs = 1000,
                readTimeoutMs = 1000,
                maxRetries = 5,
                retryDelayMs = 1000L
            )
        )

        assertEquals(1000L, fetcher.delayFor(0))
        assertEquals(2000L, fetcher.delayFor(1))
        assertEquals(30000L, fetcher.delayFor(10))
    }

    private class TestDocFetch(
        private val responses: MutableList<() -> FetchResponse> = mutableListOf(),
        config: DocFetchConfig = DocFetchConfig(
            asyncLimit = 1,
            connectTimeoutMs = 1000,
            readTimeoutMs = 1000,
            maxRetries = 3,
            retryDelayMs = 1L
        )
    ) : DocFetch(config) {
        var fetchCount = 0
            private set
        val delayAttempts = mutableListOf<Int>()
        var recordDelayAttempts = true

        suspend fun fetch(url: URL): String? {
            recordDelayAttempts = true
            return fetchWithRetry(url)
        }

        fun delayFor(attempt: Int): Long {
            recordDelayAttempts = false
            return retryDelayMs(attempt)
        }

        override suspend fun fetchOnce(url: URL): FetchResponse {
            fetchCount += 1
            return responses.removeFirst().invoke()
        }

        override fun retryDelayMs(attempt: Int): Long {
            if (recordDelayAttempts) {
                delayAttempts += attempt
            }
            return super.retryDelayMs(attempt)
        }

        override fun retryJitterMs(): Long {
            return 0L
        }
    }
}
