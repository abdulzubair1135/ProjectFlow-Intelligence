# Local Development Guide

## Environment Setup

### 1. Drive Mapping (Windows Path Protection)
If the repository path contains unicode, spaces, or Japanese characters (`ドキュメント`), map a virtual drive letter:
```bat
subst P: "C:\Users\<user>\OneDrive\ドキュメント\puneet"
cd /d P:\
```

### 2. JDK Configuration
Ensure JDK 21 LTS is targeted:
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

### 3. Running Services
- **Backend**: `.\gradlew.bat :backend:bootRun`
- **Admin Panel**: `cd admin-panel && npm run dev`
- **Desktop App**: `.\gradlew.bat :desktop-app:run`
- **Android App**: `INSTALL_ANDROID.bat` (deploys to connected ADB device)
- **All-in-One**: Double-click `START_PROJECTFLOW.bat`
