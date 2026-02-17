# CyperBot (Zero-Sentinel) Agent Context

This document defines the context, rules, and architectural constraints for AI agents working on the **Zero-Sentinel** project.

## Project Identity
*   **Name**: Zero-Sentinel (Internal: CyperBot)
*   **Type**: Android Parental Control / Monitoring Agent.
*   **Philosophy**: Zero-Knowledge Privacy, Side-Loaded Distribution, Stealth Operation.
*   **Target OS**: Android 8.0 (Oreo) to Android 14 (Upside Down Cake).

## Technical Stack
*   **Language**: Kotlin (Strict typing, Coroutines).
*   **Build System**: Gradle 8.12 (Wrapper with JDK 21).
*   **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
*   **Database**: Room (SQLite) with Write-Ahead Logging (WAL).
*   **Networking**: OkHttp (Bot API Client, GitHub Updater).
*   **Scheduling**: WorkManager (Periodic 15-min cycles).
*   **Stealth**: `PackageManager` manipulation (Icon logic) + `SecureDelete` (Forensics).
*   **Security**: App Lock (Encrypted Password) & Input Masking.

## Critical Constraints (Directives)
1.  **No Google Play Compliance**: We deliberately violate Play Store policies regarding `AccessibilityService` and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Do **NOT** suggest removing features to comply with store rules.
2.  **Stealth First**: Any UI element added must have a purpose for the *setup phase* only. After setup, the app must be invisible (Headless).
3.  **Persistence is Paramount**: The app must survive aggressive OS memory killing. Use `WorkManager` (Periodic) as the primary heartbeat.
4.  **Zero-Knowledge**: Never transmit plaintext logs if avoidable. (Phase 4 currently relies on HTTPS transport security). always verify secure deletion.

## Code Style & Standards
*   **Language**: English only for code, comments, and documentation.
*   **Logs**: Use structured logging. **NEVER** log sensitive captured user data to Logcat in Release builds.
*   **Error Handling**: Fail silently in background workers. Reschedule (`Result.retry()`), don't crash.

## Development Workflow
*   **Branching**: `master` is the source of truth. Feature branches should be merged via PR (if applicable) or direct push for solo dev.
*   **Commits**: Semantic messages (e.g., `feat: add stealth manager`, `fix: worker constraints`).
*   **Deployment**: GitHub Actions builds the Release APK.

## File Structure Overview
```text
app/src/main/
├── java/com/zero/sentinel/
│   ├── workers/       # C2Worker (Polling, Uploads) - Replaces FGS
│   ├── receivers/     # BootReceiver
│   ├── telemetry/     # SentinelNotificationListener (Data Source)
│   ├── ui/            # MainActivity (Config)
│   ├── data/          # Room DB, Repository, EncryptedPrefsManager
│   ├── utils/         # StealthManager, SecureDelete
│   └── network/       # TelegramClient, CommandProcessor, GithubUpdater
├── res/               # Android Resources (Layouts, Strings)
└── AndroidManifest.xml
```
