# CalcApp

Android calculator app with live currency conversion. Built with Kotlin + Jetpack Compose.

## Project Structure

```
app/src/main/java/com/example/calcapp/
├── MainActivity.kt                  # Entry point
├── ui/
│   ├── CalculatorScreen.kt          # Compose UI — buttons, display, currency panel
│   ├── CalculatorViewModel.kt       # State machine + currency logic
│   └── theme/Theme.kt               # Material3 dynamic colour theme
└── data/
    ├── ExchangeRateApi.kt           # Retrofit interface + response model (open.er-api.com)
    └── ExchangeRateRepository.kt    # Fetch/cache rates + persist currency prefs via DataStore
```

## Build & Install

```bash
# Build debug APK
cd ~/calcApp && ./gradlew assembleDebug

# Install over WiFi ADB (pair first if needed)
~/android/platform-tools/adb pair <ip>:<pair-port> <code>
~/android/platform-tools/adb connect <ip>:<connect-port>
~/android/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Key Decisions

- **Exchange rate API**: `open.er-api.com/v6/latest/USD` — free, no API key, 161 currencies
- **Cache TTL**: 24 hours via DataStore; `forceRefresh = true` bypasses it
- **Base currency**: USD — all rates stored relative to USD, cross-rates computed as `value * homeRate / localRate`
- **Input direction**: entered value is treated as **local currency**; home currency equivalent shown alongside
- **Currency persistence**: selected home/local currencies saved to DataStore, restored on next launch

## ADB Setup Notes

- Android SDK at `~/android`, platform-tools at `~/android/platform-tools`
- USB passthrough via usbipd-win works but Windows often holds the device; WiFi ADB is preferred
- Samsung device (R5CXC334B3V), udev rule: `ATTR{idVendor}=="04e8"` in `/etc/udev/rules.d/51-android.rules`
- ADB pairing codes expire quickly — have ports ready before running pair command
- After pairing, connection port (shown on main Wireless Debugging screen) persists across sessions
