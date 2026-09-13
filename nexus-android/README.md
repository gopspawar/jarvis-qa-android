# NEXUS Planner for Android

Prepared from the supplied index-2.html. This is an Android WebView application with native file export/import, voice input, speech playback, and a phone Clock alarm flow. It preserves the HTML interface; it is not a Kotlin/Compose rewrite.

## Included

- Dedicated Work Records: add/edit/delete, project, date, status, hours, notes, search and status filters.
- Work records persist locally and are included in JSON backups.
- Existing planner, tasks, habits, goals, fitness and other screens remain available.
- Bundled HTML works offline for local records. AI requires internet and the existing Puter service. Its sign-in/popup behavior has not been tested in Android WebView.
- The embedded Sarvam key has been removed. Native speech is available under App tools → Read latest JARVIS reply. Voice input opens the phone's speech recognizer and places the result in the chat composer for review.
- App tools → Set a phone alarm opens Clock. These alarms are managed in Clock and can ring with NEXUS closed. Existing HTML alarm entries remain foreground-only and do not automatically sync to Clock.

## Build an installable APK without a computer

1. Upload the contents of this directory, including .github/workflows/android.yml, to the root of a GitHub repository you own. The build runs on pushes to main, or manually from Actions → Build NEXUS APK → Run workflow.
2. Wait for the workflow to succeed.
3. Download the NEXUS-installable-APK artifact from that run, extract it, and open app-debug.apk on your phone. Allow installation from the app opening the APK when Android prompts.

This is a development-signed APK. Keep a stable signing key for later distributable releases; a fresh CI debug key may prevent an in-place update. Export a backup before uninstalling. GitHub Actions use is subject to the account's available quota.

## Local build

Requires JDK 17, Gradle 8.9, Android SDK platform 35 and internet for dependencies. Run `gradle :app:assembleDebug`. A Gradle wrapper JAR is not included. The included GitHub workflow installs Gradle.

## Data and remaining limits

Local data is private to this installation. Uninstalling or clearing app data removes it. Export backups using App tools. Browser data from the original website is not automatically transferred: export there and import in this app.

This project has not been compiled or tested on a device in the preparation environment: Android SDK/Gradle are unavailable and SDK download access is blocked. AI authentication, weather location, background behavior and native file/voice flows need device testing. WebView microphone and location permissions are not granted; use native Voice input, and weather may be unavailable. Existing demo analytics and sample records from the supplied HTML are retained, not verified personal history. Deleting all records in a collection no longer re-creates its demo data on restart.

## Source references

- https://developer.android.com/develop/ui/views/layout/webapps/load-local-content
- https://developer.android.com/reference/android/provider/AlarmClock
