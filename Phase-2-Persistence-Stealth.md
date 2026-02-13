# Phase 2: Persistence & Stealth - Survival in a Hostile OS

## 1. Overview
This phase focuses on ensuring the application remains active and undetected. Android's modern resource management (Doze Mode, App Buckets) aggressively kills background processes. "Zero-Sentinel" must employ advanced persistence techniques and stealth mechanisms to survive without being noticed by the child.

## 2. Advanced Persistence Strategies

### 2.1 Foreground Service (Android 14+ Compliance)
To prevent the system from killing the app, it must run as a **Foreground Service (FGS)**. Android 14 requires strictly typed FGS.

*   **Type Selection**: Use `specialUse` for side-loaded apps that don't fit standard categories (media, navigation).
*   **Manifest Declaration**:
    ```xml
    <service
        android:name=".services.SentinelService"
        android:foregroundServiceType="specialUse"
        android:exported="false">
        <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                  android:value="parental_control_monitoring" />
    </service>
    ```
*   **Implementation Details**:
    *   Call `startForeground()` within 5 seconds of service start.
    *   Bind the service to a **Low Importance Notification** (see Section 3.2).

### 2.2 Battery Optimization Bypass (Doze Mode)
Standard "Battery Optimization" will suspend the app's network access and CPU usage.
*   **Check**: `PowerManager.isIgnoringBatteryOptimizations()`.
*   **Action**: If `false`, trigger `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
    *   *Warning*: This requires the permission `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. This is a guaranteed rejection on Play Store but essential for this project.

### 2.3 The "Device Owner" Option (Nuclear Persistence)
For maximum control, the app can be provisioned as a **Device Owner (DO)**. This prevents uninstallation and allows deep system control.
*   **Provisioning Command (ADB)**:
    `adb shell dpm set-device-owner com.zero.sentinel/.receivers.DeviceAdminReceiver`
*   **Capabilities**:
    *   `setUninstallBlocked()`: Make the app impossible to uninstall via UI.
    *   `setApplicationHidden()`: Completely hide the app from the launcher while keeping it running.
    *   **Prerequisite**: The device must have no active accounts (Google) during provisioning. Accounts can be re-added afterwards.

## 3. Stealth Mechanisms (Cloaking)

### 3.1 Icon Cloaking & Disguise
Hiding the application icon prevents the child from easily finding and launching the app interface.
*   **Method A: `setComponentEnabledSetting` (Legacy)**
    *   Disabling the main launcher activity. Effective on older Androids, but generates a notification on some Android 10+ devices.
*   **Method B: `<activity-alias>` (Recommended)**
    *   Use an alias to present the app as "Calculator Service" or "SIM Toolkit" with a boring, system-like icon.
    *   This provides plausible deniability if the icon is discovered.

### 3.2 Notification Camouflage
Foreground Services *must* show a notification. We cannot hide it, so we must **disguise** it.
*   **Channel Configuration**:
    *   Name: "System Core" or "Battery Manager".
    *   Importance: `IMPORTANCE_MIN` (No sound, no vibration, collapsed in shade).
*   **Content**: "System optimization active" or "Checking for updates...".
*   **Visuals**: Use a generic "Gear" or "Android Robot" icon using vector drawables.

## 4. Anti-Tamper Measures
*   **Boot Receiver**: Listen for `BOOT_COMPLETED` to auto-start the service immediately after phone restart.
*   **Service Restarter**: Implement `START_STICKY` in the service return value. If the system kills the service due to low memory, it will attempt to recreate it automatically.

## 5. Technical Requirements
*   **Permissions**: `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
*   **ADB Access**: Required for Device Owner provisioning and initial setup.
