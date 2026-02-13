# Zero-Sentinel

Zero-Sentinel is a "Zero-Knowledge" Android parental control ecosystem designed for absolute privacy and granular monitoring. Unlike commercial solutions that rely on cloud storage and invasive data practices, Zero-Sentinel operates as a self-contained, side-loaded agent.

It leverages Android's **NotificationListenerService** to capture telemetry (messages, alerts) and stores them locally before secure transmission. The application is designed to be **Serverless**, utilizing the Telegram Bot API as a resilient Command & Control (C2) infrastructure.

## Technical Documentation

### Architecture
The application is built as a **Headless Android Application** with a focus on stealth and persistence.

### Implementation Status
- [x] **Phase 1: Core Telemetry** (Service, Local DB).
- [x] **Phase 2: Persistence & Stealth** (Hidden icon - SIM Menu, Boot Receiver).
- [x] **Phase 3: C2 Infrastructure** (Telegram Bot, Command Processor).
- [x] **Phase 4: Stealth & Privacy** (WorkManager Polling, Secure Deletion, App Hiding).

### Core Components
*   **Telemetry Engine**: `SentinelNotificationListener` capturing incoming messages and alerts independently of UI.
*   **C2 Interface**: `C2Worker` (WorkManager) operating on a 15-minute periodic cycle for silent uploads and command polling.
*   **Stealth Engine**: `StealthManager` for programmatic icon hiding/showing and `SecureDelete` for forensic-grade log cleanup.
*   **Security Core**: Encrypted Preferences for credentials (AES-256).

## Key Features
1.  **True Stealth**: No persistent notification. App icon can be hidden remotely via Telegram command `/hide`.
2.  **Resilience**: Uses `WorkManager` for guaranteed background execution even on restricted battery modes.
3.  **Data Hygiene**: Logs are securely wiped (overwritten with zeros) immediately after successful upload.
4.  **Serverless**: Uses Telegram as the C2 infrastructure, removing the need for VPS maintenance.

## Commands
Control the agent via your Telegram Bot:
*   `/ping`: Check status. Returns next scheduled wake-up time.
*   `/wipe`: Force delete all local logs and database records.
*   `/hide`: Hide the application icon from the launcher.
*   `/show`: Restore the application icon.

## Installation (Side-Loading)
This application is **NOT** available on the Play Store due to its use of high-privilege APIs.
1.  Download the latest APK from Releases.
2.  Install and follow the "Onboarding Wizard" to grant permissions.
    *   **Notification Access**: Required for telemetry.
    *   **Ignore Battery Opts**: Required for background persistence.
3.  Enter your Telegram Bot Token and Chat ID.
4.  (Optional) Send `/hide` to vanish from the launcher.

## Build Instructions
1.  **Prerequisites**: JDK 17, Android SDK API 34.
2.  **Secrets**: Create `local.properties` with `TELEGRAM_BOT_TOKEN`.
3.  **Build**: `./gradlew assembleRelease`
