# TripCalc — Bug Fix History

A summary of bugs found and fixed across the project, compiled from the git history.
Commit hashes reference the Android repository.

## Crashes

- **Currency picker crash** (`fc23e71`) — Putting a `LazyColumn` inside a `DropdownMenu` is
  illegal in Compose (nested vertical scroll) and crashed the app when opening the picker.
  Replaced with `Dialog` + `LazyColumn`, which also improved scroll performance by caching
  currency display-name lookups.
- **Release build crash after package rename** (`6090d6d`, v1.0.1) — The R8/ProGuard keep rule
  still referenced the old `com.example.calcapp` package and `-keepattributes Signature` was
  missing, so the minified release build crashed while the debug build worked.
- **`removeLast()` crash on Android 14 and earlier** (`95cecbd`, v1.2.7) — Reported from the
  field: `bracketStack.removeLast()` compiled against SDK 35 resolves to a Java 21 API that
  doesn't exist on older Android, crashing when closing brackets. Replaced with
  `removeAt(lastIndex)`.
- **Old saved cards crashing after upgrade** (`9db7aef`) — Card profiles saved before new fields
  were added would deserialize with nulls and crash; Gson null-safety applied at load time.

## Visual / layout issues

- **Status bar icons invisible** (`08ae02d`, v1.5.0) — When the app's dark mode differed from the
  system theme, the battery/signal icons blended into the status bar. Fixed with a `SideEffect`
  syncing the icon colour to the app's effective theme.
- **Unreadable text in dark mode** (`6090d6d`) — SegmentedButton tabs and FilterChips in Settings
  had bad contrast in dark mode.
- **Dropdown arrow invisible in dark mode** (`67c85d5`) — Units tab dropdown arrow needed an
  explicit tint from AppColors.
- **Calculator didn't fit all screen sizes** (`cac059f`, v1.1.1) — The layout scrolled instead of
  scaling; button grid now fills remaining height via weights, adapting to any device.
- **Tab labels wrapping to two lines** (`cac059f`) — Clamped to one line with ellipsis on narrow
  screens.
- **Top bar title truncated on small screens** (`5856ca4` / `1df0aeb`, v1.3.1–1.3.2) — First
  ellipsised, then replaced with a scrolling marquee.
- **Landscape clipping** (`8236a3e`) — Controls were cut off in landscape before the scaling
  rework.

## Behaviour bugs

- **OCR→tip double taxation** (`20afb16`, v1.6.0) — Sending a tax-added total from the receipt
  scanner to the Bill screen would add sales tax again. The "add tax" option is now
  auto-disabled when the sent value already includes it.
- **"Service included" shown with No Tip** (`414b3a6`) — The tip breakdown displayed a stale
  "Service included" line when No Tip was selected.
- **Tax chip long-press not registering** (`64922eb`) — `combinedClickable` on the chip lost
  pointer events to an inner element; fixed with a transparent overlay using
  `detectTapGestures`.
- **Card rates not saved on new cards** (`95cecbd`) — Custom rates set while creating a card were
  dropped; now carried through the add/update actions.
- **Unreliable haptic feedback** (`d731637`) — The Compose haptic abstraction fired
  inconsistently; switched to `VibrationEffect.EFFECT_CLICK` directly.
- **OCR misreads** (`8e343bc`) — Split decimals ("10" + ".35") merged via a spatial heuristic,
  trailing letters stripped ("10.35R"), and conversion bubbles re-anchored so they no longer
  covered the prices below.

## Security review fixes (v1.6.0)

All low severity, found in a codebase security audit and fixed in `20afb16`:

1. Country codes were interpolated into the Wikidata SPARQL query and restcountries URL
   unvalidated — now guarded to ISO alpha-2 only.
2. Receipt camera photos were left in the cache directory indefinitely — now deleted right after
   the bitmap is decoded.
3. The FileProvider exposed the entire cache directory — narrowed to the `camera/` subfolder.
4. The blanket ProGuard rule kept every app class, disabling obfuscation entirely — narrowed to
   just the Gson-serialized classes whose field names must survive R8.
5. An unused `okhttp-logging-interceptor` dependency was removed.

## Minor / build hygiene

- Deprecated API cleanups: `Icons.AutoMirrored.Filled.HelpOutline` (`2d963a1`) and the deprecated
  `menuAnchor()` overload (`dea5d29`).
- Welcome dialog briefly flashing for existing users (`f4db428`) — defaulted "seen" to true until
  prefs load.
