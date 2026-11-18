# Changelog

All notable changes to Waymark are documented here. The project follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
