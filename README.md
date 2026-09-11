# Jarvis QA Android

A standalone Android voice assistant for personal productivity and software QA work.

## Features

- Voice input through Android speech recognition
- Human-like Android text-to-speech output
- Personal assistant and QA Expert modes
- Offline core commands—no computer connection required after installation
- Responsive Jetpack Compose interface
- GitHub Actions APK build

## Install the APK

1. Open the repository's **Actions** tab.
2. Select the latest successful **Build Android APK** run.
3. Download `jarvis-qa-debug-apk` from **Artifacts**.
4. Extract the ZIP and install `app-debug.apk` on Android.
5. If prompted, allow installation from your browser/files app, then grant microphone access.

## Local build

Requires JDK 17 and Android SDK 35.

```bash
gradle assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

Core responses are processed inside the app. Android's speech recognition and text-to-speech engines may use their configured online services. No API key is included in this repository.
