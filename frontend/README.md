# MindustryMIT Web

Vue 3 + Vite + TDesign Vue Next frontend for MindustryMITStudio.

## Requirements

- Bun 1.3 or newer
- Kotlin backend running at `ws://127.0.0.1:19190`

## Development

```bash
bun install
bun run dev
```

Override the backend endpoint with:

```bash
VITE_WS_URL=ws://127.0.0.1:19190
```

## Build

```bash
bun run typecheck
bun run build
```

From the repository root, Gradle exposes the same build entry:

```bash
bash ./gradlew :frontend:build
```
