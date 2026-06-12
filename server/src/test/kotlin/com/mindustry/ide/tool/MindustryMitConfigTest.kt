package com.mindustry.ide.tool

import kotlin.test.Test
import kotlin.test.assertEquals
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
            """.trimIndent()
        ) {
            assertEquals("127.0.0.1", MindustryMitConfig.wsHost)
            assertEquals(19191, MindustryMitConfig.wsPort)
            assertEquals("custom-data", MindustryMitConfig.dataRoot)
            assertEquals("custom-logs", MindustryMitConfig.logDir)
            assertEquals("secret", MindustryMitConfig.wsToken)
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
                "mindustrymit.wsPort" to "19192"
            )
        ) {
            assertEquals("127.0.0.1", MindustryMitConfig.wsHost)
            assertEquals(19192, MindustryMitConfig.wsPort)
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
            "mindustrymit.wsToken"
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
