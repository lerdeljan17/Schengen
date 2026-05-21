# Schengen Tracker (Android)

Android app for tracking Schengen short-stay limits under the 90/180 rule, redesigned with a bottom-nav workflow and Google Drive AppData sync.

Latest release: https://github.com/lerdeljan17/Schengen/releases/latest

## App layout

The app uses four bottom-nav destinations:

- **Home** – live 90/180 dashboard: big "days available now" hero, next-recovery date, next-trip summary, target-date checker, and an overstay warning if any planned trip exceeds 90 days.
- **Trips** – unified Upcoming / Past list of all trips with country-flag avatars, entry/exit dates, duration, a status badge (Within limits / Close to limit / Over limit), and tap-to-edit.
- **Calendar** – continuous multi-month scrolling calendar (six months back, eighteen months forward) with rounded pill highlights spanning each trip and a per-day "days available" subnumber under the date.
- **Settings** – Preferences, Appearance, Data, Sync, and Support sections.

## Implemented features

- Unified `Trip` model
  - Past, ongoing, and upcoming trips are stored in a single `trips` table and classified by date.
  - Each trip can have an optional exit date (open / ongoing trip), source (MANUAL / AUTO), free-form note, and a list of visited countries.
  - Migrating from earlier versions (with separate `stays` / `planned_trips` tables) is automatic on first launch, with a one-time CSV backup of pre-migration data written to `<filesDir>/backups/pre-v5-<timestamp>.csv`.
- Live 90/180 metrics
  - Days used in the current 180-day window and days available now.
  - Projection that includes future trips.
  - First next date that more days become available.
  - First date that planned trips would exceed the 90-day cap (overstay warning).
- Target-date availability checker
  - Pick any date and see available days (confirmed-only and including future trips).
- Continuous calendar
  - Six months back, eighteen months forward.
  - Multi-day trips rendered as connected rounded pills, wrapping across week boundaries.
  - Per-day subnumber shows "days available" on that date.
  - Tap any trip day to edit the underlying trip.
  - Week start configurable (Monday or Sunday).
- Trip management
  - Add / edit / delete trips with entry date, optional exit date, source, countries, and note.
  - Country picker shows ISO flag emojis next to country names.
  - Status badge on each card based on usage at the trip's exit date (Within limits / Close to limit / Over limit).
- Multi-passport profiles
  - Manage multiple traveler profiles in Settings.
  - Trips are profile-scoped.
- Local backup / restore
  - "Backup trips" exports profiles + trips to a CSV file via the system file picker.
  - "Restore trips" imports a CSV backup back into the app.
  - "Delete all trips" removes trips for the active profile or all profiles.
- Google Drive AppData sync
  - Sign in with Google (Drive AppData scope) and back up trips + theme prefs as a JSON snapshot to the app-scoped Drive folder.
  - Manual sync now + restore-from-Drive.
  - Last-sync timestamp shown in Settings.
- Theming
  - Appearance mode: System / Light / Dark.
  - Eight color themes: Blue (default), Green, Purple, Orange, Pink, Teal, Slate, Red.
  - Theme choice persists and applies instantly.
- Overstay risk notifications
  - Daily background check; notifications at 30 / 15 / 7 / 1 days remaining.
  - Toggleable from Settings.
- Automatic location tracking
  - Toggleable in Settings + "Check now" button.
  - Geofence-exit and movement update triggers + periodic checks via WorkManager.
  - Country detection via platform geocoder with Nominatim fallback.
  - Auto inserts AUTO-source trips when entering / leaving Schengen.

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

1. Ensure JDK 17 is installed (the project sources/targets `JavaVersion.VERSION_17`).
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Run on device/emulator with Google Play Services for location + Drive sync support.

## Google Drive sync setup

The "Sync" section in Settings uses Google Sign-In with the Drive `appDataFolder` scope. To enable it in your own build:

1. Create an Android OAuth 2.0 client in the [Google Cloud console](https://console.cloud.google.com/apis/credentials) with package `com.schengen.tracker` and your debug + release SHA-1.
2. Enable the **Google Drive API** in the same Cloud project.
3. Add the app's package name to the OAuth consent screen and the Drive AppData scope (`https://www.googleapis.com/auth/drive.appdata`).
4. Install the app on a device with Play Services; sign-in will request only the `appDataFolder` scope (a hidden per-app folder; the user's other Drive files are never accessible).

The backup payload is a single JSON file named `schengen-backup.json` stored in the app's Drive AppData folder. It contains:

- App theme preferences (appearance mode, color theme, start-week-on-Sunday).
- All passport profiles and their trips.

Restoring replaces local data with the snapshot from Drive.

## Release signing

GitHub releases are built as signed release APKs. Configure these repository secrets before relying on automated releases:

- `ANDROID_RELEASE_KEYSTORE_BASE64`: base64-encoded `.jks` or `.keystore` file
- `ANDROID_RELEASE_KEYSTORE_PASSWORD`
- `ANDROID_RELEASE_KEY_ALIAS`
- `ANDROID_RELEASE_KEY_PASSWORD`

Android can only update an installed app when the new APK has the same `applicationId`, is signed with the same key, and has a non-lower `versionCode`. If a device already has a debug-signed or differently signed build installed, uninstall that build once before installing the signed release APK.

## Permissions requested

- `INTERNET` (Nominatim fallback and Google Drive API calls)
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `POST_NOTIFICATIONS`

## Current limitations

- Drive sync overwrites the entire snapshot on backup and replaces local data on restore; there is no per-trip merge.
- Geofence + location-update behavior depends on OS/device power and location policies.
- If a trip remains open (no exit date) and is in the past, forecasts assume you continue staying in Schengen.
