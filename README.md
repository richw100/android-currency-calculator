# Currency Calc

An Android calculator with live dual-currency conversion. Enter any amount and instantly see the equivalent in your chosen currency pair — as you type.

## Features

- **4-function calculator** with bracket support and smart bracket button
- **Live currency conversion** — from/to currencies shown alongside every calculation
- **161 currencies** sourced from [open.er-api.com](https://open.er-api.com) (free, no API key)
- **24-hour cache** — works offline using the last fetched rates
- **Manual refresh** with live spinner
- **Currency flags** and full currency names in the selector dropdown
- **Recent currencies** pinned to the top of the dropdown
- **Swap button** to instantly reverse the from/to pair
- **Persistent selections** — chosen currencies and recents saved across launches
- **About screen** accessible via the top menu

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Networking | Retrofit + Gson |
| Persistence | DataStore (Preferences) |
| Build | Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20 |
| Min SDK | 26 (Android 8.0) |

## Building

### Prerequisites

- JDK 17
- Android SDK at `~/android` (or update `local.properties` with your SDK path)
  - `platforms;android-35`
  - `build-tools;35.0.0`
  - `platform-tools`

### Build

```bash
cd android-currency-calculator
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

### Install via WiFi ADB

Enable **Developer Options → Wireless Debugging** on your Android device, then:

```bash
# Pair (one-time — code expires in ~60 seconds)
adb pair <ip>:<pair-port> <6-digit-code>

# Connect
adb connect <ip>:<connect-port>

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> **WSL2 note:** USB ADB is unreliable under WSL2 — WiFi ADB is recommended. See `CLAUDE.md` for full ADB setup notes.

## Project Structure

```
app/src/main/java/com/example/calcapp/
├── MainActivity.kt
├── ui/
│   ├── AppScreen.kt          # Top bar + navigation between screens
│   ├── CalculatorScreen.kt   # Calculator UI and About screen
│   ├── CalculatorViewModel.kt
│   └── theme/Theme.kt
└── data/
    ├── ExchangeRateApi.kt
    └── ExchangeRateRepository.kt
```

## Exchange Rates

Rates are fetched from `open.er-api.com/v6/latest/USD` — free, no API key required. All rates are stored relative to USD; cross-rates are computed as:

```
toValue = inputValue × toRate / fromRate
```

Rates are cached for 24 hours via DataStore. The app falls back to cached rates when offline.

## Developer Guide

A detailed guide covering the architecture, Gradle setup, Kotlin concepts, and ADB workflow is in [`docs/calcapp-guide.html`](docs/calcapp-guide.html).

---

If you find this useful, [buy me a coffee](https://buymeacoffee.com/rww_100) ☕
