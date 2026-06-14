# CalcApp

Android calculator app with live currency conversion. Built with Kotlin + Jetpack Compose.

## Project Structure

```
app/src/main/java/com/example/calcapp/
├── MainActivity.kt                  # Entry point — just calls AppScreen(), no theme wrapper
├── ui/
│   ├── AppTheme.kt                  # DarkModePref, AccentScheme, AppColors, buildAppColors, LocalAppColors
│   ├── CalculatorScreen.kt          # AppScreen (root), CalculatorScreen, CurrencySelector, ButtonGrid, dialogs
│   ├── CalculatorViewModel.kt       # CalculatorUiState, CalculatorAction, state machine + currency logic
│   ├── HistoryScreen.kt             # Calculation history list, tap-to-restore
│   ├── SettingsScreen.kt            # Appearance (dark mode + accent), haptic, custom rates
│   └── theme/Theme.kt               # CalcAppTheme — Material3 colour scheme (called from AppScreen, not MainActivity)
└── data/
    ├── ExchangeRateApi.kt           # Retrofit interface + response model (open.er-api.com)
    └── ExchangeRateRepository.kt    # Fetch/cache rates + persist all prefs via DataStore
docs/
├── privacy-policy.html              # Hosted via GitHub Pages for Play Store
├── store-description-short.txt      # 65-char Play Store tagline
└── store-description-long.txt       # Full Play Store description
```

## Build & Install

```bash
# Build debug APK
cd ~/calcApp && ./gradlew assembleDebug

# Connect and install over WiFi ADB
# Port changes every session — user provides it and it is saved to ~/.adb_port
PORT=$(cat ~/.adb_port) && ~/android/platform-tools/adb connect 192.168.68.128:$PORT
~/android/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Key Decisions

- **Exchange rate API**: `open.er-api.com/v6/latest/USD` — free, no API key, 161 currencies
- **Cache TTL**: 24 hours via DataStore; `forceRefresh = true` bypasses it
- **Base currency**: USD — all rates stored relative to USD, cross-rates computed as `value * homeRate / localRate`
- **Input direction**: entered value is treated as **local currency**; home currency equivalent shown alongside
- **Custom rates**: `CustomRateEntry(base, rate)` = "1 base = rate target"; `effectiveUsdRate()` converts to USD-relative for math
- **History**: logged on every `=` press into `List<HistoryEntry>` (max 50), persisted as JSON in DataStore
- **Offline indicator**: `isOffline: Boolean` in `CalculatorUiState`; set true when network request fails
- **Theming**: `LocalAppColors` CompositionLocal provides `AppColors` built from `(effectiveIsDark, AccentScheme)`; `CalcAppTheme` is applied inside `AppScreen` (not `MainActivity`) so the VM's `darkModePref` controls it
- **Currency picker**: uses `Dialog` + `LazyColumn` — **never** `DropdownMenu` + `LazyColumn` (nested vertical scroll crash)
- **`currencyName(code)`**: cached `HashMap` helper in `CalculatorScreen.kt`; use it instead of `JavaCurrency.getInstance().displayName` inline
- **Currency persistence**: all prefs (currencies, recents, custom rates, history, haptic, dark mode, accent) saved to DataStore

## Colour System

All colours come from `LocalAppColors.current` — never hardcode hex values in composables.

```kotlin
val colors = LocalAppColors.current
// Key fields: background, surface, buttonDigit, buttonDigitContent,
// buttonFunction, buttonFunctionContent, textPrimary, textSecondary,
// textMuted, divider, inputBorder, operator, operatorContent,
// equals, equalsContent, fromAmountColor, toAmountColor,
// errorColor, positiveColor, negativeColor, warningColor, promoColor
```

## ADB Setup Notes

- Android SDK at `~/android`, platform-tools at `~/android/platform-tools`
- WiFi ADB preferred; Samsung device at `192.168.68.128`
- Connect port changes every session — shown on device's Wireless Debugging screen
- If `adb connect` says "Connection refused" but the device was connected earlier in the session, `adb install` may still succeed
- Pairing codes expire quickly — have ports ready before running pair command
