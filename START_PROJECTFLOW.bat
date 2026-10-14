@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo              PROJECTFLOW INTELLIGENCE - FULL SYSTEM LAUNCHER
echo ======================================================================
echo.

:: 1. Virtual Drive Mapping
if not exist "P:\" (
    echo [INFO] Mapping drive P: for non-ASCII path safety...
    subst P: "%~dp0" >nul 2>&1
)
cd /d "P:\"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: 2. ADB Port Reversal for Mobile Device
set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if exist "%ADB_CMD%" (
    "%ADB_CMD%" reverse tcp:8080 tcp:8080 >nul 2>&1
    echo [OK] ADB Reverse port forwarding (8080 -> 8080) enabled for Android device.
)

:: 3. Launch Backend Server in background window
echo [1/3] Starting Spring Boot 3.3.4 Backend Server on port 8080...
start "ProjectFlow Backend [Port 8080]" cmd /k "cd /d P:\ && set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& .\gradlew.bat :backend:bootRun"

:: 4. Launch Admin Web Console in background window
echo [2/3] Starting Web Admin Control Panel on port 5173...
start "ProjectFlow Admin Panel [Port 5173]" cmd /k "cd /d P:\admin-panel && npm run dev"

:: 5. Display Information
echo.
echo ======================================================================
echo                      PROJECTFLOW SERVICES ACTIVE
echo ======================================================================
echo.
echo  * Web Admin Console:       http://localhost:5173
echo  * Backend REST APIs:       http://localhost:8080/api/v1
echo  * Database Mode:           PostgreSQL / Supabase (Fallback to H2-PG)
echo.
echo  Dev Credentials:
echo    - System Administrator:  admin@projectflow.io
echo    - Project Manager:       manager@projectflow.io
echo    - Team Member:           member@projectflow.io
echo.
echo  To launch Windows Desktop Manager App:
echo    Run: START_DESKTOP.bat or .\gradlew.bat :desktop-app:run
echo.
echo  To install and launch on Android Phone:
echo    Run: INSTALL_ANDROID.bat
echo.
echo  To terminate services cleanly:
echo    Run: STOP_PROJECTFLOW.bat
echo ======================================================================
echo.
pause
