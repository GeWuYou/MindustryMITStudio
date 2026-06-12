package com.mindustry.ide.tool

import java.io.File

object MindustryMitConfig {
    val dataRoot: String
        get() = value("mindustrymit.dataRoot", "MINDUSTRYMIT_DATA_ROOT", ".mindustrymit-data")

    val logDir: String
        get() = optionalValue("mindustrymit.logDir", "MINDUSTRYMIT_LOG_DIR")
            ?: File(dataRoot, "logs").path

    val wsHost: String
        get() = value("mindustrymit.wsHost", "MINDUSTRYMIT_WS_HOST", "0.0.0.0")

    val wsPort: Int
        get() {
            val raw = value("mindustrymit.wsPort", "MINDUSTRYMIT_WS_PORT", "19190")
            return raw.toIntOrNull()?.takeIf { it in 1..65535 }
                ?: throw IllegalArgumentException("Invalid WebSocket port: $raw")
        }

    val wsToken: String?
        get() = optionalValue("mindustrymit.wsToken", "MINDUSTRYMIT_WS_TOKEN")

    fun applySystemPropertyDefaults() {
        setDefault("mindustrymit.dataRoot", dataRoot)
        setDefault("mindustrymit.logDir", logDir)
        setDefault("mindustrymit.wsHost", wsHost)
        setDefault("mindustrymit.wsPort", wsPort.toString())
        wsToken?.let { setDefault("mindustrymit.wsToken", it) }
    }

    private fun value(propertyName: String, envName: String, defaultValue: String): String {
        return optionalValue(propertyName, envName) ?: defaultValue
    }

    private fun optionalValue(propertyName: String, envName: String): String? {
        return System.getProperty(propertyName).takeIfNotBlank()
            ?: System.getenv(envName).takeIfNotBlank()
            ?: loadDotenv()[envName].takeIfNotBlank()
    }

    private fun setDefault(propertyName: String, value: String) {
        if (System.getProperty(propertyName).isNullOrBlank()) {
            System.setProperty(propertyName, value)
        }
    }

    private fun String?.takeIfNotBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun loadDotenv(): Map<String, String> {
        val envFile = dotenvCandidates().firstOrNull { it.isFile } ?: return emptyMap()
        return envFile.readLines(Charsets.UTF_8)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val index = line.indexOf('=')
                if (index <= 0) return@mapNotNull null

                val key = line.substring(0, index).trim()
                val value = line.substring(index + 1).trim().trimMatchingQuotes()
                key to value
            }
            .toMap()
    }

    private fun dotenvCandidates(): List<File> {
        val configured = System.getenv("MINDUSTRYMIT_ENV_FILE")
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
        val configuredProperty = System.getProperty("mindustrymit.envFile")
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
        val workingDir = File(System.getProperty("user.dir")).absoluteFile
        return listOfNotNull(
            configuredProperty,
            configured,
            File(workingDir, ".env"),
            File(workingDir, "server/.env"),
            workingDir.parentFile?.let { File(it, "server/.env") }
        ).distinctBy { it.absolutePath }
    }

    private fun String.trimMatchingQuotes(): String {
        if (length < 2) return this
        val first = first()
        val last = last()
        return if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            substring(1, length - 1)
        } else {
            this
        }
    }
}
