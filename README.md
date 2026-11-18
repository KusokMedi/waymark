# Waymark

**Know before you go.**

Waymark is an independent, local-first Android app for monitoring official
U.S. Department of State Travel Advisories. It keeps a personal watchlist,
records meaningful advisory changes over time, and provides real Android
home-screen widgets — without accounts, a Waymark server, analytics or
advertising.

- **Package:** `com.kusokmedi.waymark`
- **Minimum Android version:** 8.0 (API 26)
- **License:** GPL-3.0
- **Website:** https://kusokmedi.github.io/waymark/

## Features

- Browse and search the full destination catalogue from the official source.
- Watchlist with ordering, favorites and per-country notification switches.
- Country dashboard with advisory level, official date, official link,
  regional records and locally recorded history.
- Advisory levels are mapped explicitly, including the special raw codes
  `10`, `20` and `30`. Unknown codes are shown as `Unknown`, never as Level 1.
- Background synchronization through WorkManager, manual refresh, honest
  offline states and preserved cached data.
- Optional notifications for worsening, improvement, new regional records and
  significant detail changes. The first synchronization never notifies.
- Real App Widget (Jetpack Glance): a 2×2 resizable, configurable watchlist
  widget with sync status and quick refresh/settings.
- JSON and ZIP backup/restore with validation and merge/replace modes.
- System light/dark theme and dynamic color on Android 12+.

## Privacy

Waymark has no backend. Watchlists, history, widget settings and preferences
stay in local storage. The only network request retrieves public advisory data
from the official U.S. Department of State ArcGIS service. See
[docs/PRIVACY.md](docs/PRIVACY.md).

## Requirements

- JDK 17
- Android SDK Platform 35 and Build-Tools 35.0.0
- Android Studio (recommended) or the command-line SDK

## Build

```bash
cd android
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # adjust if needed
JAVA_HOME=/path/to/jdk-17 ./gradlew :app:assembleDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/`. Debug
builds use the application ID suffix `.debug` and can be installed alongside a
release build.

## Test

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Instrumented tests require a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Website

The static project website lives in `website/` and is deployed to GitHub Pages.

```bash
cd website
npm ci
npm run build
```

## Data source

Advisories are published by the U.S. Department of State. Waymark is not
affiliated with or endorsed by the U.S. Department of State. Advisories are
written for U.S. citizens, nationals and legal residents and are not a
universal safety rating. See [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md).

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Data sources](docs/DATA_SOURCES.md)
- [Development](docs/DEVELOPMENT.md)
- [Releases](docs/RELEASES.md)
- [User guide](docs/USER_GUIDE.md)
- [Widgets](docs/WIDGETS.md)
- [Privacy](docs/PRIVACY.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## License

GPL-3.0. See [LICENSE](LICENSE).
