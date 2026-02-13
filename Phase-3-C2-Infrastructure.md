# Phase 3: C2 Infrastructure - Telegram Bot API Integration

## 1. Overview
This phase details the Command and Control (C2) infrastructure. Instead of maintaining expensive, detectable VPS servers, "Zero-Sentinel" uses the **Telegram Bot API** as a serverless, secure, and resilient communication channel. The bot acts as the intermediary between the child's device (Agent) and the parent (Controller).

## 2. Communication Protocol: Long Polling
The application connects directly to `api.telegram.org` using standard HTTPS requests.

### 2.1 Why Long Polling?
Webhooks require a public IP/domain, which is not feasible for a stealthy, serverless mobile agent. **Long Polling** allows the app to maintain a near-real-time connection without a server.
*   **Mechanism**: The app sends a `GET /getUpdates` request with a `timeout=50` parameter.
*   **Behavior**: The connection hangs open until the server has new data (a command from the parent) OR the timeout is reached.
*   **Efficiency**: This drastically reduces network traffic and battery consumption compared to frequent short polling (e.g., every 5 seconds).

### 2.2 Network Library
*   **OkHttp**: Used for robust HTTP/2 handling.
*   **JSON Parsing**: `kotlinx.serialization` or `Gson` for efficient payload handling.

## 3. Data Transmission (Uplink/Downlink)

### 3.1 Uplink: Sending Logs
Data is uploaded from the device to the parent via the `sendDocument` endpoint.
*   **Format**: Encrypted, GZIP-compressed binary files.
*   **Why Files?**: Sending text messages (`sendMessage`) is limited to 4096 characters and can be easily flagged or read if interception occurs. Files are cleaner and support larger payloads.
*   **Trigger**:
    *   Periodic (e.g., every 30 minutes).
    *   Buffer dependent (e.g., when logs exceed 50KB).

### 3.2 Downlink: Receiving Commands
The parent sends commands to the bot chat, which the app retrieves via `getUpdates`.
*   **Format**: JSON strings inside standard messages.
*   **Validation**: The app verifies the `chat_id` of the sender against a hardcoded/configured whitelist. Commands from unknown IDs are silently ignored.

## 4. Command Protocol Specification
The app listens for specific JSON payloads to execute remote actions.

| Command ID | Payload Example | Function |
| :--- | :--- | :--- |
| `CMD_WIPE` | `{"action": "wipe", "force": true}` | **Emergency**: Delete all local logs and database entries immediately. |
| `CMD_LOCATE` | `{"action": "locate", "precision": "high"}` | request Single Location Update (GPS/Network). Responses are sent back as location messages. |
| `CMD_HIDE` | `{"action": "hide", "alias": "calc"}` | Trigger "Icon Cloaking" or change the app alias dynamically. |
| `CMD_CONFIG` | `{"action": "config", "upload_interval": 30}` | Update internal settings (e.g., upload frequency) without app update. |
| `CMD_STATUS` | `{"action": "status"}` | Request a health check (Battery level, Last sync time, permission status). |

## 5. Security Considerations
*   **Chat ID Whitelisting**: CRITICAL. Prevents unauthorized users who find the bot from controlling the agent.
*   **SSL Pinning (Optional)**: Can be implemented to prevent Man-in-the-Middle (MitM) attacks on the connection to Telegram API.
*   **Ephemeral Commands**: Commands should be processed once and then locally discarded/acknowledged (via `offset` parameter in `getUpdates`) to prevent replay attacks.
