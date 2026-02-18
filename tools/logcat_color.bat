@echo off
setlocal

:: Configuration
set PACKAGE_NAME=com.zero.sentinel
set ADB=adb

:: Check if ADB is in PATH
where %ADB% >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] ADB not found in PATH. Please verify Android SDK platform-tools installation.
    pause
    exit /b 1
)

echo [INFO] Clearing old logs...
%ADB% logcat -c

echo [INFO] Starting colored logcat for %PACKAGE_NAME%...
echo [INFO] Press Ctrl+C to stop.
echo.

:: Use adb logcat with color output
:: grep is usually not available on standard Windows CMD, so we use findstr which doesn't support colors well for the match itself,
:: BUT adb -v color preserves colors if the terminal supports it.
:: To get good filtering + coloring on Windows without external tools (like pidcat) is tricky.
:: Best approach: Use pidcat if available, or fall back to native adb shell pid filtering.

:: Method 1: ADB with color and simple piping (Colors might be lost in pipe on some terminals)
:: %ADB% logcat -v color | findstr "%PACKAGE_NAME%"

:: Method 2: PID filtering (Preserves color better as we don't pipe the log stream itself)
:: We get the PID of the app and then filter by that PID.

:GET_PID
for /f "tokens=2" %%i in ('%ADB% shell pidof %PACKAGE_NAME%') do set PID=%%i

if "%PID%"=="" (
    echo [WAITING] App %PACKAGE_NAME% not running...
    timeout /t 2 >nul
    goto GET_PID
)

echo [INFO] Found PID: %PID%
echo [INFO] Streaming logs...

:: Stream logs for specific PID with color
%ADB% logcat -v color --pid=%PID%

:: If app crashes/restarts, loop back
goto GET_PID
