@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo          PROJECTFLOW INTELLIGENCE - ANDROID APK INSTALLER
echo ======================================================================
echo.

if not exist "P:\" (
    subst P: "%~dp0" >nul 2>&1
)
cd /d "P:\"

set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB_CMD%" (
    echo [ERROR] ADB not found at: %ADB_CMD%
    exit /b 1
)

echo [1/3] Checking connected Android devices...
"%ADB_CMD%" devices
echo.

echo [2/3] Setting up USB reverse port forwarding (8080 -> 8080)...
"%ADB_CMD%" reverse tcp:8080 tcp:8080
if %ERRORLEVEL% EQU 0 (
    echo [OK] Port 8080 reversed successfully. Android device can reach backend at http://127.0.0.1:8080
) else (
    echo [WARN] ADB reverse failed. Ensure USB debugging is enabled on device.
)
echo.

set "APK_PATH=P:\android-app\build\outputs\apk\debug\android-app-debug.apk"
if not exist "%APK_PATH%" (
    echo [INFO] APK not found. Building debug APK first...
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    call .\gradlew.bat :android-app:assembleDebug
)

echo [3/3] Installing ProjectFlow APK onto connected device...
"%ADB_CMD%" install -r "%APK_PATH%"
if %ERRORLEVEL% EQU 0 (
    echo [OK] APK installed successfully!
    echo.
    echo Launching ProjectFlow on device...
    "%ADB_CMD%" shell am start -n com.projectflow.android/.ui.SplashActivity
) else (
    echo [ERROR] Installation failed. Check device authorization.
)

echo.
echo ======================================================================
pause
