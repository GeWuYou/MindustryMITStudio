# Repository Guidelines

## Project Structure & Module Organization

`server/` is the Kotlin backend module. Main code lives in `src/main/kotlin/com/mindustry/ide/tool/`; JSON protocol, parsing, WebSocket API, and documentation utilities are under `src/main/kotlin/com/mindustry/ide/tool/json/`. Tests live in `src/test/kotlin/` and should mirror the package of the code under test.

## Build, Test, and Development Commands

- `bash ../gradlew :server:run` starts the WebSocket backend on the default port.
- `bash ../gradlew :server:test` runs Kotlin/JUnit Platform tests.
- `bash ../gradlew :server:check` runs backend verification.
- `bash ../gradlew :server:shadowJar` creates `server/build/libs/mindustrymit-server-<version>.jar`.
- `bash ../gradlew :server:publish -Pversion=<version>` publishes when GitHub Packages credentials are present.

Run commands from the repository root, or keep the `../gradlew` prefix when inside `server/`.

## Coding Style & Naming Conventions

Use Kotlin idioms already present in the module: four-space indentation, explicit package declarations, data classes for message/data shapes, and concise functions with clear validation errors. Keep WebSocket message names aligned with `WebSocketDataType` values and avoid changing protocol names without updating the frontend client.

## Testing Guidelines

Use `kotlin.test` assertions and `@Test` methods. Prefer descriptive camelCase test names that state behavior, for example `rejectsIncompleteExpression`. Add or update tests for parser precedence, JSON path behavior, protocol serialization, and error handling when those areas change.

## Security & Configuration Tips

Default local endpoint is `ws://localhost:19190`, with the development server binding to `0.0.0.0:19190` for WSL/host access. Configuration is provided through JVM system properties, environment variables, or `server/.env`; do not hard-code local secrets, absolute user paths, or production tokens in tests or examples.

## Commit & Pull Request Guidelines

Use scoped Conventional Commit messages for backend changes, such as `feat(jsonapi): add field lookup` or `refactor(server): simplify startup`.
