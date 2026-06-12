plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "com.mindustry.ide"
    version = project.findProperty("version")?.toString() ?: "0.0.0-SNAPSHOT"
}

tasks.named("build") {
    dependsOn(":server:build", ":frontend:build")
}

tasks.named("check") {
    dependsOn(":server:check", ":frontend:check")
}
