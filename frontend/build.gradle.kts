import org.gradle.api.tasks.Exec

plugins {
    base
}

val bunExecutable = providers.gradleProperty("bun.executable")
    .orElse(providers.environmentVariable("BUN_EXECUTABLE"))
    .orElse(providers.environmentVariable("BUN_INSTALL").map { "$it/bin/bun" })
    .orElse("bun")

fun Exec.bunCommand(vararg args: String) {
    workingDir = projectDir
    executable = bunExecutable.get()
    this.args = args.toList()
}

tasks.register<Exec>("bunInstall") {
    group = "frontend"
    description = "Install frontend dependencies with Bun."
    bunCommand("install", "--frozen-lockfile")
    inputs.file("package.json")
    inputs.file("bun.lock").optional()
    outputs.dir("node_modules")
}

tasks.register<Exec>("bunDev") {
    group = "frontend"
    description = "Start the Vite development server with Bun."
    bunCommand("run", "dev")
    dependsOn("bunInstall")
}

tasks.register<Exec>("bunTypecheck") {
    group = "verification"
    description = "Run frontend TypeScript type checks."
    bunCommand("run", "typecheck")
    dependsOn("bunInstall")
    inputs.dir("src")
    inputs.file("tsconfig.json")
    inputs.file("tsconfig.node.json")
}

tasks.register<Exec>("bunBuild") {
    group = "build"
    description = "Build the frontend Vite bundle."
    bunCommand("run", "build")
    dependsOn("bunInstall")
    inputs.dir("src")
    inputs.file("index.html")
    inputs.file("package.json")
    inputs.file("tsconfig.json")
    inputs.file("tsconfig.node.json")
    inputs.file("vite.config.ts")
    outputs.dir("dist")
}

tasks.named("check") {
    dependsOn("bunTypecheck")
}

tasks.named("build") {
    dependsOn("bunBuild")
}

tasks.named("clean") {
    doLast {
        delete(layout.buildDirectory)
        delete("dist")
    }
}
