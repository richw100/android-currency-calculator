# TripCalc — Feature History

A summary of new features added across the project, compiled from the git history.
Commit hashes reference the Android repository.

## Core calculator & currency conversion

- **Initial app** (`5a6bcf9`) — 4-function calculator with live dual-currency display. Rates from
  open.er-api.com (free, no API key), cached 24h via DataStore, brackets, %, swap currencies,
  currency selection persisted across launches. Kotlin + Jetpack Compose.
- **Currency flags & recents** (`914fd30`) — Flag emojis derived from ISO 4217 codes; last 5
  selected currencies pinned to a Recent section in the picker.
- **Custom exchange rate overrides** (`a06bbed`) — Settings lets you override any live rate as
  "1 GBP = 1.17 EUR"; ★ indicator on the calculator and picker; warning dialogs when selecting
  or refreshing a pair with a custom rate active.
- **Live-rate comparison** (`c1fffb9`) — When a custom rate is active, a coloured line shows how
  far it is from the live mid-market rate (green above, red below).
- **Paste support** (`215cbb4`) — Long-press the display for Copy / Share / Paste; Paste appears
  only when the clipboard holds a parseable number and previews the value first.
- **Calculation history** (`40a7947`) — Every `=` press logged (up to 50 entries) with expression
  and conversion snapshot; tap to restore; persisted via DataStore. Offline indicator added when
  cached rates are in use.
- **History notes & restore safety** (`8236a3e`, `f4db428`) — Per-entry notes and individual
  delete; restore brings back the original currencies with a confirmation dialog when currencies
  differ or a card was used.
- **Default currencies EUR → GBP** for new installs (`9756927`, v1.2.3).

## Card fee profiles

- **Card fee markup** (`cac059f`, v1.1.1) — Global 0–5% markup slider applied to conversions,
  shown in the rate label.
- **Named card profiles** (`9db7aef`, v1.2.0) — Per-card name, fee %, optional minimum fee, and
  per-card custom rates; chip row on the calculator to compare cards. Card rates take priority
  over global custom rates, which beat live rates.
- **Minimum transaction fee & per-card rates** (`f4db428`, v1.2.2) — "3% or min. £3" style fees;
  min-fee currency defaults to home currency.
- **Cards ignore global custom rates by default** (`c01cfdb`, v1.2.6) — Opt-in checkbox per card;
  rate priority documented in the card editor and Help page.

## Conversion modes

- **Distance conversion** (`8236a3e`) — Miles ↔ km with history support.
- **Temperature, Tip/Split, Fuel economy** (`8234ae8`) — °C ↔ °F; tip chips with people stepper
  and copyable breakdown; mpg (UK/US) ↔ L/100km with gallon-type setting.
- **Custom tip %** (`70865f7`) — Editable fourth chip, persisted; configurable default in
  Settings.
- **Tip → FX hand-off** (`e06f02a`) — One tap loads the per-person share into currency mode;
  original bill remembered when switching back.
- **EV efficiency converter** (`814d51e`, v1.2.8) — mi/kWh ↔ kWh/100km.
- **More distance/area units** (`6090d6d`, v1.0.1) — inch ↔ cm, feet ↔ m, sq ft ↔ sq m, with
  per-pair enable toggles in Settings.
- **General Units converter** (`755e24a`, v1.4.0) — Distance tab renamed Units; adds UK/US pints
  and gallons, lb ↔ kg, oz ↔ g, fluid oz ↔ ml, mph ↔ km/h, and a compound stone+lb ↔ kg
  converter; dropdown pair selector.
- **Units tab polish** (`67c85d5`, v1.5.0) — Grouped dropdown by category (Distance / Volume /
  Weight / Speed / Pressure / Energy); yards, acres, US cup, PSI ↔ bar, kcal ↔ kJ added;
  "Show in litres" toggle for ml conversions.
- **Bill tab redesign** (`64922eb`) — Tip tab renamed Bill; No Tip + three customisable preset
  chips (long-press to edit); sales tax editing via long-press on the Tax chip; tax selectable
  independently of tip.

## Receipt scanner (OCR)

- **Receipt OCR scanner** (`6cee37b`, v1.3.0) — Photograph or pick a receipt; ML Kit (on-device,
  offline) detects numbers; tap any price to see its converted value overlaid on the image;
  multi-select with running total and a "Use" button into the calculator.
- **Scanner UX upgrades** (`8e343bc`) — Pinch-to-zoom with pan; currency symbols in bubbles;
  mode picker (FX or Tip) when using a value; separate "Use original" / "Use converted" buttons.
- **Convert toggle & currency detection** (`5f26782`, v1.3.1) — Toggle between converted and raw
  amounts; detects the receipt's currency from symbols (£, $, €, ¥, etc.).
- **Tax-aware hand-off** (`20afb16`, v1.6.0) — Sending a tax-added total to the Bill screen
  auto-disables "add tax" so it isn't applied twice.

## Other screens

- **About screen & top navigation bar** (`cccf21e`) — Hamburger menu, app info, version
  (auto-read from BuildConfig since `cac059f`).
- **Settings screen** (`a06bbed` onwards) — Grew to cover appearance, haptics, custom rates,
  card profiles, tab visibility, unit pairs, tip defaults, fuel gallon type.
- **Clothing & shoe size converter** (`a9eadfa`, v1.2.0) — Men's/women's shoes and clothing
  across EU, UK, US, Japan, Australia, Korea (+ France/Italy for women's clothing).
- **Help page** (`f4db428`, v1.2.2) — Full feature documentation via the menu; one-time welcome
  prompt for new users.
- **Local Info screen** (`ac3cd6d`, v1.5.0) — Country-level travel data: driving side and
  emergency numbers from Wikidata, calling code/currency/languages from REST Countries, plus
  bundled speed limits, BAC limits, tap water safety, plug types and alcohol purchase age.
  7-day cache, recent-country history.

## Theming & UX

- **Haptic feedback** (`33423a5`, `d731637`) — Click feedback on every button with a Settings
  toggle; copy/share menus on converted amounts.
- **Light/dark mode & accent schemes** (`237ed3b`) — System/Light/Dark preference and five accent
  colour schemes, all persisted; full AppColors theming system.
- **Swap 0/. key layout** (`6090d6d`) — Optional `. | 0 | ⌫` bottom-row order, default on since
  v1.1.0.
- **Responsive layout** (`cac059f`) — Button grid scales to fill any screen size instead of
  scrolling.
- **Custom app icon & branding** (`3eb5e76`, `e06f02a`) — Adaptive icon; renamed Currency Calc →
  TripCalc.

## Platform & release

- **iOS app** (`cf13388`, `671a7c3`) — SwiftUI + MVVM port with Codemagic CI; full parity with
  Android v1.2.8.
- **Release signing & R8 minification** (`5764f2f`) — Keystore via gitignored properties file;
  obfuscation fully effective since the v1.6.0 ProGuard-rule narrowing.
- **Debug build variant** (`64922eb`) — Distinct app ID and name so debug and Play Store builds
  coexist on one device.
- **GitHub Pages site** (`126cb48` onwards) — Landing page, privacy policy, and app guide served
  from `docs/`.
