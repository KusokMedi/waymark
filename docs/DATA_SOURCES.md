# Data sources

## Primary source

- Official reference: https://travel.state.gov/en/international-travel/travel-advisories.html
- Structured source: ArcGIS FeatureServer "Travel Advisory Levels (NEW)", layer 54
- Service URL: https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer/54

## Verified fields

- `OBJECTID`, `NAME` (country or state/region name), `LEVEL_`, `URL`, `ADVDATE`, `ISO_3`, `GlobalID`, `EditDate`, `AD_OD_Status`, `AD_OD_Text`, `AD_OD_Date`.

## Advisory codes

| Code | Level | Higher-risk areas |
|---:|---:|---|
| 1 | 1 | no |
| 10 | 1 | yes |
| 2 | 2 | no |
| 20 | 2 | yes |
| 3 | 3 | no |
| 30 | 3 | yes |
| 4 | 4 | no |

Unknown codes are preserved with `level = null` and are never converted to Level 1.

## Dates

`ADVDATE` and `EditDate` are Unix timestamps in milliseconds (UTC). They are converted to UTC calendar dates to avoid day shifts.

`AD_OD_Date` is a date-only string.

## Regional records

Some ISO codes contain multiple records (e.g. Mexico and its states, French overseas territories). These are grouped by ISO code and shown as regional records. No country-to-region relationship is fabricated.

## CORS

The ArcGIS endpoint returns `Access-Control-Allow-Origin: *`, so direct browser access is possible. The app still prefers the same-origin snapshot and falls back to direct fetch when needed.

## Update frequency

Scheduled twice daily (06:00 and 18:00 UTC). Manual refresh attempts a direct source fetch.
EOF