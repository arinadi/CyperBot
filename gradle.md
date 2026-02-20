# Gradle Configuration

This document details the build configuration for the Zero-Sentinel (CyperBot) project. It serves as a reference for configuring dependencies, plugins, and build parameters.

## 📂 Project Structure

The project follows a standard multi-module Android structure:
*   `build.gradle.kts` (Root): Defines common plugins and build configuration.
*   `app/build.gradle.kts` (Module): Specific configuration for the Android application.
*   `settings.gradle.kts`: Defines the project name and included modules.

## 🛠️ Build Environment

*   **Gradle Plugin**: 8.5.1
*   **Kotlin Version**: 1.9.24
*   **KSP Version**: 1.9.24-1.0.20
*   **Java Version**: 21 (Source & Target Compatibility)

## 📦 Gradle Wrapper

The project uses the **Gradle Wrapper** to ensure a consistent build environment across all machines (developers & CI).

*   **Unix/macOS**: `./gradlew`
*   **Windows**: `gradlew.bat` (or just `gradlew` in PowerShell)
*   **Configuration**: `gradle/wrapper/gradle-wrapper.properties`

Always use the wrapper scripts (`gradlew`) instead of a local `gradle` installation.

## 📱 Android Configuration

Defined in `app/build.gradle.kts`:

| Parameter | Value | Description |
| :--- | :--- | :--- |
| `namespace` | `com.zero.sentinel` | Unique package name for resources. |
| `applicationId` | `com.zero.sentinel` | Unique ID for the app on the device. |
| `compileSdk` | `34` | Android 14 (Upside Down Cake). |
| `minSdk` | `26` | Android 8.0 (Oreo). |
| `targetSdk` | `34` | Android 14 (Upside Down Cake). |

### Versioning Strategy
The app uses **Git-based versioning**. The version name is dynamically generated at build time:

```kotlin
versionName = "2.$commitCount.$commitHash"
versionCode = commitCount
```
*   `commitCount`: Total number of commits in the current branch.
*   `commitHash`: First 7 characters of the current commit hash.

### Signing Config
Release builds are signed using a keystore defined via **Environment Variables**:
*   `SIGNING_KEY_STORE_BASE64` (CI) / `release.jks` (Local)
*   `SIGNING_STORE_PASSWORD`
*   `SIGNING_KEY_ALIAS`
*   `SIGNING_Key_PASSWORD`

## 📦 Dependencies

Dependencies are defined directly in `app/build.gradle.kts`.

### Core Libraries
*   **AndroidX**: Core KTX, AppCompat, ConstraintLayout, Material Design.
*   **Lifecycle**: Runtime KTX, Service (for background operations).

### Functional Libraries
*   **Room (2.6.1)**: Local database. Uses KSP for annotation processing.
*   **WorkManager (2.9.0)**: Background task scheduling.
*   **OkHttp & Gson**: Network requests and JSON parsing.
*   **Security Crypto**: EncryptedSharedPreferences for securing sensitive data.

## 🚀 Common Tasks

Run these commands from the project root (use `./gradlew` on Linux/Mac or `gradlew` on Windows):

*   **Build Debug APK**: `./gradlew assembleDebug`
*   **Build Release APK**: `./gradlew assembleRelease`
*   **Build All Variants**: `./gradlew assemble`
*   **Run Lint Checks**: `./gradlew lint`
*   **Clean Project**: `./gradlew clean`
*   **Check Dependencies**: `./gradlew app:dependencies`

## ⚠️ Troubleshooting

### JDK Compatibility
**Error**: `Unsupported class file major version`
**Fix**: Ensure you are using **JDK 17** or higher. The build is configured for Java 17 compatibility.

### KSP / Room Issues
**Error**: `Schema export directory is not provided to the annotation processor`
**Fix**: The configuration is already present in `app/build.gradle.kts`:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```
Ensure the `schemas` folder exists or is writable.
