# Repository Guidelines

## Project Structure & Module Organization

This is a Gradle multi-module project for MindustryMITStudio.

- `server/` contains the Kotlin WebSocket backend and Mindustry JSON tooling.
- `frontend/` contains the Vue 3 + Vite + TDesign web client.
- `gradle/`, `gradlew`, `settings.gradle.kts`, and `build.gradle.kts` define shared build configuration.
- `.github/workflows/` contains CI and release automation.

Module-specific contributor notes live in `server/AGENTS.md` and `frontend/AGENTS.md`. Follow the nearest `AGENTS.md` for files you edit.

## Build, Test, and Development Commands

- `bash ./gradlew build` builds both backend and frontend modules.
- `bash ./gradlew check` runs backend tests and frontend type checks.
- `bash ./gradlew :server:run` starts the backend WebSocket server.
- `bash ./gradlew :server:shadowJar` builds the executable backend jar used by CI and releases.
- `bash ./gradlew :frontend:bunDev` starts the frontend Vite dev server through Gradle.

The frontend also supports direct Bun commands from `frontend/`; see `frontend/AGENTS.md`.

## Coding Style & Naming Conventions

Keep changes scoped to the module being edited. Kotlin code uses package paths under `com.mindustry.ide.tool`; prefer descriptive class names and Kotlin test names such as `buildsBinaryTreeByOperatorPrecedence`. Frontend source uses TypeScript, Vue single-file components, Pinia stores, and TDesign components.

Do not commit generated output such as `build/`, `server/build/`, `frontend/dist/`, or `frontend/node_modules/`.

## Testing Guidelines

Run the smallest relevant check before submitting:

- Backend-only changes: `bash ./gradlew :server:test`
- Frontend-only changes: `bash ./gradlew :frontend:check`
- Cross-module changes: `bash ./gradlew check`

Add tests for parser, protocol, or data behavior changes under the affected module.

## Commit & Pull Request Guidelines

History uses Conventional Commit style, often with scopes, for example `feat(jsonapi): ...`, `fix: ...`, `refactor(server): ...`, and `chore(build): ...`. Keep messages imperative and scoped.

Pull requests should describe the change, list commands run, link related issues when available, and include screenshots or recordings for visible frontend changes.

## Security & Configuration Tips

The backend listens on `ws://127.0.0.1:19190` by default. Use `mindustrymit.wsToken` for token-protected local runs and `mindustrymit.dataRoot` to control document/data storage.
