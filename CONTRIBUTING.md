# Contributing

## Setup

```bash
npm ci
npm run sync:data
```

## Checks

```bash
npm --workspace app run test
npm --workspace app run typecheck
npm --workspace app run lint
npm run build
```

## Commits

Use Conventional Commits (feat:, fix:, docs:, test:, ci:, chore:).

## Pull requests

- Keep changes focused.
- Add tests for new behavior.
- Update documentation when needed.
EOF