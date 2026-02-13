# Phase 1: Core Telemetry - Accessibility Service & Event Interception

## 1. Overview
This phase establishes the foundational layer of the "Zero-Sentinel" application. The core objective is to build a robust telemetry engine capable of capturing user interactions (keystrokes, screen content, application usage) using the Android **AccessibilityService** API. This serves as the primary data source for the parental control system.

## 2. Accessibility Service Architecture

### 2.1 Service Declaration
The backbone of the application is a service extending `AccessibilityService`. Unlike standard services, this requires specific declarations in `AndroidManifest.xml` to bind to the system's accessibility layer.

**Manifest Configuration:**
```xml
<service
    android:name=".services.SentinelAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

### 2.2 Service Configuration (`accessibility_service_config.xml`)
To capture the necessary data, the service must request specific capabilities and event types.

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewTextChanged|typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```
- **`typeViewTextChanged`**: For keylogging capabilities.
- **`typeWindowStateChanged`**: For app usage tracking.
- **`typeWindowContentChanged`**: For reading dynamic content (e.g., incoming chat messages).
- **`canRetrieveWindowContent`**: **CRITICAL**. Must be `true` to access the `AccessibilityNodeInfo` hierarchy.

## 3. Telemetry Logic Implementation

### 3.1 Event Processing Pipeline
The `onAccessibilityEvent(AccessibilityEvent event)` method acts as the entry point. The logic must filter and route events efficiently to prevent UI lag.

**Core Event Handlers:**

1.  **Keystroke Capture (`TYPE_VIEW_TEXT_CHANGED`)**:
    *   **Source**: Detects text entry in `EditText` fields.
    *   **Logic**:
        *   Extract `event.getText()`.
        *   **Debouncing**: Implement a buffer to handle rapid updates. For example, typing "User" might generate events "U", "Us", "Use", "User". The logger must resolve this to a single entry "User".
        *   **Context**: Capture `event.getPackageName()` to identify *where* the user is typing (e.g., Chrome vs. WhatsApp).

2.  **App/Activity Tracking (`TYPE_WINDOW_STATE_CHANGED`)**:
    *   **Logic**: extract package name and class name to log app transitions.
    *   *Usage*: "User opened Instagram at 14:30".

3.  **Screen Scraping (`TYPE_WINDOW_CONTENT_CHANGED`)**:
    *   **Target**: Read static or incoming text (e.g., WhatsApp chat bubbles).
    *   **Mechanism**: Perform a **Breadth-First Search (BFS)** traversal on the root node (`getRootInActiveWindow()`).
    *   **Heuristics**: Match nodes against known ID patterns (e.g., `id/message_text` for WhatsApp, though obfuscated IDs require text-based heuristics or layout analysis).

### 3.2 Keylogging & Password Restrictions
*   **Standard Input**: Captures all standard text fields.
*   **Password Fields**: Android sets `inputType="textPassword"` which masks the content from Accessibility Services (returns dots or empty string).
    *   *Constraint*: Real-time password capture via Accessibility is restricted by the OS design for security.
    *   *Strategy*: Focus on capturing the *context* of login events rather than the password itself to maintain stealth and stability.

### 3.3 Data Sanitation
*   **Noise Filtering**: Ignore system UI elements (Clock, Battery status, Notification shade) to keep logs clean.
*   **Deduplication**: Compare current text state with the previous state to avoid logging identical content repeatedly.

## 4. Bypassing "Restricted Settings" (Android 13+)

### 4.1 The Challenge
Side-loaded applications requesting Accessibility Services on Android 13+ are blocked by "Restricted Settings". The user sees a "Restricted" dialog when trying to enable the service.

### 4.2 The Solution: Guided User Flow
There is no programmatic bypass without Root. The solution is a **User Education Implementation (Onboarding Wizard)**.

**Step-by-Step Guide Implementation:**
The app must detect the "Restricted" state and display an interactive overlay/tutorial guiding the parent through the unlock process:

1.  **Navigate to System Settings**: Open `Settings -> Apps -> [App Name]`.
2.  **Access Hidden Menu**: Click the three-dot menu in the top-right corner.
3.  **Allow Restricted Settings**: Select the "Allow restricted settings" option.
    *   *Note*: This option **only** appears after the user has attempted to enable the service and failed once.
4.  **Enable Service**: Return to Accessibility Settings and enable the service (now accessible).

**Vendor-Specific Paths (Deep Linking Strategy):**
The onboarding logic should detect the manufacturer (Google, Samsung, Xiaomi, Oppo/Realme) and adapt instructions/deep-links accordingly (refer to Master Plan Section 2.3 for path table).

## 5. Technical Stack & Requirements
*   **Language**: Kotlin
*   **Minimum API**: Android 8.0 (Oreo) - API 26
*   **Target API**: Android 14 (Upside Down Cake) - API 34
*   **Coroutines**: For asynchronous processing of event data off the main UI thread.
*   **Room Database**: For local buffering of captured logs before encryption/transmission.
