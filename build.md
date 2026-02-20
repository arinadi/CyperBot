# Build & Release Workflow

This document details the CI/CD pipeline defined in `.github/workflows/build.yml`. This workflow handles building the Android application, running tests (if configured), generating artifacts, and creating GitHub Releases.

## 🚀 Workflow Overview

*   **Name**: Build and Release
*   **Runs On**: `ubuntu-latest`
*   **Java Version**: JDK 21 (Temurin distribution)

## 🎯 Triggers

The workflow is triggered automatically on:
1.  **Push** to `main` or `master` branches.
2.  **Push** of tags matching `v*` (e.g., `v1.0.0`).
3.  **Pull Request** targeting `main` or `master`.

## 🔑 Keystore Generation & Secrets Setup

To sign the release APK, you must generate a keystore and configure GitHub Secrets.

### 1. Generate a Keystore
Run the following command to generate a new upload key. Keep this file safe!

```bash
keytool -genkey -v -keystore app/release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```
*Replace `my-key-alias` with your desired alias.*
*You will be prompted to create a password for the keystore and the key.*

### 2. Base64 Encode the Keystore
GitHub Secrets cannot store binary files directly. You must encode the `.jks` file to Base64.

**Mac/Linux:**
```bash
base64 -i app/release.jks -o signing_key_base64.txt
```

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/release.jks")) | Out-File -Encoding ASCII signing_key_base64.txt
```

### 3. Add Secrets to GitHub
Go to **Settings > Secrets and variables > Actions** in your repository and add the following secrets:

| Secret Name | Value |
| :--- | :--- |
| `SIGNING_KEY_STORE_BASE64` | The content of `signing_key_base64.txt` (the long base64 string). |
| `SIGNING_STORE_PASSWORD` | The password you set for the keystore. |
| `SIGNING_KEY_ALIAS` | The alias you used (e.g., `my-key-alias`). |
| `SIGNING_KEY_PASSWORD` | The password you set for the key (usually same as store password). |
| `GITHUB_TOKEN` | Automatically provided by GitHub Actions (no setup needed). |

## 🛠️ Build Steps

The workflow executes the following steps in order:

1.  **Checkout Code**: Fetches the full history (`fetch-depth: 0`) for version calculation.
2.  **Set up JDK 21**: Installs Java 21 to build the Android project.
3.  **Decode Keystore**:
    *   Takes the `SIGNING_KEY_STORE_BASE64` secret.
    *   Decodes it back to `app/release.jks` so the build system can use it.
4.  **Build APKs**:
    *   Runs `./gradlew assembleRelease assembleDebug`.
    *   Uses the configured secrets to sign the release APK.
5.  **Upload Artifacts**:
    *   Uploads debug and release APKs as a workflow artifact named `app-apks`.
    *   Retention is managed by GitHub Default (usually 90 days).

## 🏷️ Versioning & Release

### Automatic Versioning
The workflow calculates a version name dynamically based on git commits:
```bash
VERSION_NAME = "2.{COMMIT_COUNT}.{COMMIT_HASH}"
```
*   `COMMIT_COUNT`: Total number of commits on the branch.
*   `COMMIT_HASH`: Short SHA of the current commit.

### GitHub Release
If the pipeline runs on `main` or `master`, it creates a GitHub Release:
*   **Tag**: `v{VERSION_NAME}`
*   **Files**: Attaches both Debug and Release APKs.
