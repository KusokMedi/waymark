# Widgets

Waymark provides real Android App Widgets built with Jetpack Glance. They are
not web views, shortcuts or in-app simulations.

## Available widgets

| Widget | Default size | Behavior |
|---|---|---|
| Waymark Watchlist | 2×2 (resizable) | Shows countries from your watchlist with current advisory levels and sync status |
| Waymark Country | 4×2 (resizable) | Shows the current advisory for one selected country |

Both widgets are resizable. Because Glance uses `SizeMode.Responsive`, content
adapts to the launcher-provided size.

## Configuration

- **Waymark Watchlist** has no configuration screen. It is added immediately and
  always shows the countries in your current watchlist, in watchlist order.
  Manage which countries appear by editing your watchlist in the app.
- **Waymark Country** opens a country picker when added. Choose one country to
  display. To change the country, remove and re-add the widget.

Every widget instance stores its own configuration keyed by App Widget ID.
Configuration survives process restarts and device reboots because it lives in
the local Room database.

## Behavior

- Widgets read from the local database; the refresh action triggers a
  synchronization so new data can be fetched and the status updates.
- The header shows sync status: `Updating…` while a sync is running, or the last
  successful update time when idle (for example `Updated 14:32`).
- Tapping a country opens its dashboard in the app.
- With an empty watchlist (or no country selected for the single-country
  widget) the widget shows a clear empty state instead of a fabricated advisory.
- Deleting a widget removes its stored configuration.

## Data shown

- Country flag (emoji, derived from the country code).
- Country name.
- Advisory level with official wording, or `Unknown` when unavailable.
- Advisory publication date when available.
- Regional advisory count on the single-country widget when present.

The widget never invents an advisory level; unknown data is shown as unknown.

## Limitations

Android's widget APIs constrain what is possible. Waymark does not promise
arbitrary layouts or interactions:

- Glance/RemoteViews supports a limited set of views; rich scrolling and
  free-form positioning are constrained by the platform.
- Exact dimensions are controlled by the launcher; `SizeMode.Responsive`
  provides breakpoints rather than pixel-perfect layouts.
- Widget updates are subject to Android scheduling. There is no guaranteed
  refresh time, and continuous animations are not available in App Widgets.
- Flags are rendered as emoji based on ISO country codes. Appearance may vary
  slightly by device and system font.

## Testing

Instrumented tests verify that widget configurations are independent per widget
ID and survive storage round-trips. Launcher rendering should be checked
manually on at least one Android 8–11 device and one Android 12+ device.
