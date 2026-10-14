@echo off
setlocal enabledelayedexpansion

if not exist "P:\" (
    subst P: "%~dp0" >nul 2>&1
)
cd /d "P:\"

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Starting ProjectFlow Manager Desktop Application...
call .\gradlew.bat :desktop-app:run
