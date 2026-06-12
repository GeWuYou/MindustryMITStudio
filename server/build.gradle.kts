plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
    `maven-publish`
}

dependencies {
    implementation(libs.mindustry.core)
    implementation(libs.arc.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.java.websocket)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.classic)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.mindustry.ide.tool.MainKt")
}

tasks.withType<JavaExec> {
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    listOf(
        "mindustrymit.envFile",
        "mindustrymit.wsHost",
        "mindustrymit.wsPort",
        "mindustrymit.dataRoot",
        "mindustrymit.logDir",
        "mindustrymit.wsToken",
        "logback.rootLevel"
    ).forEach { name ->
        System.getProperty(name)?.let { systemProperty(name, it) }
    }
}

tasks.jar {
    archiveBaseName.set("mindustrymit-server")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes("Main-Class" to "com.mindustry.ide.tool.MainKt")
    }
}

tasks.shadowJar {
    archiveBaseName.set("mindustrymit-server")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest {
        attributes("Main-Class" to "com.mindustry.ide.tool.MainKt")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "mindustrymit-server"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY")}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
