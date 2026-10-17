@echo off
setlocal enabledelayedexpansion
title ProjectFlow Intelligence - Website Launcher

echo ======================================================================
echo           PROJECTFLOW INTELLIGENCE - WEBSITE LAUNCHER
echo ======================================================================
echo.

:: 1. Path Safety Check
if not exist "P:\" (
    subst P: "%~dp0" >nul 2>&1
)
if exist "P:\" (
    cd /d "P:\"
) else (
    cd /d "%~dp0"
)

:: 2. Check if Node.js server is already running on port 8082
netstat -aon | findstr :8082 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [OK] ProjectFlow Server is already active on port 8082.
) else (
    echo [INFO] Starting ProjectFlow Full-Stack Server on port 8082...
    start "ProjectFlow Web Server [Port 8082]" cmd /k "node server.js"
    timeout /t 3 >nul
)

:: 3. Launch default browser to Website
echo [INFO] Launching Web Admin Control Panel in your browser...
start http://localhost:8082

echo.
echo ======================================================================
echo                     WEBSITE IS NOW RUNNING!
echo ======================================================================
echo.
echo   * Local Web URL:          http://localhost:8082
echo   * Live Render Cloud URL:  https://projectflow-intelligence.onrender.com
echo.
echo   Administrator Credentials:
echo     - Email:    admin@projectflow.io
echo     - Password: ProjectFlow@2026
echo.
echo   Features Active:
echo     - 100% Real Supabase Cloud Database (Zero Demo Data)
echo     - WhatsApp-Style Direct & Group Discussions
echo     - Interactive 4-Lane Kanban Task Board
echo     - Firebase FCM Push Broadcast Engine
echo     - Audit Logging with CSV/JSON Export
echo.
echo   To stop the server later, double-click: STOP_PROJECTFLOW.bat
echo ======================================================================
echo.
pause
