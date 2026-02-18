# Zero-Sentinel Todo List

## Phase 4: Stealth & Persistence
- [ ] **Implement SIM Toolkit Decoy UI**
  - [ ] **Icon**: Ensure app icon remains as "SIM Menu" (ic_sim_menu).
  - [ ] **Decoy Layout**:
    - Create a simple `ListView` mimicking a standard SIM Toolkit menu.
    - **Items**: 
      - "Search"
      - "Favorites"
      - "Transaction Services"
      - "Information Services"
      - "Entertainment Services"
      - "USSD Service Catalog"
      - "Help and Support"
  - [ ] **Trigger Logic**:
    - Clicking on most items should show a generic "Service not available" or "Connection error" toast to simulate a real cellular menu.
    - **Secret Trigger**: Clicking on a specific item (e.g., "Help and Support" or a hidden specific sequence).
  - [ ] **Authentication**:
    - Show a numeric input dialog (PIN Pad).
    - **Validation**: Check against a hardcoded or configured numeric PIN. Use digits only (0-9).
    - **Success**: Unlock the real Admin UI / Main Activity.
    - **Failure**: Show "Invalid MMI Code" or similar generic error.

## Phase 4: Intelligence & Data Gathering
- [ ] **Implement Passive Media Monitoring and Location Tracking (Smart Scan)**
  - [ ] **Logic**: Only check for valid photo/video files created *today*.
  - [ ] **Efficiency**: Avoid full directory scanning. Use `MediaStore` queries with date filters.
  - [ ] **Deduplication**: Compare file name/path with the last uploaded file (stored in `EncryptedSharedPreferences`).
  - [ ] **Constraint**: If `current_file.name == last_uploaded_file.name`, skip upload & reporting.
  - [ ] **Objective**: Get location metadata (EXIF) from the latest photo without triggering GPS.
