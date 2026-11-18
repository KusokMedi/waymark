# Development

## Requirements

- Node.js 20+
- npm

## Install

```bash
npm ci
```

## Run

```bash
npm run dev:app      # http://localhost:5173/waymark/app/
npm run dev:website  # http://localhost:5174/waymark/
```

## Tests

```bash
npm --workspace app run test
npm --workspace app run typecheck
npm --workspace app run lint
```

## Build

```bash
npm run build
```

## Data snapshot

```bash
npm run sync:data
```

## Troubleshooting

- If the app cannot load data locally, run `npm run sync:data` to create `app/public/data/latest.json`.
- GitHub Pages base paths are configured in each Vite config.
EOF