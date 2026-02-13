# Phase 4: Security & Privacy - Zero-Knowledge Architecture

## 1. Overview
This phase implements the "Zero-Knowledge" security model. In this model, the service provider (Telegram) and any potential interceptors are mathematically incapable of reading the user's data. Only the holder of the private key (the Parent) can decrypt the logs.

## 2. Hybrid Encryption Scheme
Due to performance constraints and payload sizes, a Hybrid Cryptosystem combining RSA (Asymmetric) and AES (Symmetric) is required.

### 2.1 Key Generation (Off-Device)
Users must generate a key pair before building the app.
*   **Algorithm**: RSA-4096 (OAEP Padding).
*   **Public Key (`public_key.pem`)**: Embedded into the APK via `assets/` or build config.
*   **Private Key (`private_key.pem`)**: **NEVER** leaves the parent's control. It is NOT included in the app.

### 2.2 On-Device Encryption Process (The "Envelope")
When a log file is ready for upload:

1.  **Session Key Generation**: The app generates a random, single-use 256-bit AES key (`Key_Session`).
2.  **Data Encryption**: The log content is compressed (GZIP) and encrypted using AES-GCM with `Key_Session`.
    *   *Result*: `Encrypted_Data`.
3.  **Key Encryption**: `Key_Session` is encrypted using the embedded RSA `Public_Key`.
    *   *Result*: `Encrypted_Key`.
4.  **Payload Construction**: The final file sent to Telegram is a concatenation:
    `[Header_Length][Encrypted_Key][IV][Encrypted_Data]`

### 2.3 Decryption (Parent-Side)
The parent uses a separate decryptor tool (Python/Desktop app) to:
1.  Read the file header to extract `Encrypted_Key`.
2.  Decrypt `Encrypted_Key` using their `Private_Key` -> Recovers `Key_Session`.
3.  Decrypt the body using `Key_Session` -> Recovers original log.

## 3. Data-at-Rest Security (Local Storage)

### 3.1 Room Database
*   **Structure**: Logs are stored in a local SQLite database accessed via Room.
*   **Volatility**: The database is a *temporary buffer*. Records are deleted immediately upon successful upload (HTTP 200 OK from Telegram).
*   **Crash Recovery**: Write-Ahead Logging (WAL) ensures database integrity if the app is killed mid-write.

### 3.2 Secure Deletion (`SecureDelete`)
When deleting sensitive files (e.g., temporary log artifacts before upload), standard deletion acts only as "unlinking," leaving data recoverable.
*   **Implementation**: A `SecureDelete` utility must overwrite the file content with random bytes or zeros (0x00) *before* deleting the file reference.
    *   *Pass 1*: Overwrite with random data.
    *   *Pass 2*: Overwrite with zeros.
    *   *Pass 3*: Call `File.delete()`.

## 4. Operational Security
*   **Memory Hygiene**: Sensitive variables (like the AES session key) should be cleared from memory (set to null or overwritten) immediately after use to prevent extraction via memory dumps.
*   **Certificate Pinning**: To prevent Man-in-the-Middle (MitM) attacks during the TLS handshake with Telegram servers.
