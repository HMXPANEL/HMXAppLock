# Permission Audit — HMX Shield

## Declared permissions (AndroidManifest.xml)

| Permission | Type | Required? | Why |
|---|---|---|---|
| `RECEIVE_BOOT_COMPLETED` | manifest | Yes | Reload protection state after reboot. |
| `USE_BIOMETRIC` | manifest | Optional | Fingerprint unlock convenience. |
| `POST_NOTIFICATIONS` | runtime | Optional | Security / intruder alerts. |
| `CAMERA` | runtime | Optional | Intruder selfie capture. |
| `PACKAGE_USAGE_STATS` | special (Settings) | Fallback | Secondary foreground detection. |
| `QUERY_ALL_PACKAGES` | — | **NOT used** | Replaced by `<queries>` for MAIN/LAUNCHER. |

## Key decision: no `SYSTEM_ALERT_WINDOW`

The lock screen is implemented as a **full-screen `Activity` launched by an
AccessibilityService** (`AppLockAccessibilityService` → `LockActivity`, task
affinity `.lock`, `excludeFromRecents`, `showOnLockScreen`). This appears above
the protected app without needing the `SYSTEM_ALERT_WINDOW` (draw-overlays)
permission, which is heavily restricted, frequently auto-revoked by OEMs, and a
common Play Store rejection trigger.

Consequences:
- `SYSTEM_ALERT_WINDOW` is intentionally **omitted** from the manifest.
- Detection relies on Accessibility window events (primary) with UsageStats as a
  documented secondary fallback.
- The lock `Activity` uses `FLAG_SECURE` to prevent screenshots of the PIN entry.

## Special access (granted in Settings, not runtime)

- **Accessibility Service** — required for foreground detection. Guided from the
  Permission Center / onboarding to `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- **Usage Access** — `Settings.ACTION_USAGE_ACCESS_SETTINGS`. Only used if
  Accessibility is off.
- **Battery optimization** — `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  so the protection process is not killed.
- **Notifications / Camera** — normal runtime requests via `ActivityResultContracts`.

## OEM notes

Xiaomi / OPPO / vivo / Huawei / Samsung may silently revoke or ignore these
settings. The Permission Center shows device-specific guidance (see
`core/util/OemInfo.kt`) and a re-check button. There is no silent system
modification — we only open the relevant Settings screens.
