# HMX Shield

Privacy-first Android App Lock. Lock any app with a PIN, password, pattern, or
biometric; optional intruder selfie; on-device encrypted vault. No account, no
internet, no analytics.

## Architecture

- Single Gradle module `:app`, package-by-feature.
- Lock overlay = `AccessibilityService` launches a full-screen `LockActivity`
  (Activity overlay, **not** `SYSTEM_ALERT_WINDOW`). See `PERMISSION_AUDIT.md`.
- Secrets stored in `EncryptedSharedPreferences` + Android Keystore
  (HMAC-SHA256 credential digest; vault AES-GCM). No plaintext credentials.
- Crash reporting is fully on-device and defensive (see `crash/`).

## Building

Builds run via **GitHub Actions** (the committed workflows install Gradle
8.9, JDK 17, and the Android SDK — no wrapper is committed because the local
environment uses an incompatible JDK).

- Push to `main` → `Debug Build` + `Unit Tests` + `Lint` run automatically.
- Tag `v*` or manual dispatch → `Release Build` (requires a `KEYSTORE_BASE64`
  repo secret; otherwise it is skipped).

```bash
# Locally (requires JDK 17 + Android SDK 35):
gradle assembleDebug
```

## Module map

```
core/            Constants, ThemeController, security (Keystore/Crypto/Credential/Session)
data/            Room database, repositories
features/        authentication, applock, dashboard, vault, intruder,
                 securitycenter, permissions, themes, settings, stealth, onboarding
system/          accessibility service, boot receiver
crash/           on-device crash capture + safe launch
ui/              theme, components, navigation
```

## Tests

JVM unit tests cover pure logic (session/relock, cache, crash formatting).
Run with `gradle testDebugUnitTest`.
