# User guide

## First launch

Waymark walks you through four short steps:

1. **Welcome** — what the app does and what it does not do.
2. **Advisory levels** — how Levels 1–4 work and the higher-risk area note.
3. **Choose countries** — search and select the destinations you want to follow.
4. **Notifications** — optional. Enabling them asks for Android's notification
   permission.

The first synchronization downloads the country catalogue and advisory records.
It establishes the baseline for history and never sends notifications.

## Watchlist

The Watchlist tab is the home screen. Each card shows:

- Country name and ISO monogram.
- Advisory level with its official wording.
- A note when the country contains areas with a higher security risk.
- The official advisory publication date.
- A star for favorites.

Pull the refresh action in the top bar, or use **Settings → Sync now**, to
request an update. Android may delay background work; the banner always shows
the last successful update and any problem.

## Explore

Explore lists every destination in the downloaded catalogue.

- Search by name, ISO alpha-2 or ISO alpha-3.
- Sort by name, advisory level or favorites.
- Tap the add/check button to add or remove a country.
- Tap the star to mark a favorite.

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

- **Appearance** — system/light/dark theme and dynamic color (Android 12+).
- **Synchronization** — automatic updates on/off, interval (6/12/24 hours),
  manual sync.
- **Notifications** — global opt-in; per-country switches are on each country
  page.
- **Data management** — export JSON, export ZIP, import backup, choose merge or
  replace, clear history.
- **About** — version, privacy summary, licenses, official source link.

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
