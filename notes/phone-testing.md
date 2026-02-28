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
- Verify voice speaks each block and progress updates
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

## 8) Bundle your custom routine into the APK
Use this when you want your girlfriend to install the app with your routine already preloaded on first launch.

1. In the app, open your routine and tap `Share JSON`.
2. Send/copy the JSON payload to your computer.
3. Replace file:
	- `app/src/main/assets/bundled_routine.json`
4. Build and share the APK (`./gradlew assembleDebug` or VS Code task).

Notes:
- The bundled routine is only applied when the app has no routines yet (fresh install/app data cleared).
- If the app already has saved routines, it keeps existing data.

## 9) Bundle specific audio files into the APK
If you want songs to travel with the app install (no phone file picker needed):

1. Put your audio files in:
	- `app/src/main/assets/bundled_audio/`
2. In the app editor, open a block and choose `Add bundled song`.
3. Save/share your routine JSON and rebuild the APK.

Supported file extensions:
- `.mp3`, `.wav`, `.ogg`, `.m4a`, `.aac`, `.flac`

## 10) Set a bundled default background image
To ship the app with a default background photo:

1. Add this file:
	- `app/src/main/assets/default_background.jpg`
2. Rebuild the APK.

Notes:
- If no custom background is picked by the user, the app uses this bundled image.
- If the image file is missing, the app falls back to an in-app gradient background.
