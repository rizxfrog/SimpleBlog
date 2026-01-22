# Repository Guidelines

## Project Structure & Module Organization

- `src/` holds the application code for this Vue 3 + Vite frontend.
- `src/components/` contains reusable UI components (PascalCase Vue SFCs).
- `src/views/` contains route-level pages (e.g., `AdminEditor.vue`).
- `src/router/` defines route configuration in `index.ts`.
- `src/stores/` holds Pinia stores (`auth.ts`, `ui.ts`).
- `src/assets/` stores global styles and static assets (e.g., `main.css`).
- `src/apollo.ts` wires the Apollo GraphQL client.
- `index.html` is the Vite entry HTML; `vite.config.ts` configures bundling.

## Build, Test, and Development Commands

- `npm install` installs dependencies.
- `npm run dev` starts the Vite dev server with hot reload.
- `npm run build` runs `vue-tsc -b` type-checking, then builds production assets.
- `npm run preview` serves the production build locally for a final check.

No test script is configured yet; add one if you introduce tests.

## Coding Style & Naming Conventions

- Indentation: 2 spaces in `.vue` templates and TypeScript files.
- Use single quotes in TS/JS imports and strings (matches existing files).
- Vue SFCs use `<script setup lang="ts">` and PascalCase filenames.
- Keep reusable UI in `components/` and pages in `views/`.

No formatter or linter is configured; keep diffs tidy and consistent.

## Testing Guidelines

- There is no testing framework configured in `package.json`.
- If adding tests, prefer a `*.spec.ts` naming pattern and document the new script.

## Commit & Pull Request Guidelines

- No Git history is available in this directory to infer conventions.
- Use short, imperative commit summaries (e.g., "Add admin post editor").
- PRs should include: a clear description, linked issues (if any), and UI screenshots
  for visual changes.

## Configuration & Environment

- GraphQL endpoint is configured via `VITE_API_URL` in `src/apollo.ts`.
- Default fallback is `http://localhost:8888/graphql`; document any env changes.
