# Schengen Tracker (Android)

Android app for tracking Schengen short-stay limits under the 90/180 rule.

## Implemented features

- Manual stay management
  - Add entry date
  - Add exit date (closes latest open stay)
  - Delete recorded stays
- Calendar visualization
  - Monthly calendar with confirmed stay days highlighted
  - Planned trip days highlighted separately
  - Prev/next month navigation
- Live rule metrics
  - Days used in the rolling 180-day window
  - Days available now (out of 90)
  - First next date when more days become available
  - First planned date that would exceed the 90/180 rule
- Planned trips simulation
  - Add/delete planned entry/exit ranges before booking
  - Simulation is included in overstay forecasting
- Multi-passport profiles
  - Create multiple traveler/passport profiles
  - Switch active profile instantly
  - All stays/planned trips are profile-scoped
- CSV import/export (local)
  - Export all local profiles, stays, and planned trips into one CSV
  - Import CSV back into local storage (no cloud dependency)
- Overstay risk notifications
  - Local notifications when available days hit thresholds (30, 15, 7, 1)
  - Includes planned-trip warning context if simulations exceed limits
- Automatic tracking scaffold
  - Optional background location checks (WorkManager)
  - Reverse geocoding to detect country
  - Auto entry/exit when Schengen in/out state changes

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

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `POST_NOTIFICATIONS`

## Current limitations

- Local-only storage by design (no cloud sync/backups).
- Automatic detection uses periodic checks (every 6 hours), not geofence edge events.
- If a stay remains open, forecasts assume you continue staying in Schengen.
