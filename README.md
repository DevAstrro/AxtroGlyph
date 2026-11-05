# Axtro Glyph

A lightweight, privacy-first Android prototype for a live battery/charging meter, initially targeting Nothing Phone (2a).

> **Current status:** The app reads Android battery state and displays it in the app. It does **not** yet control the rear Glyph LEDs. The official Nothing Glyph SDK/API, supported device permissions, and screen-off behaviour must be verified on a real Phone (2a) before claiming hardware support.

## Features
- Live battery percentage via Android's sticky `ACTION_BATTERY_CHANGED` broadcast (no polling loop).
- Charging/full-state detection.
- Minimal dark Jetpack Compose UI.
- No ads, analytics, network permission, database, or background polling.
- GitHub Actions builds a debug APK on pushes, pull requests, and manual runs.

## Get the APK
1. Open the repository's **Actions** tab.
2. Select the latest **Android APK** workflow run.
3. Download the `axtro-glyph-debug-apk` artifact.
4. Extract it and install `app-debug.apk` on your test phone.

This is a debug APK, not a Play Store release.

## Build locally
Requirements: JDK 17, Android SDK platform 35, Gradle 8.11.1.

```bash
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Glyph hardware roadmap
1. Obtain the official Nothing Glyph Developer Kit compatible with Phone (2a).
2. Add the exact SDK artifact and permission/key configuration from Nothing's current documentation.
3. Implement its supported progress API and map battery percentage to Phone (2a) LED zones.
4. Test foreground operation on-device first.
5. Separately test screen-off/background operation. A foreground service must not be assumed to bypass SDK foreground-app restrictions.
6. Add a clear enable/disable setting and rate-limit updates.

## Lightweight design
- No periodic timer or polling.
- Battery state updates are event-driven.
- No internet permission or background service in this prototype.
- No analytics, crash reporting, ads, or third-party runtime SDKs.
- Compose state changes only when battery values change.

## Project
- Package: `space.axtro.glyph`
- Minimum Android: API 34
- Target Android: API 35
- Kotlin + Jetpack Compose + Material 3

MIT License. See [LICENSE](LICENSE).
