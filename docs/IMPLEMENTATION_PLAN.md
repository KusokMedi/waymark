# Waymark Implementation Plan

## Audit summary

The repository currently contains:

- `app/`, a Vite + React + TypeScript travel advisory dashboard.
- `website/`, a static Vite + React project website.
- `data/latest.json` and `app/public/data/latest.json`, ArcGIS snapshots.
- `scripts/sync-data.mjs`, snapshot synchronizer.
- GitHub Actions for CI, Pages deployment, and data sync.
- Documentation for the web implementation.

The web application is useful as a reference for data semantics and UI intent, but it is not an Android application. The advisory mapping, unknown-code handling, snapshot format, and source research are retained.

## Target architecture

```
waymark/
├── android/                 Native Android application
│   ├── app/                 Single app module, package com.kusokmedi.waymark
│   └── gradle/              Wrapper and version catalog
├── website/                 Static project website (GitHub Pages)
├── data/                    Optional retained snapshot (reference/CI)
├── docs/                    Product and engineering documentation
├── .github/workflows/       CI, release, and Pages workflows
├── README.md
├── CHANGELOG.md
└── LICENSE
```

The Android module is split internally by package responsibility rather than by Gradle module to keep the build simple and maintainable:

- `data/remote`, official ArcGIS client and DTO parsing.
- `data/local`, Room entities, DAOs, database, DataStore preferences.
- `domain`, advisory levels, normalization, change detection, models.
- `sync`, WorkManager worker, sync coordinator, notifications.
- `ui`, Compose screens, theme, navigation.
- `widget`, Glance widgets and receivers.
- `backup`, JSON/ZIP export and validated restore.

## Data source decision

Primary source remains the official U.S. Department of State ArcGIS layer:

- Reference page: https://travel.state.gov/en/international-travel/travel-advisories.html
- Feature service: https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer
- Layer: `54`, "Travel Advisory Levels (NEW)"

Verified fields from the existing implementation: `OBJECTID`, `GlobalID`, `NAME`, `LEVEL_`, `URL`, `ADVDATE`, `ISO_3`, `EditDate`, `AD_OD_Status`, `AD_OD_Text`, `AD_OD_Date`.

The client uses pagination (`resultOffset` / `resultRecordCount`), `returnGeometry=false`, and a bounded page loop. It never fabricates missing data.

## Advisory semantics

Raw codes are mapped explicitly:

| Raw code | Level | Higher-risk areas |
|---:|---:|---|
| 1 | 1 | no |
| 10 | 1 | yes |
| 2 | 2 | no |
| 20 | 2 | yes |
| 3 | 3 | no |
| 30 | 3 | yes |
| 4 | 4 | no |
| anything else | unknown | unknown |

Unknown values are stored and displayed as `Unknown`; they are never converted to Level 1 and never sorted as ordinary integers.

## Local data model

Room tables:

- `countries`, stable catalogue identity, ISO codes, display name.
- `advisories`, source records, including region records, with raw code and official dates.
- `watchlist`, selected countries, ordering, favorite flag, notification preference.
- `history_events`, deduplicated meaningful changes with a stable fingerprint.
- `sync_state`, last success/attempt, status, error, ETag/Last-Modified if available.
- `widget_configs`, independent configuration per widget ID.

History is retained indefinitely until the user clears it explicitly. No destructive migrations.

## Synchronization

- WorkManager unique work (`waymark-sync`) with network constraint and exponential backoff.
- Periodic work every 12 hours (source update cadence; Android may delay it).
- One-time sync on app start and manual refresh.
- Partial page failures preserve previously stored records.
- HTTP `If-None-Match` / `If-Modified-Since` are sent when available; `304` is treated as success without rewriting data.
- First successful sync establishes a baseline and produces no notifications.

## Application experience

- Onboarding: introduction, level explanation, privacy, manual country selection, notification opt-in.
- Bottom navigation: Watchlist, Explore, History, Settings.
- Watchlist cards show flag/monogram, name, level text, higher-risk indicator, advisory date.
- Country detail: advisory summary, official link, regions, last sync, history chart.
- Explore: search, alphabetical/severity/favorite sorting, add/remove.
- Settings: theme, dynamic color, sync interval/manual sync, notifications, backup/restore, privacy, licenses, about.

## Widgets

Jetpack Glance is used for real App Widgets. The current widget is a single
watchlist widget:

- Shows the countries in the user's watchlist, in watchlist order.
- Displays country flag, country name, advisory level, and last update.
- Resizable from 2 by 2 down to 2 by 1.
- Tapping a country opens its dashboard.
- Shows an empty state when the watchlist is empty.
- Refreshes automatically after synchronization and via the refresh action.

There is no separate widget configuration activity. Appearance is fixed to a
default (12 dp corner radius, solid surface background). Widgets observe the
shared local database and are refreshed after sync; they do not run their own
network jobs.

## Website

Preserve the existing static site and content. Update it to present the Android product, add an interactive sample-data demonstration, label all sample data clearly, keep GitHub Pages base-path compatibility, and link to GitHub Releases. Placeholder screenshot regions remain clearly labeled until real screenshots are supplied.

## Testing

- Pure Kotlin unit tests: level mapping, unknown codes, ISO handling, normalization, grouping, change detection, backup serialization.
- Repository/DAO tests with Robolectric + in-memory Room where practical.
- Widget configuration tests for independent instances.
- CI runs `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- Instrumented tests are provided for critical Room/Compose flows but require a device/emulator.

## Release

- Semantic versioning and `v*` tags.
- Debug builds use the debug signing config.
- Release signing is configured only from environment variables / secrets; no keystore is committed.
- GitHub Actions builds APKs and attaches them to a GitHub Release.
- Pages deployment remains independent of the Android build.

## Risks and limitations

- No local Android SDK was initially present; the implementation is verified with a locally installed command-line SDK where possible and by CI otherwise.
- Glance/RemoteViews cannot render arbitrary layouts; the current widget uses a fixed layout.
- Country flag assets: a bundled vector set is not yet complete; a styled flag/monogram fallback is used and the asset contract is documented.
- ArcGIS remains the only source. If it changes or disappears, cached data is preserved and the UI reports the failure honestly.
