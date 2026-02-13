# Phase 5: DevOps & Deployment - Automated Release Pipeline

## 1. Overview
This phase automates the build and release process using **GitHub Actions**. Consistent, reproducible builds are critical for security and ease of updates. Since this app is side-loaded, we bypass the Play Store entirely and distribute via GitHub Releases.

## 2. CI/CD Architecture (GitHub Actions)

### 2.1 Workflow Trigger
The pipeline runs on:
1.  **Push to Main**: Ensures the `main` branch always builds.
2.  **Tags (v*)**: Creating a generic tag (e.g., `v1.0.0`) triggers a Release build.

### 2.2 Secure Signing (Keystore Management)
We cannot store the binary Keystore (`.jks`) in the repo.
*   **Solution**: Base64 Encoding.
    1.  Encode the Keystore: `base64 -w 0 release.jks > key_base64.txt`.
    2.  Store `key_base64.txt` content as a **GitHub Secret** (`SIGNING_KEY_STORE_BASE64`).
    3.  **Workflow Step**: Decode the secret back to a file during the build process.

### 2.3 Build Configuration (`build.gradle.kts`)
*   **Obfuscation (R8)**: Enabled for Release builds.
    ```kotlin
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    ```
*   **Why R8?**: It renames classes/methods to random characters (e.g., `a.b.c()`), making reverse engineering significantly harder.

### 2.4 The Pipeline Script (`.github/workflows/build.yml`)
Key steps in the YAML file:
1.  **Checkout Code**.
2.  **Set up JDK 17**.
3.  **Decode Keystore**:
    ```yaml
    - name: Decode Keystore
      run: echo "${{ secrets.SIGNING_KEY_STORE_BASE64 }}" | base64 --decode > app/release.jks
    ```
4.  **Inject Secrets**: Write `local.properties` with API keys from GitHub Secrets.
5.  **Build APK**: `./gradlew assembleRelease`.
6.  **Upload Artifact**: Uses `actions/upload-artifact` to save the APK.
7.  **Create Release**: Uses `softprops/action-gh-release` to publish the APK to the Releases page (only on tag push).

## 3. Deployment & Onboarding (Manual)

### 3.1 Distribution Channel
*   **Primary**: Direct download from the GitHub Releases page (private repo recommended).
*   **Secondary**: Transfer via USB/ADB.

### 3.2 Installation Wizard (The First Run)
The crucial step is getting the parent to set up the device correctly.
1.  **Install APK**: "Install from Unknown Sources" must be allowed.
2.  **Launch App**: App opens an "Onboarding Wizard".
3.  **Grant Permissions**:
    *   Notification Access.
    *   Ignore Battery Optimization.
    *   Accessibility Service (with "Restricted Settings" bypass guide).
    *   Device Admin (Optional).
4.  **Hide App**: Once all checks pass, the app hides its icon and starts background services.

## 4. Update Strategy
Since there is no Play Store, updates must be self-managed.
*   **In-App Updater**: The app checks the GitHub API for new releases (tags).
*   **Notification**: If a new version exists, prompt the parent (downlink command) or silently download.
*   **Note**: Android 14 restricts non-store apps from updating themselves without user intervention explicitly approving the install.
