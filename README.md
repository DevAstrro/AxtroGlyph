# Axtro Glyph

A lightweight Android prototype for live battery charging progress on Nothing Phone (2a).

## Current implementation
- Includes the official Nothing Glyph Developer Kit 2.0 AAR in `app/libs/`.
- Uses Android's event-driven battery-change broadcast (no polling loop).
- Initialises and registers the Nothing Glyph SDK for Phone (2a).
- Opens a Glyph session and maps charging percentage to the Phone (2a) C-series progress arc.
- Turns the Glyph off when the device is not charging.
- Minimal Jetpack Compose UI; no ads, analytics, network permission, or third-party runtime services.

## Important limitations
- **Hardware behaviour is not yet verified on a physical device.** The next test must confirm SDK registration, the C-series progress output, and LED brightness.
- Nothing's official SDK currently documents third-party Glyph control as foreground-app-only. This prototype updates while Axtro Glyph is open; it does not claim continuous screen-off operation. An Android foreground service does not automatically override that SDK restriction.
- The manifest currently uses the SDK's documented debug key `test`. For a production build, follow Nothing's current API-key guidance for the Android version/device being targeted.
- The app currently targets Phone (2a) only (model code A142 / SDK device constant `DEVICE_23111`).

## Build APK
Every push to `main`, pull request, or manual workflow dispatch runs GitHub Actions.

1. Open the **Actions** tab.
2. Select **Android APK** and open the latest run.
3. Download the `axtro-glyph-debug-apk` artifact.
4. Extract `app-debug.apk` and install it on the Phone (2a).

This is a debug APK, not a Play Store release.

## Local build
Requirements: JDK 17, Android SDK platform 35, Gradle 8.11.1.

```bash
gradle --no-daemon :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## SDK
The included AAR is from the official [Nothing Glyph Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit). See its README for API lifecycle, supported devices, and restrictions.

## Privacy and efficiency
- No internet permission.
- No analytics, ads, crash reporting, or account system.
- Battery changes are event-driven; no periodic polling.
- Glyph session is closed and SDK unbound when the activity is destroyed.

## Stack
Kotlin · Jetpack Compose · Material 3 · Nothing Glyph SDK 2.0 · Android API 34+
