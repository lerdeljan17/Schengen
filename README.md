# Schengen Tracker (Android)

Android app for tracking Schengen short-stay limits under the 90/180 rule.

Latest release: https://github.com/lerdeljan17/Schengen/releases/latest

## Implemented features

- Tabbed workflow
  - `Main`: live 90/180 metrics, target-date checker, calendar insights
  - `History`: manual stays + automatic location detection controls
  - `Planned`: planned trip simulation and management
  - `Tools`: CSV import/export and passport profile management
- Live 90/180 metrics and simulation
  - Days used now and days available now
  - Planned-trip impact for today (projected used/available + delta)
  - Simulated impact at the latest planned exit date
  - First next date when more days become available
  - First planned date that would exceed the 90/180 rule
- Target-date availability checker
  - Pick any date and view available days
  - Compare confirmed-only vs confirmed+planned availability
- Calendar visualization and unlock-day analytics
  - Monthly calendar with confirmed stay days (green)
  - Planned trip days highlighted separately (gold)
  - "Unlock" days that increase availability (blue)
  - Prev/Today/Next month navigation
  - Monthly unlock summary + contiguous date-range breakdown
- Manual and automatic stay history
  - Add manual entry (with optional note)
  - Add manual exit (closes latest open stay)
  - Edit/delete existing stays (entry, exit, source, note)
- Planned trip management
  - Add/edit/delete planned entry/exit ranges with optional notes
  - Convert a planned trip into a confirmed stay (confirm as stay)
- Multi-passport profiles
  - Create/select/edit/delete traveler/passport profiles
  - Active profile switching
  - Stays and planned trips are profile-scoped
- CSV import/export (local)
  - Export all local profiles, stays, and planned trips into one CSV
  - Import CSV back into local storage
- Overstay risk notifications
  - Local notifications when available days hit thresholds (30, 15, 7, 1)
  - Includes planned-trip warning context if simulations exceed limits
- Automatic location tracking
  - Toggleable tracking state persisted in app preferences
  - Dynamic movement anchor geofence (~5 km radius)
  - Geofence-exit and movement update triggers for immediate checks
  - Periodic fallback checks via WorkManager (every 6 hours)
  - Manual "Check now" button for immediate validation/testing
  - Country detection via platform geocoder with Nominatim fallback
  - Auto entry/exit recording when Schengen in/out state changes

## Rule model used

- Entry date counts as a day in Schengen.
- Exit date counts as a day in Schengen.
- For any date `D`, used days = days physically present within `[D-179, D]`.
- Available days = `90 - usedDays` (clamped to `0..90`).

## Important legal note

This app is a planning aid and not legal advice. Border authorities make the final determination.

Reference pages used while implementing:

- European Commission short-stay calculator page:
  - https://home-affairs.ec.europa.eu/policies/schengen/border-crossing/short-stay-calculator_en
- EU visa policy overview (90/180 definition):
  - https://home-affairs.ec.europa.eu/policies/schengen-borders-and-visa/visa-policy_en

## Open in Android Studio

1. Ensure JDK 17 is installed.
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Run on device/emulator with Google Play Services for location support.

## Permissions requested

- `INTERNET` (used for reverse-geocoding fallback requests)
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `POST_NOTIFICATIONS`

## Current limitations

- Local-only storage by design (no cloud sync/backups).
- Import currently appends data and does not deduplicate identical rows.
- Geofence + location-update behavior depends on OS/device power and location policies.
- If a stay remains open, forecasts assume you continue staying in Schengen.
