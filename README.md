# Jarvis QA Android

A standalone Android voice assistant with a free, on-device Qwen LLM for personal productivity and software QA work.

## Features

- Voice input through Android speech recognition
- Human-like Android text-to-speech output
- Personal assistant and QA Expert modes
- Qwen3 1.7B Q4 local LLM through llama.cpp—no API key or computer required
- Resumable first-run model download (~1.1 GB), then fully offline AI chat
- Offline core commands remain available before the model is downloaded
- Responsive Jetpack Compose interface
- GitHub Actions APK build

## Install the APK

1. Open the repository's **Actions** tab.
2. Select the latest successful **Build Android APK** run.
3. Download `jarvis-qa-debug-apk` from **Artifacts**.
4. Extract the ZIP and install `app-debug.apk` on Android.
5. If prompted, allow installation from your browser/files app, then grant microphone access.
6. Tap **Enable offline Qwen AI** once while connected to Wi-Fi. Keep the app open until the model loads.

## Local build

Requires JDK 17 and Android SDK 35.

```bash
gradle assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy

LLM responses are generated locally on the phone using Qwen3 1.7B and llama.cpp. Android's speech recognition and text-to-speech engines may use their configured online services. No prompt, API key, or paid AI service is required.
