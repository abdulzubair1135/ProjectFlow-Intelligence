@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo            PROJECTFLOW INTELLIGENCE - STOP ALL SERVICES
echo ======================================================================
echo.

echo Terminating processes on port 8082 (ProjectFlow Node Full-Stack Server)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8082 ^| findstr LISTENING') do (
    echo Killing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo Terminating processes on port 8080 (Legacy Backend)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Killing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo Terminating processes on port 5173 (Dev Admin Panel)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5173 ^| findstr LISTENING') do (
    echo Killing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [OK] All ProjectFlow services stopped cleanly.
echo ======================================================================
pause
