# Zero-Sentinel

Zero-Sentinel is a "Zero-Knowledge" Android parental control ecosystem designed for absolute privacy and granular monitoring. Unlike commercial solutions that rely on cloud storage and invasive data practices, Zero-Sentinel operates as a self-contained, side-loaded agent.

It leverages Android's **NotificationListenerService** to capture telemetry (messages, alerts) and stores them locally before secure transmission. The application is designed to be **Serverless**, utilizing the Telegram Bot API as a resilient Command & Control (C2) infrastructure.

## Technical Documentation

### Architecture
The application is built as a **Headless Android Application** with a focus on stealth and persistence.



### Core Components
*   **Telemetry Engine**: `SentinelNotificationListener` capturing incoming messages and alerts independently of UI.
*   **Intelligence Engine**: Passive Monitoring (WiFi/Location) triggered by `C2Worker` cycles.
*   **C2 Interface**: `C2Worker` (WorkManager) operating on a 15-minute periodic cycle for silent uploads and command polling.
*   **Stealth Engine**: `SIMMenuActivity` (Decoy UI), `StealthManager` (Icon Hiding), and `SecureDelete` (Forensics).
*   **Security Core**: Encrypted Preferences (AES-256) and `SentinelDeviceAdminReceiver` (Uninstall Protection).

## Key Features
1.  **Stealth Decoy**: App disguised as "SIM Menu" with a fake UI. Access real app via secret PIN.
2.  **Passive Monitoring**: Silently logs WiFi SSID and GPS Location (from photos) every 15 minutes.
3.  **Resilience**: Uses `WorkManager` for guaranteed background execution and **Device Admin** to prevent uninstallation.
4.  **Data Hygiene**: Logs are securely wiped (overwritten with zeros) immediately after successful upload.
5.  **Serverless**: Uses Telegram as the C2 infrastructure, removing the need for VPS maintenance.
6.  **Secure Access**: Remote PIN management and local App Lock (AES-256) to prevent unauthorized access.

## Commands
Control the agent via your Telegram Bot:
*   `/ping`: Check status. Returns next scheduled wake-up time.
*   `/wipe`: Force delete all local logs and database records.
*   `/setpin <PIN>`: Remotely change the app access PIN.

## Installation (Side-Loading)
This application is **NOT** available on the Play Store due to its use of high-privilege APIs.
1.  Download the latest APK from Releases.
2.  Install and open **"SIM Menu"**.
3.  Click **"Help and Support"** and enter default PIN **`123123`**.
4.  Follow the "Onboarding Wizard" to grant permissions:
    *   **Notification Access**: Required for telemetry.
    *   **Ignore Battery Opts**: Required for background persistence.
    *   **Uninstall Protection**: Enable Device Admin to prevent removal.
5.  Enter your Telegram Bot Token and Chat ID.
6.  **Set App Password**: Change the default PIN immediately.

## Build Instructions
1.  **Prerequisites**: JDK 21, Android SDK API 34.
2.  **Secrets**: Create `local.properties` with `TELEGRAM_BOT_TOKEN`.
3.  **Build**: `./gradlew assembleRelease`

## DevOps & Deployment (GitHub Actions)

This repository uses GitHub Actions to automatically build and release signed APKs.

### 1. Generate Signing Keystore
You must generate a secure Keystore to sign the release APKs.
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias key0
```

### 2. Configure GitHub Secrets
Go to **Settings > Secrets and variables > Actions** in your repository and add the following secrets:

1.  `SIGNING_KEY_STORE_BASE64`: The Base64 encoded content of your `release.jks` file.
    *   **Linux/Mac**: `base64 -w 0 release.jks | pbcopy`
    *   **Windows (PowerShell)**: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard`
2.  `SIGNING_STORE_PASSWORD`: The password for the keystore.
3.  `SIGNING_KEY_ALIAS`: The alias of the key (e.g., `key0`).
4.  `SIGNING_KEY_PASSWORD`: The password for the key.

### 3. Trigger a Release
*   **Push to `master`**: Triggers a build check.
*   **Push a Tag (e.g., `v1.0.0`)**: Triggers a Release build and uploads the APK to GitHub Releases.
    ```bash
    git tag v1.0.0
    git push origin v1.0.0
    ```
