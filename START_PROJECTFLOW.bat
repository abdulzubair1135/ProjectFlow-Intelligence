@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo              PROJECTFLOW INTELLIGENCE - FULL SYSTEM LAUNCHER
echo ======================================================================
echo.

:: 1. Virtual Drive Mapping
if not exist "P:\" (
    echo [INFO] Mapping drive P: for path safety...
    subst P: "%~dp0" >nul 2>&1
)
if exist "P:\" (
    cd /d "P:\"
) else (
    cd /d "%~dp0"
)

:: 2. ADB Port Reversal for Mobile Device
set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if exist "%ADB_CMD%" (
    "%ADB_CMD%" reverse tcp:8082 tcp:8082 >nul 2>&1
    echo [OK] ADB Reverse port forwarding (8082 -> 8082) enabled for Android device.
)

:: 3. Launch Node.js Full-Stack Server in background
echo [1/2] Starting ProjectFlow Full-Stack Server on port 8082...
netstat -aon | findstr :8082 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% neq 0 (
    start "ProjectFlow Server [Port 8082]" cmd /k "node server.js"
    timeout /t 3 >nul
) else (
    echo [OK] Server is already running on port 8082.
)

:: 4. Open Website in default browser
echo [2/2] Opening Web Admin Control Panel...
start http://localhost:8082

echo.
echo ======================================================================
echo                      PROJECTFLOW SERVICES ACTIVE
echo ======================================================================
echo.
echo  * Local Website:              http://localhost:8082
echo  * Live Render Cloud:          https://projectflow-intelligence.onrender.com
echo  * Database Persistence:       Supabase PostgreSQL Cloud
echo  * Firebase Push Gateway:      Active (FCM Registered)
echo.
echo  Administrator Credentials:
echo    - Email:    admin@projectflow.io
echo    - Password: ProjectFlow@2026
echo.
echo  To launch Windows Desktop Manager App:
echo    Run: START_DESKTOP.bat
echo.
echo  To terminate services cleanly:
echo    Run: STOP_PROJECTFLOW.bat
echo ======================================================================
echo.
pause
