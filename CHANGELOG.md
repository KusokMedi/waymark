# Changelog

All notable changes to Waymark are documented here. The project follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.3.4] - 2026-11-18

### Changed

- Onboarding country selection now runs synchronization directly, so the
  catalogue loads during initial setup; search works as soon as data arrives.
- Widget manual refresh now runs synchronization directly (no WorkManager
  throttling) and updates both data and widget immediately.
- Widget transparency slider uses 5% increments (0–100%).
- Increased spacing between countries in the widget.

### Fixed

- Onboarding search not working at startup.
- Widget manual refresh appearing to work only once per minute.

## [1.3.3] - 2026-11-18

### Changed

- Widget settings now use corner radius 0–20 dp and background opacity 0–100%.
- Widget background opacity applies only to the background; cards, text and
  buttons remain opaque.
- Advisory level is hidden by default in widgets and can be enabled manually.
- Advisory level is no longer shown on Watchlist/Explore cards; it remains
  visible on the country detail screen.
- Onboarding now downloads the country catalogue before the country selection
  step, so countries can be chosen during initial setup.

### Fixed

- Widget refresh/update reliability: manual refresh now runs synchronization
  directly and updates the widget; repeated taps are ignored while syncing.
- Widget settings (save/apply/return) reliably update the widget.
- Onboarding country selection now works when mobile data is available.

## [1.3.2] - 2026-11-18

### Changed

- Onboarding order updated so **Permissions & network** (including mobile data
  option) comes before **Choose countries**, making country selection the final
  step.
- Watchlist, Explore and country detail screens have smoother transitions and
  loading states, with clearer progress/empty-state feedback.
- Home screen widget rewritten for more reliable updates and a cleaner layout,
  with in-widget refresh/settings actions and improved spacing.

### Fixed

- Navigation issue that could prevent returning to Watchlist and send users back
  to Explore.
- Widget refresh, status updates and settings/configuration reliability (save,
  apply and return behavior).
- Minor documentation updates for the current onboarding flow.

## [1.3.1] - 2026-11-18

### Added

- Option to allow using mobile data for synchronization (asked during initial
  setup and configurable in **Settings → Synchronisation**). Default is Wi-Fi
  only.

## [1.3.0] - 2026-11-18

### Changed

- Onboarding country selection shows **Skip** when no countries are selected.
- Watchlist sorting defaults to A–Z with options A–Z, Z–A, Favorites, Level 1–4,
  Level 4–1.
- Advisory date year is shown in country overview/detail; cards, lists and
  widgets show advisory date without year.
- Widget rows have clearer spacing between countries.

### Removed

- Favorite (star) action from Explore/search cards.
- “Default” watchlist sort option.

### Fixed

- Notification sound label updates correctly when switching between default,
  silent and custom sounds.
- Battery optimization action reflects when exemption is already allowed in
  Settings and onboarding.
- Widget settings save and apply reliably and return to the home screen instead
  of the app.
- Widget refresh/update behavior and syncing indicator are more reliable; manual
  refresh is no longer ignored when a sync request is already queued.
- Spacing between countries in widgets when multiple are shown.

## [1.2.0] - 2026-11-18

### Added

- Widget settings (⚙) from the widget header: corner radius, transparent
  background (off by default), and visibility of flag, name, level and date.
- Dynamic color support for widgets when enabled in app settings (Android 12+).
- Sync error details screen in Settings; tap a sync error banner in the app to
  view details.
- Notification sound and priority settings (default priority High).
- Optional battery optimization exemption prompt in onboarding (Permissions step)
  and access from notification settings.
- Additional sorting options: A–Z/Z–A and Level 1–4/4–1 in Explore and Watchlist,
  with Favorites sorting in Watchlist.

### Changed

- Settings reorganized into separate screens (Appearance, Synchronisation,
  Notifications, Data management, About).
- Onboarding notifications step replaced with a Permissions step.
- Widget sync status shows an animated updating indicator while syncing and
  shows `Error, last update at …` on failure; stale "updating" states are
  recovered.
- Reduced duplication in advisory/risk display and improved advisory date
  presentation.
- Adding a country to the watchlist enables per-country notifications by default
  when global notifications are enabled.
- Dynamic color setting applies to widgets as well as the app.

### Removed

- Single-country widget (replaced by the configurable watchlist widget).

### Fixed

- Widgets no longer remain stuck on "Updating…" after an interrupted sync.
- Onboarding screens now adapt correctly to dark theme.

## [1.1.0] - 2026-11-18

### Added

- Emoji country flags across the app and widgets.
- A single-country home screen widget (default 4×2, resizable) with country selection when added.
- Widget sync status text showing when data is updating and when it was last updated.

### Changed

- The watchlist widget is now 2×2 by default, resizable, shows only countries from your watchlist, and is added without a configuration screen.
- Widget refresh now triggers synchronization so update status is visible.
- Refreshed widget appearance with rounded corners and clearer layout.

### Removed

- The separate 3×1 compact widget (replaced by the resizable watchlist widget).

## [1.0.0] - 2026-11-18

### Added

- Native Android application (`com.kusokmedi.waymark`, API 26+).
- Local watchlist with ordering, favorites and per-country notification
  switches.
- Country catalogue and search with ISO alpha-2/alpha-3 matching.
- Country dashboard with official advisory level, official date, official link,
  regional records and locally recorded history chart.
- Explicit advisory level mapping for raw codes `1`, `10`, `2`, `20`, `3`,
  `30`, `4`; unknown codes remain `Unknown`.
- Room storage for countries, advisories, watchlist, history, sync state and
  widget configurations; DataStore preferences.
- Official ArcGIS client with pagination, conditional requests and honest
  failure handling.
- WorkManager synchronization (startup, manual, periodic 6/12/24 hours) with
  exponential backoff and cached-data preservation.
- Optional notifications for worsening, improvement, new regional records and
  significant detail changes; first sync never notifies.
- Jetpack Glance App Widgets: watchlist list/grid and compact card, independent
  configuration per widget ID.
- JSON and ZIP backup/export and validated restore with merge/replace modes.
- English Compose UI with system light/dark theme and dynamic color.
- Unit tests for level mapping, normalization, grouping, change detection and
  backup serialization; instrumented Room tests.
- GitHub Actions CI for Android build/tests/lint and the website.
- Tag-driven Android release workflow with signing secrets and GitHub Releases.

### Changed

- The website was rewritten as a product site for the Android app with a
  clearly labeled sample-data demonstration.
- Documentation was replaced with Android architecture, data source, widget,
  privacy, user guide and release documentation.

### Removed

- The website no longer deploys the legacy React dashboard as the application.

## [0.1.0] - 2026-11-18

### Added

- Initial web prototype of the travel advisory dashboard.
- ArcGIS advisory integration and data snapshots.
- Project website, CI and Pages deployment.
