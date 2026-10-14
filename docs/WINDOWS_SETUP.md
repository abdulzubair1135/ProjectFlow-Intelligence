# Windows Development Setup

## Windows Toolchain Configuration

### Java Development Kit
- **Recommended**: OpenJDK 21 LTS distributed with Android Studio (`C:\Program Files\Android\Android Studio\jbr`).
- Configure `JAVA_HOME` in System Environment Variables:
  ```powershell
  [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Android\Android Studio\jbr', [System.EnvironmentVariableTarget]::User)
  ```

### Android SDK & Platform Tools
- Located at `%LOCALAPPDATA%\Android\Sdk`.
- Add platform-tools to PATH for direct ADB access:
  ```powershell
  $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
  $userPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
  [System.Environment]::SetEnvironmentVariable("Path", "$userPath;$adbPath", "User")
  ```

### Virtual Drive P: Mapping
To bypass path encoding errors caused by Japanese or non-ASCII characters in Windows paths (`ドキュメント`), run:
```bat
subst P: "%~dp0"
```
The ProjectFlow batch scripts handle this automatically.
