# Privacy policy

Waymark is an independent, local-first Android application. This document
describes the actual behavior of the app in this repository.

## Summary

- No account is required.
- No user data is sent to the project owner or any third party.
- There is no Waymark backend, cloud sync, analytics, telemetry or advertising
  SDK.
- The only network request retrieves public travel advisory data from the
  official U.S. Department of State ArcGIS service.

## Data stored on the device

Waymark stores the following information locally in a Room database and in
DataStore preferences:

- Downloaded country catalogue and advisory records.
- Your watchlist, ordering, favorites and per-country notification switches.
- Locally recorded advisory history.
- App preferences (theme, dynamic color, automatic updates, interval,
  notification opt-in).
- Widget configurations, one per widget instance.

This data stays on the device unless you explicitly export a backup. Backups
are written to a location you choose through the Android system file picker.

## Network communication

The app contacts one host:

```
https://services6.arcgis.com/…/FeatureServer/54/query
```

The request contains only the query needed to retrieve public advisory data
(pagination, field list, JSON format). It does not include your watchlist,
identifiers, device information or usage data.

No other network requests are made by the app. The website is a separate static
site deployed on GitHub Pages.

## Backups

- Export writes a JSON or ZIP file containing the data listed above.
- Import validates the file before changing the database.
- Import defaults to merging; replacing existing data requires an explicit
  toggle.
- You control where backups are stored. The app does not upload them.

## Notifications

Notifications are optional and require explicit consent. On Android 13+ the
system notification permission is requested only when you enable them. Notices
are generated locally from data already on the device. The app never contacts a
push service.

Android's standard notification sound is used.

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Retrieve public advisory data |
| `ACCESS_NETWORK_STATE` | Detect offline state before syncing |
| `POST_NOTIFICATIONS` | Optional advisory change notifications |
| `RECEIVE_BOOT_COMPLETED` | WorkManager reschedules periodic work after reboot |

## Data removal

- Removing a country deletes its watchlist entry; cached advisory records for
  that country may remain until the next synchronization refreshes the
  catalogue.
- Clearing history permanently deletes recorded events.
- Uninstalling the app removes all local data.
- The app does not collect data that would need server-side deletion.

## Children

Waymark is a general-audience travel information app and does not knowingly
collect personal information from anyone.

## Changes

Material changes to this policy will be documented in `CHANGELOG.md` and in the
repository history.

## Contact

Open an issue at https://github.com/KusokMedi/waymark.

## Independence

Waymark is not affiliated with, endorsed by, or operated by the U.S. Department
of State. U.S. Travel Advisories are written for U.S. citizens, nationals and
legal residents and are not a universal safety rating for every traveler.
