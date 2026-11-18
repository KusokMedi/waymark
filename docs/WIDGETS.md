# Widgets

Waymark provides a real Android App Widget built with Jetpack Glance. It is not
a web view, shortcut or in-app simulation.

## Available widget

| Widget | Default size | Behavior |
|---|---|---|
| Waymark Watchlist | 2×2 (resizable) | Shows countries from your watchlist with current advisory levels and sync status |

The widget is resizable. Because Glance uses `SizeMode.Responsive`, content
adapts to the launcher-provided size.

## Configuration

- The widget is added immediately and always shows the countries in your current
  watchlist, in watchlist order. Manage which countries appear by editing your
  watchlist in the app.
- Tap the settings (⚙) button in the widget header to open widget settings.
- Available options:
  - **Corner radius** — adjust the widget corner rounding.
  - **Transparent background** — off by default.
  - **Visible elements** — show or hide the flag, country name, advisory level,
    and advisory date.

Every widget instance stores its own configuration keyed by App Widget ID.
Configuration survives process restarts and device reboots because it lives in
the local Room database.

## Behavior

- Widgets read from the local database; the refresh (↻) action triggers a
  synchronization so new data can be fetched and the status updates.
- The header shows sync status:
  - `Updating.` / `Updating..` / `Updating...` while a sync is running.
  - `Updated HH:mm` (or date and time) after a successful sync.
  - `Error, last update at …` when the last attempt failed or was interrupted.
- Tapping a country opens its dashboard in the app.
- With an empty watchlist the widget shows a clear empty state instead of a
  fabricated advisory.
- Deleting a widget removes its stored configuration.

## Appearance

- When **Dynamic color** is enabled in app settings (default), the widget
  follows Android system colors on supported devices (Android 12+).
- Country flags are rendered as emoji derived from ISO country codes.

## Data shown

- Country flag (optional).
- Country name (optional).
- Advisory level with official wording, or `Unknown` when unavailable (optional).
- Advisory publication date when available (optional).

The widget never invents an advisory level; unknown data is shown as unknown.

## Limitations

Android's widget APIs constrain what is possible. Waymark does not promise
arbitrary layouts or interactions:

- Glance/RemoteViews supports a limited set of views; rich scrolling and
  free-form positioning are constrained by the platform.
- Exact dimensions are controlled by the launcher; `SizeMode.Responsive`
  provides breakpoints rather than pixel-perfect layouts.
- Widget updates are subject to Android scheduling. There is no guaranteed
  refresh time, and continuous animations are not available in App Widgets (the
  updating indicator is refreshed periodically while a sync is in progress).
- Flags are rendered as emoji based on ISO country codes. Appearance may vary
  slightly by device and system font.

## Testing

Instrumented tests verify that widget configurations are independent per widget
ID and survive storage round-trips. Launcher rendering should be checked
manually on at least one Android 8–11 device and one Android 12+ device.
