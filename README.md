# JARVIS QA Android

Native Android app for Gopal's QA and personal assistant. It connects directly to Ollama/Qwen on a Windows computer over the same trusted Wi-Fi. It includes Android speech recognition and text-to-speech.

## Build

1. Open this folder in Android Studio.
2. Allow Gradle sync to finish.
3. Select Build > Build APK(s).
4. Install app/build/outputs/apk/debug/app-debug.apk.

## Automatic GitHub build

Push this project to GitHub. Open Actions, select Build JARVIS APK, and choose Run workflow. Download the JARVIS-QA-debug artifact after the build succeeds.

## Connect Ollama

Find the computer's Wi-Fi IPv4 address with ipconfig.

Run in Windows PowerShell:

    setx OLLAMA_HOST "0.0.0.0:11434"

Restart Ollama. Allow TCP port 11434 through Windows Firewall on the Private network only. Keep the phone and computer on the same trusted Wi-Fi.

In the app Settings, enter http://YOUR-PC-IP:11434. Never expose port 11434 to the public internet.
