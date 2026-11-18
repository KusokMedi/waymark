# User guide

## First launch

Waymark walks you through four short steps:

1. **Welcome** — what the app does and what it does not do.
2. **Advisory levels** — how Levels 1–4 work and the higher-risk area note.
3. **Choose countries** — search and select the destinations you want to follow.
4. **Permissions** — optional. Enable advisory notifications (Android may ask for
   notification permission), choose whether to allow mobile data for updates
   (default is Wi-Fi only), and, if you want, allow Waymark to ignore battery
   optimizations to reduce delays in background updates.

The first synchronization downloads the country catalogue and advisory records.
It establishes the baseline for history and never sends notifications.

## Watchlist

The Watchlist tab is the home screen. Each card shows:

- Country flag and name.
- Advisory level with its official wording and a note when the country contains
  areas with a higher security risk.
- The official advisory publication date.
- A star for favorites.

You can sort the watchlist (A–Z by default, Z–A, Favorites, Level 1–4, Level 4–1).
Tap the refresh action in the top bar, or use **Settings → Synchronisation →
Sync now**, to request an update. If a sync fails, tap the status banner to view
synchronization details. Android may delay background work.

## Explore

Explore lists every destination in the downloaded catalogue.

- Search by name, ISO alpha-2 or ISO alpha-3.
- Sort by A–Z, Z–A, Level 1–4 or Level 4–1.
- Tap the add/check button to add or remove a country.

When you add a country to your watchlist and advisory notifications are enabled,
per-country notifications are enabled by default for that country.

## Country dashboard

Tap any country to see:

- The advisory level, official wording and higher-risk area indicator.
- The official advisory date.
- Departure notice text when the source publishes one.
- A button that opens the official advisory in your browser.
- Regional records for the same country, when the source publishes them.
- Locally recorded history and a chart of recorded level transitions.
- Watchlist controls and a per-country notification switch.
- The last successful synchronization time on this device.

The chart only connects transitions that Waymark actually observed. It does not
invent earlier values.

## History

History lists meaningful changes for countries in your watchlist:

- Advisory worsened.
- Advisory improved.
- New regional record.
- Significant advisory detail change.

Events are deduplicated and retained until you clear them. Use the bin icon in
the History tab and confirm to delete all recorded events. Current advisories
and your watchlist are kept.

## Settings

Settings are organized into categories:

- **Appearance** — system/light/dark theme and dynamic color (Android 12+; also
  affects widgets).
- **Synchronisation** — automatic updates on/off, allow mobile data for updates
  (Wi-Fi only by default), interval (6/12/24 hours), manual sync, and
  synchronization details.
- **Notifications** — global opt-in, notification sound and priority (default
  High), and battery optimization settings.
- **Data management** — export JSON, export ZIP, import backup, choose merge or
  replace, clear history.
- **About** — version, privacy summary, licenses, official source link.

Per-country notification switches are available on each country page. If a sync
fails, you can tap the error banner in the app to view details.

## Offline behavior

Downloaded advisories remain readable without a connection. When a refresh
fails:

- Cached records stay intact.
- The last successful update time stays visible.
- The failure is reported in the sync banner.
- Missing information is shown as `Unknown`, never as Level 1.

## Backup and restore

Exports contain countries, cached advisories, watchlist, history, widget
configurations and preferences, plus a schema version and app identifier.

Import validates the file first:

- Invalid or incompatible files are rejected with a message.
- Unknown fields from newer versions are ignored safely.
- Merge mode keeps existing data and adds the backup.
- Replace mode deletes current local data first; use it deliberately.

Backups never leave the device unless you save or share them yourself.

## Data accuracy

Waymark displays what the official source publishes. It does not interpret
advisory text, rate safety independently or guarantee that a destination is
safe. Always read the official advisory. Advisories are written for U.S.
citizens and are not a universal safety rating.
