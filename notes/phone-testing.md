# Test on Android Phone (No USB)

## 1) Build APK in WSL
From `/home/sam/dev/audioRoutine`:
- `./gradlew assembleDebug`

APK output:
- `app/build/outputs/apk/debug/app-debug.apk`

## 2) Serve APK from Windows PowerShell
Use Windows PowerShell (this is the method that worked):
- `Set-Location "\\wsl.localhost\Ubuntu-22.04\home\sam\dev\audioRoutine\app\build\outputs\apk\debug"`
- `py -m http.server 8000 --bind 0.0.0.0`

## 3) Install from phone browser
On phone (connected to your hotspot), open:
- `http://10.193.160.1:8000/app-debug.apk`

Wait for download to finish, then install.

## 4) If install is blocked
Enable:
- Settings → Security/Apps → Install unknown apps
- Allow for your browser/files app

## 5) Quick smoke test
- Open app
- Create/edit routine blocks
- Save routine
- Start playback
- Verify TTS speaks each block and progress updates
- Verify schedule screen accepts time input

## 6) USB flow (recommended)
If you have a cable, this is faster and more reliable than browser install.

On phone:
- Enable Developer options
- Enable USB debugging
- Set USB mode to File transfer
- Accept the RSA prompt

Build in WSL:
- `./gradlew assembleDebug`

Install from Windows adb:
- `powershell.exe -NoProfile -Command '& "$env:USERPROFILE\platform-tools\adb.exe" devices -l'`
- `powershell.exe -NoProfile -Command '& "$env:USERPROFILE\platform-tools\adb.exe" install -r "$env:USERPROFILE\Downloads\audioRoutine-app-debug.apk"'`

Tip: copy APK from WSL to Windows first:
- `cp /home/sam/dev/audioRoutine/app/build/outputs/apk/debug/app-debug.apk /mnt/c/Users/sam_p/Downloads/audioRoutine-app-debug.apk`

## 7) VS Code task
Use task `Build + Install (Windows ADB)` to:
- Build debug APK in WSL
- Copy APK to Windows Downloads
- Install with Windows `adb`
