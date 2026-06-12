plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "com.mindustry.ide"
    version = project.findProperty("version")?.toString() ?: "0.0.0-SNAPSHOT"
}
