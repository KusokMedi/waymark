# Development

## Prerequisites

- JDK 17 (Gradle and AGP 8.7 are not compatible with JDK 21+ in this setup)
- Android SDK Platform 35
- Android SDK Build-Tools 35.0.0
- Android SDK Platform-Tools
- Node.js 20+ for the website

The project was verified with the Android command-line SDK and Gradle 8.9
(wrapper).

## Local setup

```bash
git clone https://github.com/KusokMedi/waymark.git
cd waymark/android
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

`local.properties` is ignored by Git.

## Build

```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:assembleDebug
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`.

Release builds are minified and shrink resources:

```bash
./gradlew :app:assembleRelease
```

Without `android/keystore.properties` the release APK is unsigned, which is
useful for local verification but not for distribution. See
[RELEASES.md](RELEASES.md).

## Checks

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

Unit tests cover level mapping, unknown codes, ISO lookups, normalization,
grouping, change detection and backup serialization. Instrumented tests cover
Room schema behavior, cascade deletion, widget configuration independence and
history fingerprint deduplication.

## Project layout

```
android/                 Gradle project and app module
  app/src/main/          Production sources and resources
  app/src/test/          JVM unit tests
  app/src/androidTest/   Instrumented tests
  app/schemas/           Exported Room schema (committed)
website/                 Static project website (Vite + React)
docs/                    Documentation
.github/workflows/       CI, release and Pages workflows
```

## Room schema changes

1. Increment `@Database(version = …)`.
2. Add an explicit `Migration` to `data/local/Migrations.kt`.
3. Export the schema to `android/app/schemas/` (KSP does this automatically).
4. Add a migration test in `androidTest`.
5. Never add `fallbackToDestructiveMigration()`.

## Website

```bash
cd website
npm ci
npm run dev      # http://localhost:5174
npm run build
```

`VITE_BASE_PATH` overrides the GitHub Pages base path (default `/waymark/`).

## Style

- Kotlin official code style.
- Compose UI text in English.
- Business logic stays independent of Android UI classes.
- Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`,
  `perf:`, `chore:`.
- One logical change per commit.
