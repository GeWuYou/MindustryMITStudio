package com.mindustry.ide.tool

import java.io.File

object Logging {
    fun configureDefaults() {
        if (System.getProperty("mindustrymit.logDir").isNullOrBlank()) {
            val dataRoot = System.getProperty("mindustrymit.dataRoot", ".mindustrymit-data")
            System.setProperty("mindustrymit.logDir", File(dataRoot, "logs").path)
        }

        if (System.getProperty("logback.rootLevel").isNullOrBlank()) {
            System.setProperty("logback.rootLevel", "INFO")
        }
    }
}
