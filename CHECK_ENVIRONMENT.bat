@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo             PROJECTFLOW INTELLIGENCE - ENVIRONMENT AUDIT
echo ======================================================================
echo.

:: 1. Check or map virtual drive P:
if not exist "P:\" (
    echo [INFO] Mapping virtual drive P: to protect against non-ASCII paths...
    subst P: "%~dp0" >nul 2>&1
    if exist "P:\" (
        echo [OK] Virtual drive P: successfully mapped to %~dp0
    ) else (
        echo [WARN] Could not map virtual drive P:. Using current directory.
    )
) else (
    echo [OK] Virtual drive P: is active.
)
echo.

:: 2. Check Java 21 LTS
echo [1/4] Checking Java Runtime Environment...
set "JAVA_CMD=java"
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    set "JAVA_CMD=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
)

"%JAVA_CMD%" -version 2>&1 | findstr /i "version"
if %ERRORLEVEL% EQU 0 (
    echo [OK] Java detected.
) else (
    echo [ERROR] Java not found. Please install OpenJDK 21 LTS.
)
echo.

:: 3. Check Node.js and NPM
echo [2/4] Checking Node.js and NPM...
call node -v >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%i in ('node -v') do echo [OK] Node.js: %%i
    for /f "tokens=*" %%i in ('npm -v') do echo [OK] NPM: %%i
) else (
    echo [ERROR] Node.js / NPM not found in PATH.
)
echo.

:: 4. Check Android SDK
echo [3/4] Checking Android SDK...
set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
if exist "%SDK_PATH%" (
    echo [OK] Android SDK located at: %SDK_PATH%
) else (
    echo [WARN] Android SDK not detected at standard location %SDK_PATH%
)
echo.

:: 5. Check ADB Device Connectivity
echo [4/4] Checking ADB and Connected Mobile Devices...
set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if exist "%ADB_CMD%" (
    "%ADB_CMD%" devices
    echo [OK] ADB platform-tools operational.
) else (
    echo [WARN] adb.exe not found at %ADB_CMD%
)
echo.

echo ======================================================================
echo Environment check complete.
echo ======================================================================
pause
