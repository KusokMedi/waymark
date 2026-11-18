# Architecture

## Stack

- Website and application are separate Vite + React + TypeScript projects.
- Styling uses CSS custom properties for design tokens and light/dark themes.
- Tests use Vitest + Testing Library.
- Deployment uses GitHub Actions + GitHub Pages.

## Structure

- `app/` — dashboard application.
- `website/` — project landing site.
- `data/latest.json` — version-controlled source snapshot.
- `scripts/sync-data.mjs` — fetches the official source and writes the snapshot.
- `scripts/assemble.mjs` — combines both builds into `dist/`.

## Data flow

1. `scripts/sync-data.mjs` fetches all ArcGIS features and writes `data/latest.json`.
2. The app loads `data/latest.json` from the same origin.
3. `normalize.ts` validates and normalizes raw features into `AdvisoryRecord` and `Destination`.
4. Persistence (favorites, theme, view, watchlist, cache) uses `localStorage`.

## Routing

The app uses hash-based routing to remain compatible with GitHub Pages static hosting.
EOF