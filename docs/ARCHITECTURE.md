# Architecture

## Overview

Waymark is a single-module native Android application written in Kotlin with
Jetpack Compose and Jetpack Glance. The module is split by package
responsibility instead of by Gradle module, which keeps the build simple while
preserving clear boundaries.

```
android/app/src/main/java/com/kusokmedi/waymark/
├── WaymarkApp.kt          Application, AppContainer (manual DI)
├── SyncWorker.kt          WorkManager worker and scheduler
├── data/
│   ├── AdvisoryRepository.kt   Reads Room, produces UI models
│   ├── NetworkMonitor.kt       Connectivity check
│   ├── local/                  Room entities, DAOs, database, DataStore
│   └── remote/                 Official ArcGIS client
├── domain/                Levels, ISO catalogue, normalization, changes
├── sync/                  SyncEngine and notification handling
├── backup/                JSON and ZIP export/import
├── widget/                Glance widgets, receivers, config activity
└── ui/                    Compose screens, theme, navigation
```

## Layering

| Layer | Responsibility |
|---|---|
| Presentation (`ui`, `widget`) | Compose screens, navigation, Glance widgets |
| Domain (`domain`) | Advisory levels, normalization, grouping, change detection |
| Data (`data`) | Room, DataStore, official HTTP client, repository |
| Background (`sync`, `SyncWorker`) | WorkManager scheduling, sync transactions, notifications |
| Backup (`backup`) | Serialization, validation and transactional restore |

Business rules (level mapping, change detection) are pure Kotlin objects with
JVM unit tests and no Android dependencies.

## Advisory data model

The source publishes one record per destination, including region records that
share the country ISO code. Records are grouped by ISO alpha-3 code (falling
back to the normalized name when no ISO code exists).

The country record is the record whose name matches the ISO catalogue name. If
it cannot be identified and grouped records disagree on the level, the
aggregate level stays unknown instead of guessing.

Persisted tables:

| Table | Purpose |
|---|---|
| `countries` | Stable identity, ISO codes, display name |
| `advisories` | Every source record, including regions and raw level code |
| `watchlist` | Selection, order, favorite, per-country notification switch |
| `history_events` | Deduplicated meaningful changes, retained indefinitely |
| `sync_state` | Last success/attempt, status, message, validators, count |
| `widget_configs` | Independent configuration per App Widget ID |

Schema version 1 is exported to `android/app/schemas/` and committed. Migrations
are explicit; there is no destructive fallback.

## Synchronization flow

1. Check connectivity. If offline, record the attempt and keep cached data.
2. `GET` the official ArcGIS layer with `resultOffset` pagination,
   `returnGeometry=false`, and conditional headers when available.
3. `304 Not Modified` is a success and does not rewrite data.
4. Normalize each feature; records without a destination name are skipped.
5. Group into countries and compute change events against stored snapshots.
6. In one Room transaction: upsert countries, replace advisories, insert new
   history events for watchlisted countries, write sync state.
7. Notify only if notifications are enabled, only for watchlisted countries,
   and never during the first successful synchronization.
8. Refresh widgets from the local database.

Any failure preserves previously stored data. The last successful timestamp is
kept separately from the advisory publication date.

## Change detection

Snapshots contain the aggregate level, higher-risk flag, advisory date, region
names and a hash of all record content. Events are produced only when a
meaningful difference exists:

- severity increased → `WORSENED`
- severity decreased → `IMPROVED`
- a region name was added → `REGIONAL`
- other content changed while level and regions stayed stable → `DETAILS`

Each event has a stable fingerprint and the history table enforces uniqueness,
so repeated syncs cannot duplicate events. Unknown levels never produce
fabricated level transitions.

## Widgets

Jetpack Glance renders real App Widgets. Widgets read from the shared Room
database; they never perform network work. Configuration is stored per App
Widget ID, so multiple instances are independent. See
[WIDGETS.md](WIDGETS.md).

## Background work

`SyncScheduler` enqueues a unique periodic request (6–24 hours, default 12) and
a unique one-time request for app start and manual refresh, both constrained to
a connected network with exponential backoff. Android may delay background
work; the app never promises exact timing. Disabling automatic updates cancels
the periodic request.

## Dependency choice

- Jetpack Compose + Material 3 for UI.
- Room for structured data, DataStore for preferences.
- WorkManager for persistent background synchronization.
- Glance for App Widgets.
- OkHttp + kotlinx.serialization for the official JSON service.
- Manual `AppContainer` instead of a DI framework: the graph is small and
  explicit.

## Known limitations

- Country flags are rendered as emoji derived from ISO country codes; their
  appearance depends on the device and system font.
- Glance/RemoteViews cannot express arbitrary layouts; the widget builder is
  limited to supported element toggles and 1–3 columns.
- The official ArcGIS layer is the only source. If it changes shape or
  disappears, cached data is preserved and the UI reports the failure.
- Release signing requires a keystore that is not committed to the repository.
