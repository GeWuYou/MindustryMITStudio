package com.mindustry.ide.tool

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MindustryMitConfigTest {

    @Test
    fun readsDefaultsWhenNoOverridesExist() {
        withConfigEnvironment {
            assertEquals(".mindustrymit-data", MindustryMitConfig.dataRoot)
            assertEquals(".mindustrymit-data/logs", MindustryMitConfig.logDir)
            assertEquals("0.0.0.0", MindustryMitConfig.wsHost)
            assertEquals(19190, MindustryMitConfig.wsPort)
            assertNull(MindustryMitConfig.wsToken)
            assertEquals(4, MindustryMitConfig.docFetchAsyncLimit)
            assertEquals(5, MindustryMitConfig.docFetchMaxRetries)
            assertEquals(3000L, MindustryMitConfig.docFetchRetryDelayMs)
            assertEquals(60000, MindustryMitConfig.docFetchConnectTimeoutMs)
            assertEquals(60000, MindustryMitConfig.docFetchReadTimeoutMs)
        }
    }

    @Test
    fun readsDotenvValues() {
        withConfigEnvironment(
            dotenv = """
                MINDUSTRYMIT_WS_HOST=127.0.0.1
                MINDUSTRYMIT_WS_PORT=19191
                MINDUSTRYMIT_DATA_ROOT=custom-data
                MINDUSTRYMIT_LOG_DIR=custom-logs
                MINDUSTRYMIT_WS_TOKEN=secret
                MINDUSTRYMIT_DOC_FETCH_ASYNC_LIMIT=2
                MINDUSTRYMIT_DOC_FETCH_MAX_RETRIES=7
                MINDUSTRYMIT_DOC_FETCH_RETRY_DELAY_MS=1500
                MINDUSTRYMIT_DOC_FETCH_CONNECT_TIMEOUT_MS=5000
                MINDUSTRYMIT_DOC_FETCH_READ_TIMEOUT_MS=6000
            """.trimIndent()
        ) {
            assertEquals("127.0.0.1", MindustryMitConfig.wsHost)
            assertEquals(19191, MindustryMitConfig.wsPort)
            assertEquals("custom-data", MindustryMitConfig.dataRoot)
            assertEquals("custom-logs", MindustryMitConfig.logDir)
            assertEquals("secret", MindustryMitConfig.wsToken)
            assertEquals(2, MindustryMitConfig.docFetchAsyncLimit)
            assertEquals(7, MindustryMitConfig.docFetchMaxRetries)
            assertEquals(1500L, MindustryMitConfig.docFetchRetryDelayMs)
            assertEquals(5000, MindustryMitConfig.docFetchConnectTimeoutMs)
            assertEquals(6000, MindustryMitConfig.docFetchReadTimeoutMs)
        }
    }

    @Test
    fun systemPropertiesOverrideDotenvValues() {
        withConfigEnvironment(
            dotenv = """
                MINDUSTRYMIT_WS_HOST=0.0.0.0
                MINDUSTRYMIT_WS_PORT=19190
            """.trimIndent(),
            properties = mapOf(
                "mindustrymit.wsHost" to "127.0.0.1",
                "mindustrymit.wsPort" to "19192",
                "mindustrymit.docFetch.asyncLimit" to "3"
            )
        ) {
            assertEquals("127.0.0.1", MindustryMitConfig.wsHost)
            assertEquals(19192, MindustryMitConfig.wsPort)
            assertEquals(3, MindustryMitConfig.docFetchAsyncLimit)
        }
    }

    @Test
    fun rejectsInvalidDocFetchValues() {
        withConfigEnvironment(
            properties = mapOf("mindustrymit.docFetch.asyncLimit" to "0")
        ) {
            assertFailsWith<IllegalArgumentException> {
                MindustryMitConfig.docFetchAsyncLimit
            }
        }
    }

    private fun withConfigEnvironment(
        dotenv: String? = null,
        properties: Map<String, String> = emptyMap(),
        block: () -> Unit
    ) {
        val envFile = kotlin.io.path.createTempFile("mindustrymit", ".env").toFile()
        val configProperties = listOf(
            "mindustrymit.envFile",
            "mindustrymit.dataRoot",
            "mindustrymit.logDir",
            "mindustrymit.wsHost",
            "mindustrymit.wsPort",
            "mindustrymit.wsToken",
            "mindustrymit.docFetch.asyncLimit",
            "mindustrymit.docFetch.maxRetries",
            "mindustrymit.docFetch.retryDelayMs",
            "mindustrymit.docFetch.connectTimeoutMs",
            "mindustrymit.docFetch.readTimeoutMs"
        )
        val previousProperties = configProperties.associateWith { System.getProperty(it) }

        try {
            if (dotenv == null) {
                envFile.delete()
            } else {
                envFile.writeText(dotenv, Charsets.UTF_8)
            }

            System.setProperty("mindustrymit.envFile", envFile.absolutePath)
            configProperties.forEach(System::clearProperty)
            System.setProperty("mindustrymit.envFile", envFile.absolutePath)
            properties.forEach { (key, value) -> System.setProperty(key, value) }

            block()
        } finally {
            previousProperties.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
            envFile.delete()
        }
    }
}
