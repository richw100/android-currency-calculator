# ADB Session Setup for Claude

## Device
- Samsung device at `192.168.68.128`
- Android SDK: `~/android`, platform-tools: `~/android/platform-tools`
- WiFi ADB — port changes every session (shown on device under Settings → Developer options → Wireless Debugging)

## Connect

The current port is stored in `~/.adb_port`. When the user gives you a new port, write it there first:

```bash
echo "PORT" > ~/.adb_port
```

Then connect:

```bash
PORT=$(cat ~/.adb_port) && ~/android/platform-tools/adb connect 192.168.68.128:$PORT
```

If `adb connect` says "Connection refused" but the device was connected earlier in the session, try `adb install` anyway — it may still work.

If multiple devices are listed, target explicitly:

```bash
~/android/platform-tools/adb -s 192.168.68.128:$PORT install -r app/build/outputs/apk/debug/app-debug.apk
```

## Build & Install

```bash
cd ~/calcApp && ./gradlew assembleDebug
PORT=$(cat ~/.adb_port) && ~/android/platform-tools/adb -s 192.168.68.128:$PORT install -r app/build/outputs/apk/debug/app-debug.apk
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Rules
- Always read the port from `~/.adb_port` — never ask the user to run adb commands themselves
- When the user gives a new port number, write it to `~/.adb_port` immediately before connecting
- Pairing codes expire in ~60 seconds — run `adb pair` immediately after the user pastes the code
