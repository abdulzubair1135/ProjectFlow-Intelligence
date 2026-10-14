@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo           PROJECTFLOW INTELLIGENCE - TEST SUITE RUNNER
echo ======================================================================
echo.

if not exist "P:\" (
    subst P: "%~dp0" >nul 2>&1
)
cd /d "P:\"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/3] Running Shared-Core Unit Tests (Deterministic Risk Engine, DAG Validator, What-If Engine)...
call .\gradlew.bat :shared-core:test --info
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Shared-Core tests failed!
    exit /b %ERRORLEVEL%
)
echo [OK] All Shared-Core tests passed.
echo.

echo [2/3] Running Backend Integration Tests (Security, RBAC, REST APIs, Isolated Workspaces)...
call .\gradlew.bat :backend:test --info
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Backend tests failed!
    exit /b %ERRORLEVEL%
)
echo [OK] All Backend integration tests passed.
echo.

echo [3/3] Running Web Admin TypeScript Quality Checks...
cd /d "P:\admin-panel"
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Admin panel build / type-check failed!
    exit /b %ERRORLEVEL%
)
echo [OK] Admin Panel type checks and bundle verification passed.
echo.

echo ======================================================================
echo           ALL TEST SUITES EXECUTED AND PASSED SUCCESSFULLY!
echo ======================================================================
pause
