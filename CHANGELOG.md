# Changelog

All notable changes to Waymark are documented here. The project follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.4.7] - 2026-11-21

### Fixed

- Added a 35-second widget sync timeout guard so a stuck synchronization no longer leaves the widget in an indefinite "Syncing..." state.

## [1.4.6] - 2026-11-21

### Fixed

- Widget refresh now uses the same default `SyncEngine.synchronize()` path as in-app Watchlist refresh, allowing the recent-success fast path instead of forcing a full network sync on every tap.

## [1.4.5] - 2026-11-21

### Fixed

- Removed widget refresh hammering that caused lag on first tap: the Glance action callback now returns immediately, debounce/running feedback/sync run in background, and widget updates happen at key sync points instead of every 700ms.

## [1.4.4] - 2026-11-21

### Fixed

- Made widget refresh button non-blocking: the Glance action callback now returns immediately, while sync animation updates run in a guarded background scope, preventing lag during rapid taps/spam.

## [1.4.3] - 2026-11-21

### Changed

- Onboarding advisory levels now appear immediately without entrance animations.

### Fixed

- Widget refresh now runs the same direct synchronization path as the in-app Watchlist refresh, avoiding WorkManager scheduling delay while keeping immediate feedback and sync animation.

## [1.4.2] - 2026-11-21

### Fixed

- Stabilized widget refresh button: immediate feedback on tap, 1.5s debounce, preserved sync state fields, and removed the 3-second false timeout that marked active syncs as failed.
- Improved widget sync animation with a short dot/progress refresh window while keeping the real sync outcome owned by WorkManager/SyncEngine.

## [1.4.1] - 2026-11-21

### Fixed

- Replaced the letter "W" placeholder with the actual app icon in onboarding and fixed an About screen crash.

## [1.4.0] - 2026-11-21

### Fixed

#### Critical Stability Issues
- **Race Conditions**: Fixed critical race condition in sync engine where firstSync check happened outside transaction, potentially causing duplicate notifications or missed changes during parallel syncs.
- **Thread Safety**: Protected markFailedIfRunning with mutex to prevent concurrent state corruption.
- **Error Handling**: Added comprehensive error handling to all ViewModel operations with proper logging and user-facing error events.
- **Settings Persistence**: Replaced unsafe delay(50) with proper await in syncNowWithMobileData to ensure settings are persisted before sync reads them.

#### Widget System
- **State Management**: Replaced volatile flags with StateFlow-based UpdateState for proper thread-safe widget update coordination.
- **Performance**: Added 200ms throttling to forceUpdateAll to prevent excessive widget refreshes during mass data changes.
- **Overflow Protection**: Fixed widget refresh tick overflow by using Long range instead of Int modulo 1M.

#### Security & Validation
- **Deeplink Safety**: Added validation to check country exists before navigation, preventing crashes from malicious or invalid deeplinks.
- **SQL Safety**: Escaped LIKE special characters (%, _, \) in country search queries.
- **Fingerprint Collisions**: Changed history fingerprints from pipe-delimited to Base64-encoded to prevent character collisions.

#### Data & Performance
- **Database Index**: Added index on history_events.occurredAt for better query performance (database migration 5→6).
- **Stale State Recovery**: Consolidated duplicate stale running sync recovery logic into shared SyncStateDao.recoverStaleRunning() method.

#### Lifecycle & Memory
- **Widget Cleanup**: Used goAsync() in widget receiver onDeleted to ensure database cleanup completes before process termination.
- **Context Documentation**: Added comprehensive documentation about Application Context requirements to prevent memory leaks in SyncEngine, ChangeNotifier, and BackupManager.

#### Error Logging
- **Notifications**: Added warning logs when notification permission is revoked or channels are deleted.
- **Data Parsing**: Added debug logging for invalid URLs and date parsing errors.
- **Network Layer**: Added warning log when pagination MAX_PAGES limit is reached.

#### Sync Scheduling
- **Work Policy**: Changed syncNowFromWidget from REPLACE to KEEP policy to prevent cancelling already-running sync operations.

### Documentation
- Added CODE_AUDIT_FIXES.md with comprehensive analysis of 45 identified issues and their resolution status.
- Documented Application Context requirements across all long-lived components.
- Added comments explaining timeout coordination, notification channel management, and JSON parsing concerns.

## [1.3.20] - 2026-11-21

### Fixed

- Widget refresh button now responds reliably: simplified sync path to use only WorkManager, reduced stale-state timeout from 3 minutes to 30 seconds, added 2-second debounce to prevent rapid duplicate syncs, and added force-sync flag to bypass minimum sync interval. Widget no longer gets stuck in infinite "Syncing..." state.
- Widget UI updates are now immediate for force refreshes, bypassing rate limiting and coalescing delays.

## [1.3.19] - 2026-11-21

### Changed

- Removed advisory date from Watchlist and Explore list cards; advisory date remains visible on the full country detail screen.
- Redesigned the About settings screen with clearer app, privacy, source, and license information.

### Fixed

- Widget refresh no longer stays stuck in an infinite loading state as easily: stale running syncs can be overridden after 60 seconds, and refresh outcomes are handled more reliably.

## [1.3.18] - 2026-11-21

### Changed

- Country detail screen now shows a clear not-found state for invalid links and a richer official advisory card.
- Country cards show advisory dates in Watchlist and Explore, and Explore now supports favorite toggling.
- Widget status text now distinguishes offline from failed syncs and shows higher-risk areas in level text.

### Fixed

- Widget empty state is now tappable to open Waymark.
- Notification permission flow no longer flickers enabled state before Android permission is granted.

## [1.3.17] - 2026-11-21

### Changed

- Redesigned all onboarding steps: welcome hero with value cards, clearer permissions/setup cards, richer country selection with flags and selection feedback, and a polished step indicator.

## [1.3.16] - 2026-11-21

### Changed

- Redesigned the onboarding advisory levels step with richer level cards, action-oriented descriptions, a higher-risk areas example, and a subtle animated reveal.

## [1.3.15] - 2026-11-21

### Changed

- Widget refresh hardening: added widget-triggered sync path, timeout/failure guards, sync-state recovery, and widget update coalescing to improve refresh reliability.

## [1.3.14] - 2026-11-21

### Fixed

- Widget refresh button now provides immediate feedback and triggers updates more reliably.
- Onboarding level list no longer shows duplicated advisory level text inside chips.

## [1.3.13] - 2026-11-21

### Changed

- Level chips keep a consistent width while restoring content-based height.
- Country cards show regional warning counts and provide a direct link to the official advisory when available.

### Fixed

- Explore no longer lists countries with unknown advisory levels.

## [1.3.12] - 2026-11-21

### Changed

- Level chips in the app now use a consistent fixed size for each variant.

### Fixed

- Widget country items now have clearer, reliably rendered spacing.

## [1.3.11] - 2026-11-21

### Fixed

- Widget country cards now use a consistent 2 dp spacing between items.
- Widget no longer shows an infinite/stuck loading state when a previous sync
  was interrupted; stale sync status is handled correctly.
- Widget configuration is no longer rewritten on every refresh.

## [1.3.10] - 2026-11-20

### Fixed

- Widget country cards now use consistent corner rounding and spacing.
- Widget layouts now adapt to compact and larger widget sizes, filling the
  available area correctly.

## [1.3.9] - 2026-11-20

### Changed

- Release version 1.3.9.

## [1.3.8] - 2026-11-19

### Changed

- Release version 1.3.8.

## [1.3.7] - 2026-11-20

### Changed

- Widget defaults to 2×2, resizable down to 2×1.
- Widget no longer has settings button; all settings removed, fixed defaults.
- Widget shows only level + color (no advisory details).
- Widget status text changed to "Loading...".
- Widget icons replaced with vector icons (refresh).
- Widget per-position corner rounding.
- Widget transparency slider snaps to 5% increments.
- Spam protection for refresh button (2s debounce).
- Onboarding progress bar fully visible.
- Removed "Retry" button from onboarding.
- WiFi sync fixed (no longer requires mobile data).
- SyncBanner: removed cached data message, shortened date format.
- Theme settings: Dynamic color disabled on Android < 12.

## [1.3.6] - 2026-11-18

### Changed

- Widget country cards now have per-position corner rounding: top card rounds
  top corners, bottom card rounds bottom corners, middle cards are square,
  single card rounds all corners.
- Widget transparency slider now snaps to 5% increments (0, 5, 10, …, 100).

## [1.3.5] - 2026-11-18

### Changed

- Widget now shows advisory level by default (instead of advisory date).
- Watchlist and Explore cards now show advisory level instead of advisory date.

## [1.3.4] - 2026-11-18

### Changed

- Onboarding country selection now runs synchronization directly, so the
  catalogue loads during initial setup; search works as soon as data arrives.
- Widget manual refresh now runs synchronization directly (no WorkManager
  throttling) and updates both data and widget immediately.
- Widget transparency slider uses 5% increments (0-100%).
- Increased spacing between countries in the widget.

### Fixed

- Onboarding search not working at startup.
- Widget manual refresh appearing to work only once per minute.

## [1.3.3] - 2026-11-18

### Changed

- Widget settings now use corner radius 0-20 dp and background opacity 0-100%.
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
- Watchlist sorting defaults to A-Z with options A-Z, Z-A, Favorites, Level 1-4,
  Level 4-1.
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
- Additional sorting options: A-Z/Z-A and Level 1-4/4-1 in Explore and Watchlist,
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
