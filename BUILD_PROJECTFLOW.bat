@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo           PROJECTFLOW INTELLIGENCE - FULL SYSTEM BUILD
echo ======================================================================
echo.

if not exist "P:\" (
    subst P: "%~dp0" >nul 2>&1
)
cd /d "P:\"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/4] Building Shared Core and Backend Services...
call .\gradlew.bat :shared-core:assemble :backend:assemble
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Backend / Shared-Core build failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Backend built successfully.
echo.

echo [2/4] Building Web Admin Control Panel...
cd /d "P:\admin-panel"
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Admin Panel build failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Admin Panel built successfully into dist/.
echo.

echo [3/4] Building Windows Desktop Manager Application...
cd /d "P:\"
call .\gradlew.bat :desktop-app:assemble
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Desktop application build failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Desktop application assembled successfully.
echo.

echo [4/4] Building Native Android Mobile Application...
call .\gradlew.bat :android-app:assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Android build failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Android APK assembled successfully at android-app/build/outputs/apk/debug/.
echo.

echo ======================================================================
echo           ALL PROJECTFLOW COMPONENTS BUILT SUCCESSFULLY!
echo ======================================================================
pause
