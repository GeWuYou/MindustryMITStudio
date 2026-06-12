# Repository Guidelines

## Project Structure & Module Organization

`frontend/` is the Vue 3 client. Application entry files are `src/main.ts` and `src/App.vue`; routes are in `src/router/`; Pinia stores are in `src/stores/`; backend WebSocket access is in `src/api/`; views are in `src/views/`; static assets are split between `public/` and `src/assets/`.

## Build, Test, and Development Commands

- `bun install` installs dependencies from `bun.lock`.
- `bun run dev` starts the Vite development server.
- `bun run typecheck` runs `vue-tsc --noEmit`.
- `bun run build` type-checks and builds the production bundle.
- `bun run preview` serves the built bundle locally.
- `bash ../gradlew :frontend:build` runs the same build through Gradle.

Set `VITE_WS_URL=ws://127.0.0.1:19190` when pointing the client at a non-default backend.

## Coding Style & Naming Conventions

Use TypeScript and Vue single-file components. Keep component names in PascalCase, store files in camelCase or lower-case descriptive names, and exported types/classes in PascalCase. Follow the existing two-space indentation in Vue templates and TypeScript files. Prefer TDesign Vue Next components and `tdesign-icons-vue-next` icons for UI consistency.

## Testing Guidelines

No unit test framework is currently configured for the frontend. Treat `bun run typecheck` and `bun run build` as required verification for frontend changes. For visible UI changes, manually test the affected route in Vite and include screenshots or a short description in the pull request.

## API Integration Guidelines

Keep protocol type names in `src/api/mindustrymit.ts` synchronized with the backend `WebSocketDataType`. Preserve the request shape where `content` is JSON-stringified before being sent over WebSocket.

## Commit & Pull Request Guidelines

Use scoped Conventional Commit messages for frontend changes, such as `feat(frontend): add workspace view` or `fix(ui): handle websocket disconnect`.
