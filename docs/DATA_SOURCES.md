# Data sources

## Primary source

- Official reference:
  https://travel.state.gov/en/international-travel/travel-advisories.html
- Structured source: ArcGIS FeatureServer "Travel Advisory Levels (NEW)",
  layer `54`
- Service URL:
  https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer

The Android app queries layer `54` directly:

```
…/FeatureServer/54/query
  ?where=NAME IS NOT NULL
  &outFields=*
  &returnGeometry=false
  &resultOffset=<n>
  &resultRecordCount=<page size>
  &orderByFields=OBJECTID
  &f=json
```

Pagination continues while the response reports
`exceededTransferLimit = true` and returns a non-empty page, bounded to 50
pages. `If-None-Match` and `If-Modified-Since` are sent when the previous
response supplied validators; `304 Not Modified` is treated as a successful
sync with no data rewrite.

This is not a documented permanent API contract. If the service changes or
disappears, the app keeps cached data and shows the failure honestly.

## Verified fields

| Field | Meaning |
|---|---|
| `OBJECTID` | Stable source row identifier |
| `GlobalID` | Preferred stable record identifier |
| `NAME` | Country or region name |
| `ISO_3` | ISO 3166-1 alpha-3 country code |
| `LEVEL_` | Raw advisory code (see below) |
| `URL` | Official advisory URL |
| `ADVDATE` | Advisory publication timestamp (ms, UTC) |
| `EditDate` | Source row edit timestamp (ms, UTC) |
| `AD_OD_Status` | Departure/ordered-departure status |
| `AD_OD_Text` | Departure notice text |
| `AD_OD_Date` | Departure notice date (date string) |

## Advisory codes

| Raw code | Level | Meaning | Higher-risk areas |
|---:|---:|---|---|
| 1 | 1 | Exercise Normal Precautions | no |
| 10 | 1 | Exercise Normal Precautions | yes |
| 2 | 2 | Exercise Increased Caution | no |
| 20 | 2 | Exercise Increased Caution | yes |
| 3 | 3 | Reconsider Travel | no |
| 30 | 3 | Reconsider Travel | yes |
| 4 | 4 | Do Not Travel | no |

The mapping is explicit and lives in
`domain/AdvisoryLevels.kt`. Raw codes `10`, `20` and `30` are not arithmetic
variants of the base level and are never sorted as ordinary numbers.

Unknown or missing codes produce `level = null` and are displayed as
`Unknown`. They are never converted to Level 1, never counted as severity 1 and
never generate fabricated change events.

## Dates

`ADVDATE` and `EditDate` are Unix timestamps in milliseconds (UTC) and are
converted to UTC calendar dates so that time zones cannot shift the published
day. `AD_OD_Date` is already a date string and is stored trimmed.

The locally observed synchronization time is stored separately in
`sync_state.lastSuccessAt` and is never presented as the advisory date.

## Regional records

Some ISO codes contain several records (for example Mexico with its states, or
French overseas territories). Records that share an ISO code are grouped. The
record whose name matches the ISO catalogue name becomes the country record;
the remaining records are treated as regional records of that same country.
Grouping never invents geographic relationships.

If no country record can be identified and the grouped records disagree on the
level, the aggregate level remains unknown.

## Missing or failed data

- HTTP errors, malformed JSON and empty usable payloads are treated as sync
  failures.
- Previously stored advisories are preserved on failure.
- If nothing has ever been downloaded, the UI shows `Unknown` and the
  destination list is whatever is cached; no placeholder advisories are
  invented.
- A response with no usable records is rejected rather than replacing good
  local data with an empty catalogue.

## Source update cadence

The U.S. Department of State publishes advisories as they change. Waymark
requests updates every 12 hours by default (configurable 6/12/24 hours) and on
app start or manual refresh. Android may delay WorkManager jobs; Waymark does
not promise exact refresh times.

## Legacy snapshot

`data/latest.json` and `app/public/data/latest.json` are snapshots produced by
the earlier web dashboard. The Android app does not read them.
