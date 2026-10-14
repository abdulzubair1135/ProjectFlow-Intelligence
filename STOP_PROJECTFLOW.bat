@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo            PROJECTFLOW INTELLIGENCE - STOP ALL SERVICES
echo ======================================================================
echo.

echo Terminating processes on port 8080 (Spring Boot)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Killing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo Terminating processes on port 5173 (Vite Admin Panel)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5173 ^| findstr LISTENING') do (
    echo Killing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [OK] All ProjectFlow background services stopped cleanly.
echo ======================================================================
pause
