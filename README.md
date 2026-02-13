Zero-Sentinel is a "Zero-Knowledge" Android parental control ecosystem designed for absolute privacy and granular monitoring. Unlike commercial solutions that rely on cloud storage and invasive data practices, Zero-Sentinel operates as a self-contained, side-loaded agent.

It leverages Android's Accessibility Services to capture telemetry (keystrokes, screen content, app usage) and encrypts this data on-device using a hybrid RSA-4096 + AES-256 scheme. The encrypted logs are transmitted via the Telegram Bot API to the parent, ensuring that neither the transport provider nor any third party can access the sensitive data. The private key remains exclusively with the parent.

# Technical Documentation

## Architecture
The application is built as a **Headless Android Application** with a focus on stealth and persistence.

## Implementation Status
- [x] **Phase 1: Core Telemetry** (Service, Local DB).
- [x] **Phase 2: Persistence & Stealth** (Hidden icon - SIM Menu, FGS, Boot).
- [x] **Phase 3: C2 Infrastructure** (Telegram Bot, Scheduled Polling, Notification Listener).
- [ ] **Phase 4: Security & Encryption** (RSA/AES, Code Obfuscation).

### Core Components
*   **Telemetry Engine**: `NotificationListenerService` capturing incoming messages and alerts.
*   **Persistence Layer**: Foreground Service (`specialUse`) + Boot Receiver.
*   **C2 Interface**: Scheduled Polling (10 min) Client for Telegram Bot API.
*   **Security Core**: Encrypted Preferences for credentials (AES-256).

## Key Features
1.  **Zero-Knowledge Privacy**: Client-side encryption ensures only the parent holds the decryption key.
2.  **Stealth Mode**: Icon cloaking and disguised notifications (`<activity-alias>`).
3.  **Resilience**: Bypass "Doze Mode" and "Restricted Settings" via guided onboarding.
4.  **Serverless**: Uses Telegram as the C2 infrastructure, removing the need for VPS maintenance.

## Installation (Side-Loading)
This application is **NOT** available on the Play Store due to its use of high-privilege APIs.
1.  Download the latest APK from Releases.
2.  Install and follow the "Onboarding Wizard" to grant permissions.
3.  (Optional) Provision as Device Owner for anti-uninstall protection.

## Build Instructions
1.  **Prerequisites**: JDK 17, Android SDK API 34.
2.  **Secrets**: Create `local.properties` with `TELEGRAM_BOT_TOKEN` and `RSA_PUBLIC_KEY`.
3.  **Build**: `./gradlew assembleRelease`
