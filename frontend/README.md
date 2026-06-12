# MindustryMIT Web

Vue 3 + Vite + TDesign Vue Next frontend for MindustryMITStudio.

## Requirements

- Bun 1.3 or newer
- Kotlin backend running at `ws://localhost:19190`

## Development

```bash
bun install
bun run dev
```

Override the backend endpoint with:

```bash
cp .env.example .env
```

Edit `.env` when the backend is exposed on a different address:

```dotenv
VITE_WS_URL=ws://localhost:19190
```

For WSL localhost forwarding issues, try `ws://127.0.0.1:19190` or
`ws://[::1]:19190` depending on which address Windows exposes.

## Build

```bash
bun run typecheck
bun run build
```

From the repository root, Gradle exposes the same build entry:

```bash
bash ./gradlew :frontend:build
```
