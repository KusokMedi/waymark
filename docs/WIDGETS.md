# Widgets

Waymark provides real Android App Widgets built with Jetpack Glance. They are
not web views, shortcuts or in-app simulations.

## Available widgets

| Widget | Typical size | Behavior |
|---|---|---|
| Waymark Watchlist | 4×2 and larger | List or grid of selected countries, with a refresh action |
| Waymark Compact | 3×1 | A single country card |

Both widgets are resizable. Because Glance uses `SizeMode.Responsive`, content
adapts to the launcher-provided size rather than assuming one fixed layout.

## Configuration

Adding a widget opens **Widget settings**:

- **Layout** — compact, list or grid.
- **Columns** — 1 to 3 for grid layouts.
- **Show advisory date** — official publication date.
- **Show advisory level** — level number and wording.
- **Show flag monogram** — ISO monogram (see limitations).
- **Show country name**.
- **Transparent background** — translucent surface.
- **Selected countries** — search, select, reorder with up/down buttons.
- If no countries are selected, the widget follows your current watchlist.

Every widget instance stores its own configuration keyed by App Widget ID, so
two widgets on the same home screen can show different countries and layouts.
Configuration survives process restarts and device reboots because it lives in
the Room database.

## Behavior

- Widgets read from the local database only; they never start network work.
- Content refreshes after a successful synchronization and after configuration
  changes.
- The refresh icon in the widget header triggers a redraw from local data.
- Tapping a country opens its dashboard in the app.
- With an empty database the widget shows a clear empty state instead of a
  fabricated advisory.
- Deleting a widget removes its stored configuration.

## Data shown

Every country row can show:

- Country name (optional).
- Flag monogram (optional).
- Advisory level with official wording (optional).
- Advisory publication date (optional).

The widget never shows a level when the source data is unknown; it shows
`Unknown`.

## Limitations

Android's widget APIs constrain what is possible. Waymark does not promise
arbitrary layouts or interactions:

- Glance/RemoteViews supports a limited set of views; scrolling lists are
  supported through Glance's `LazyColumn`, but nested scrolling and free-form
  positioning are not.
- Columns are limited to 1–3.
- Exact dimensions are controlled by the launcher; `SizeMode.Responsive`
  provides sensible breakpoints rather than pixel-perfect layouts.
- Widget updates are subject to Android scheduling. There is no guaranteed
  refresh time.
- Country flags currently render as styled ISO monograms. A bundled flag asset
  set is not yet included; the asset contract is documented in
  [ARCHITECTURE.md](ARCHITECTURE.md).

## Testing

Instrumented tests verify that widget configurations are independent per widget
ID and survive storage round-trips. Launcher rendering should be checked
manually on at least one Android 8–11 device and one Android 12+ device.
