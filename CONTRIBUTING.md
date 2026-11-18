# Contributing

Thanks for helping Waymark. The app is a local-first Android client for
official U.S. Department of State travel advisories.

## Requirements

- JDK 17
- Android SDK Platform 35, Build-Tools 35.0.0
- Node.js 20+ for the website

## Android checks

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Instrumented tests need a device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Website checks

```bash
npm ci
npm --workspace website run typecheck
npm --workspace website run build
```

## Rules

- Do not add accounts, cloud sync, analytics, telemetry or advertising SDKs.
- Do not contact unofficial advisory sources.
- Unknown or missing advisory data must stay `Unknown`; never default to
  Level 1.
- Preserve cached data when a network request fails.
- Add or update tests for behavior changes.
- Update documentation when behavior changes.
- Keep user-visible text in English.

## Data changes

- Level mapping lives in `domain/AdvisoryLevels.kt` and must remain explicit.
- New raw codes require a documented mapping and tests; unknown codes must stay
  unknown.
- Room schema changes require a new version, an explicit migration and a
  migration test. Destructive fallbacks are not accepted.

## Commits

Conventional Commits in the imperative mood:

```
feat: add regional advisory filter
fix: keep cached advisories when the source is offline
docs: document widget limitations
test: cover unknown level codes
```

One logical change per commit. Do not mix formatting with behavior changes.

## Pull requests

- Explain the user-visible effect.
- List the checks you actually ran.
- Note anything that is not verified (for example, no device available for
  instrumented tests).
