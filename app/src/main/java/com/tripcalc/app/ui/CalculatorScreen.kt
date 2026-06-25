package com.tripcalc.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect as AndroidRect
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import java.util.Currency as JavaCurrency
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripcalc.app.BuildConfig
import com.tripcalc.app.R
import com.tripcalc.app.ui.theme.CalcAppTheme

fun currencyFlag(code: String): String {
    val countryCode = when (code) {
        "EUR" -> "EU"
        else -> code.take(2)
    }
    return try {
        countryCode.uppercase().map { char ->
            String(Character.toChars(char.code - 'A'.code + 0x1F1E6))
        }.joinToString("")
    } catch (e: Exception) { "" }
}

private val currencyNameCache = HashMap<String, String>(200)
fun currencyName(code: String) = currencyNameCache.getOrPut(code) {
    try { JavaCurrency.getInstance(code).displayName } catch (e: Exception) { code }
}

// ── Root composable ──────────────────────────────────────────────────────────

private enum class Screen { Calculator, History, Settings, About, Converter, Help, OcrOverlay }

private data class DetectedNumber(
    val rawText: String,
    val value: Double,
    val imageBox: AndroidRect,
    val selected: Boolean = false
)

private fun loadOrientedBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    val bitmap = context.contentResolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it) } ?: return null
    val rotation = context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    if (rotation == 0f) return bitmap
    val m = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen() {
    var screen by remember { mutableStateOf(Screen.Calculator) }
    var menuExpanded by remember { mutableStateOf(false) }
    val vm: CalculatorViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    val systemDark = isSystemInDarkTheme()
    val effectiveIsDark = when (state.darkModePref) {
        DarkModePref.SYSTEM -> systemDark
        DarkModePref.LIGHT  -> false
        DarkModePref.DARK   -> true
    }
    val colors = buildAppColors(effectiveIsDark, state.accentScheme)

    val context = LocalContext.current
    var ocrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showOcrSourceSheet by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = loadOrientedBitmap(context, uri)
            if (bmp != null) { ocrBitmap = bmp; screen = Screen.OcrOverlay }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                val bmp = loadOrientedBitmap(context, uri)
                if (bmp != null) { ocrBitmap = bmp; screen = Screen.OcrOverlay }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File.createTempFile("ocr_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied — use gallery instead", Toast.LENGTH_SHORT).show()
        }
    }

    CalcAppTheme(darkTheme = effectiveIsDark) {
        CompositionLocalProvider(LocalAppColors provides colors) {
            if (!state.helpHintSeen) {
                AlertDialog(
                    onDismissRequest = { vm.onAction(CalculatorAction.DismissHelpHint) },
                    containerColor = colors.surface,
                    title = { Text("👋 Welcome to TripCalc", color = colors.textPrimary, fontSize = 17.sp) },
                    text = {
                        Text(
                            "Tap the menu (☰) → Help to discover all the features — card profiles, custom rates, size converter, and more.",
                            color = colors.textSecondary, fontSize = 14.sp, lineHeight = 21.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { vm.onAction(CalculatorAction.DismissHelpHint) }) {
                            Text("Got it", color = colors.fromAmountColor)
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .systemBarsPadding()
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00BCD4))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Currency & Holiday Calculator",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (screen == Screen.Calculator) {
                        IconButton(onClick = { showOcrSourceSheet = true }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Scan receipt", tint = colors.textPrimary)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.textPrimary)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Size Converter") },
                                    leadingIcon = { Icon(Icons.Default.Checkroom, null) },
                                    onClick = { screen = Screen.Converter; menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("History") },
                                    leadingIcon = { Icon(Icons.Default.History, null) },
                                    onClick = { screen = Screen.History; menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                                    onClick = { screen = Screen.Settings; menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Help") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, null) },
                                    onClick = { screen = Screen.Help; menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("About") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = { screen = Screen.About; menuExpanded = false }
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { screen = Screen.Calculator }) {
                            Icon(Icons.Default.Close, contentDescription = "Back to calculator", tint = colors.textPrimary)
                        }
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                when (screen) {
                    Screen.Calculator -> CalculatorScreen(vm = vm, modifier = Modifier.weight(1f))
                    Screen.History    -> HistoryScreen(vm = vm, modifier = Modifier.weight(1f), onEntryRestored = { screen = Screen.Calculator })
                    Screen.About      -> AboutScreen(modifier = Modifier.weight(1f))
                    Screen.Settings   -> SettingsScreen(vm = vm, modifier = Modifier.weight(1f))
                    Screen.Converter  -> SizeConverterScreen(vm = vm, modifier = Modifier.weight(1f))
                    Screen.Help       -> HelpScreen(modifier = Modifier.weight(1f))
                    Screen.OcrOverlay -> {
                        val bmp = ocrBitmap
                        if (bmp != null) {
                            OcrOverlayScreen(
                                bitmap = bmp,
                                uiState = state,
                                modifier = Modifier.weight(1f),
                                onUseAmount = { amount ->
                                    vm.onAction(CalculatorAction.PasteValue(amount))
                                    screen = Screen.Calculator
                                },
                                onClose = { screen = Screen.Calculator }
                            )
                        } else {
                            screen = Screen.Calculator
                        }
                    }
                }

                if (showOcrSourceSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showOcrSourceSheet = false },
                        containerColor = colors.surface
                    ) {
                        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                            Text("Scan a receipt or image", fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp, color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = 12.dp))
                            ListItem(
                                headlineContent = { Text("Take photo", color = colors.textPrimary) },
                                leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = colors.textSecondary) },
                                modifier = Modifier.clickable {
                                    showOcrSourceSheet = false
                                    cameraPermissionLauncher.launch("android.permission.CAMERA")
                                }
                            )
                            ListItem(
                                headlineContent = { Text("Choose from gallery", color = colors.textPrimary) },
                                leadingContent = { Icon(Icons.Default.History, null, tint = colors.textSecondary) },
                                modifier = Modifier.clickable {
                                    showOcrSourceSheet = false
                                    galleryLauncher.launch("image/*")
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── About screen ─────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF00BCD4))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(text = "TripCalc", color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(text = "Version ${BuildConfig.VERSION_NAME}", color = colors.textSecondary, fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Your travel companion for money and more. TripCalc gives you live currency conversion across 161 currencies, with support for card fee profiles so you always know what you'll actually pay.",
            color = colors.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Also includes a tip calculator, fuel economy converter, distance and temperature tools, and an international clothing and shoe size guide — everything you need when you're away from home.",
            color = colors.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No ads. No account. No tracking.",
            color = colors.textPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Exchange rates from open.er-api.com\nUpdated every 24 hours · 161 currencies",
            color = colors.textMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rww_100")))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9F0A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9F0A))
        ) {
            Text("Buy me a coffee ☕", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/richw100/android-currency-calculator")))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
        ) {
            Text("View on GitHub", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Calculator screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(vm: CalculatorViewModel = viewModel(), modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showRefreshDialog by remember { mutableStateOf(false) }
    var warningCurrency by remember { mutableStateOf("") }
    var displayMenuExpanded by remember { mutableStateOf(false) }
    val infoTooltipState = rememberTooltipState()
    val rateLabelTooltipState = rememberTooltipState()

    val warningEntry = state.customRates[warningCurrency]
    if (warningCurrency.isNotEmpty() && warningEntry != null) {
        CustomRateWarningDialog(
            currency = warningCurrency,
            entry = warningEntry,
            onKeep = { warningCurrency = "" },
            onClear = {
                vm.onAction(CalculatorAction.ClearCustomRate(warningCurrency))
                warningCurrency = ""
            }
        )
    }

    if (showRefreshDialog) {
        RefreshCustomRateDialog(
            state = state,
            onKeepAndRefresh = {
                vm.onAction(CalculatorAction.RefreshRates)
                showRefreshDialog = false
            },
            onDropAndRefresh = {
                if (state.customRates.containsKey(state.fromCurrency))
                    vm.onAction(CalculatorAction.ClearCustomRate(state.fromCurrency))
                if (state.customRates.containsKey(state.toCurrency))
                    vm.onAction(CalculatorAction.ClearCustomRate(state.toCurrency))
                vm.onAction(CalculatorAction.RefreshRates)
                showRefreshDialog = false
            },
            onDismiss = { showRefreshDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Mode toggle — only show enabled modes (CURRENCY always first)
        val visibleModes = ConversionMode.entries.filter { it in state.enabledModes }
        val segmentColors = SegmentedButtonDefaults.colors(
            activeContainerColor   = colors.operator,
            activeContentColor     = colors.operatorContent,
            inactiveContainerColor = colors.surface,
            inactiveContentColor   = colors.textPrimary,
            activeBorderColor      = colors.operator,
            inactiveBorderColor    = colors.divider
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 2.dp)
        ) {
            visibleModes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.conversionMode == mode,
                    onClick = { vm.onAction(CalculatorAction.SetConversionMode(mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = visibleModes.size),
                    colors = segmentColors
                ) { Text(mode.tabLabel, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }

        when (state.conversionMode) {
        ConversionMode.CURRENCY -> {
            // Currency selectors + inline refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(state.lastUpdated) } },
                    state = infoTooltipState
                ) {
                    IconButton(onClick = { scope.launch { infoTooltipState.show() } }) {
                        Icon(Icons.Default.Info, contentDescription = "Last updated", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
                CurrencySelector(
                    label = "From",
                    selected = state.fromCurrency,
                    available = state.availableCurrencies,
                    recentCurrencies = state.recentCurrencies,
                    customRates = state.customRates,
                    onSelected = { code ->
                        vm.onAction(CalculatorAction.SetFromCurrency(code))
                        if (state.customRates.containsKey(code)) warningCurrency = code
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.onAction(CalculatorAction.SwapCurrencies) }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap currencies", tint = colors.textSecondary)
                }
                CurrencySelector(
                    label = "To",
                    selected = state.toCurrency,
                    available = state.availableCurrencies,
                    recentCurrencies = state.recentCurrencies,
                    customRates = state.customRates,
                    onSelected = { code ->
                        vm.onAction(CalculatorAction.SetToCurrency(code))
                        if (state.customRates.containsKey(code)) warningCurrency = code
                    },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val hasCustom = state.customRates.containsKey(state.fromCurrency)
                            || state.customRates.containsKey(state.toCurrency)
                        if (hasCustom) showRefreshDialog = true
                        else vm.onAction(CalculatorAction.RefreshRates)
                    },
                    enabled = !state.isRefreshing
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.textSecondary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh rates", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Card selector (shown when any card profiles exist)
            if (state.cardProfiles.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.activeCardId == null,
                            onClick = { vm.onAction(CalculatorAction.SetActiveCard(null)) },
                            label = { Text("No card fee", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.buttonFunction,
                                selectedLabelColor = colors.buttonFunctionContent,
                                containerColor = colors.surface,
                                labelColor = colors.textPrimary
                            )
                        )
                    }
                    items(state.cardProfiles) { card ->
                        val feeLabel = if (card.markupPercent == 0.0) "0%"
                            else "+${"%.2f".format(card.markupPercent).trimEnd('0').trimEnd('.')}%"
                        FilterChip(
                            selected = state.activeCardId == card.id,
                            onClick = { vm.onAction(CalculatorAction.SetActiveCard(card.id)) },
                            label = { Text("💳 ${card.name} · $feeLabel", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.operator,
                                selectedLabelColor = colors.operatorContent,
                                containerColor = colors.surface,
                                labelColor = colors.textPrimary
                            )
                        )
                    }
                }
            }

            // Exchange rate info (no last-updated — see Settings)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                if (state.exchangeRateLabel.isNotEmpty()) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(state.exchangeRateLabel) } },
                        state = rateLabelTooltipState
                    ) {
                        Text(
                            text = state.exchangeRateLabel,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { scope.launch { rateLabelTooltipState.show() } }
                        )
                    }
                }
                state.customRatePctDiff?.let { pct ->
                    val isAbove = pct >= 0
                    val liveRate = state.liveRates[state.fromCurrency]
                        ?.let { b -> state.liveRates[state.toCurrency]?.let { t -> if (b != 0.0) t / b else null } }
                    val pctText = "${"%.2f".format(abs(pct))}% ${if (isAbove) "above" else "below"} live"
                    val liveText = liveRate?.let { " (live: ${"%.4f".format(it)})" } ?: ""
                    Text(
                        text = "$pctText$liveText",
                        color = if (isAbove) colors.positiveColor else colors.negativeColor,
                        fontSize = 11.sp
                    )
                }
                if (state.isOffline) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, contentDescription = "Offline", tint = colors.warningColor, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Offline — using cached rates", color = colors.warningColor, fontSize = 11.sp)
                    }
                }
            }
        }
        ConversionMode.DISTANCE -> DistanceConversionRow(
            pair = state.distancePair,
            reversed = state.distanceReversed,
            enabledPairs = state.enabledDistancePairs,
            rateLabel = state.exchangeRateLabel,
            onSwap = { vm.onAction(CalculatorAction.SwapDistanceUnits) },
            onSelectPair = { vm.onAction(CalculatorAction.SetDistancePair(it)) }
        )
        ConversionMode.TEMPERATURE -> TempConversionRow(
            unit = state.tempUnit,
            rateLabel = state.exchangeRateLabel,
            onSwap = { vm.onAction(CalculatorAction.SwapTempUnits) }
        )
        ConversionMode.TIP -> TipControlsRow(
            tipPercent = state.tipPercent,
            customTipPercent = state.customTipPercent,
            people = state.tipPeopleCount,
            onSetPercent = { vm.onAction(CalculatorAction.SetTipPercent(it)) },
            onSetCustomPercent = { vm.onAction(CalculatorAction.SetCustomTipPercent(it)) },
            onSetPeople = { vm.onAction(CalculatorAction.SetTipPeople(it)) }
        )
        ConversionMode.FUEL -> FuelConversionRow(
            inputIsMpg = state.fuelInputIsMpg,
            useUkGallons = state.fuelUseUkGallons,
            fuelMode = state.fuelMode,
            rateLabel = state.exchangeRateLabel,
            onSwap = { vm.onAction(CalculatorAction.SwapFuelDirection) },
            onSetFuelMode = { vm.onAction(CalculatorAction.SetFuelMode(it)) }
        )
        }

            // Conversion display
            if (state.conversionMode == ConversionMode.TIP) {
                TipBreakdownDisplay(
                    bill = state.display.toDoubleOrNull() ?: 0.0,
                    tipPercent = state.tipPercent,
                    people = state.tipPeopleCount,
                    onConvertToFX = { vm.onAction(CalculatorAction.SendTipShareToFX) }
                )
            } else {
                val shareText = if (state.conversionMode == ConversionMode.CURRENCY)
                    "${currencyFlag(state.fromCurrency)} ${state.fromAmount} = ${currencyFlag(state.toCurrency)} ${state.toAmount}"
                else
                    "${state.fromAmount} = ${state.toAmount}"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (state.conversionMode == ConversionMode.CURRENCY) {
                        CopyableAmount(text = "${currencyFlag(state.fromCurrency)} ${state.fromAmount}", color = colors.fromAmountColor, shareText = shareText)
                        CopyableAmount(text = "${currencyFlag(state.toCurrency)} ${state.toAmount}", color = colors.toAmountColor, shareText = shareText)
                    } else {
                        CopyableAmount(text = state.fromAmount, color = colors.fromAmountColor, shareText = shareText)
                        CopyableAmount(text = state.toAmount, color = colors.toAmountColor, shareText = shareText)
                    }
                }
            }

            // Expression line — single line, right-aligned so tail is always visible
            if (state.expression.isNotEmpty()) {
                Text(
                    text = state.expression,
                    color = colors.textSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Main display — font shrinks for longer numbers
            val displayFontSize = when {
                state.display.length <= 6  -> 72.sp
                state.display.length <= 9  -> 56.sp
                state.display.length <= 12 -> 44.sp
                else                       -> 34.sp
            }
            Box {
                Text(
                    text = if (state.isError) "Error" else state.display,
                    color = colors.textPrimary,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .combinedClickable(onClick = {}, onLongClick = { displayMenuExpanded = true }),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                val clipText = clipboard.getText()?.text
                val pasteNumber = remember(clipText) {
                    clipText?.let { Regex("-?\\d+(?:[.,]\\d+)*(?:\\.\\d+)?").find(it)?.value?.replace(",", "") }
                        ?.takeIf { it.toDoubleOrNull() != null }
                }
                DropdownMenu(expanded = displayMenuExpanded, onDismissRequest = { displayMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = { clipboard.setText(AnnotatedString(state.display)); displayMenuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, if (state.conversionMode == ConversionMode.CURRENCY)
                                    "${state.display} ${state.fromCurrency} = ${state.toAmount}"
                                else "${state.fromAmount} = ${state.toAmount}")
                                    }, null
                                )
                            )
                            displayMenuExpanded = false
                        }
                    )
                    if (pasteNumber != null) {
                        DropdownMenuItem(
                            text = { Text("Paste $pasteNumber") },
                            onClick = { vm.onAction(CalculatorAction.PasteValue(pasteNumber)); displayMenuExpanded = false }
                        )
                    }
                }
            }
        }
        // Button grid — weight(1f) fills all remaining vertical space
        ButtonGrid(
            onAction = vm::onAction,
            hapticEnabled = state.hapticEnabled,
            swapZeroDot = state.swapZeroDot,
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 2.dp)
        )

        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rww_100"))) },
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.promoColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.promoColor)
        ) {
            Text(text = "Like using this ad-free app? Buy me a coffee to say thanks! ☕", fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Currency selector ─────────────────────────────────────────────────────────

@Composable
fun CurrencySelector(
    label: String,
    selected: String,
    available: List<String>,
    recentCurrencies: List<String>,
    customRates: Map<String, CustomRateEntry> = emptyMap(),
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val recentSet = remember(recentCurrencies) { recentCurrencies.toSet() }
    val filteredRecents = remember(search, recentCurrencies) {
        recentCurrencies.filter { code ->
            search.isEmpty() || code.contains(search, true) || currencyName(code).contains(search, true)
        }
    }
    val filteredOthers = remember(search, available, recentSet) {
        available.filter { code ->
            code !in recentSet &&
                (search.isEmpty() || code.contains(search, true) || currencyName(code).contains(search, true))
        }
    }

    OutlinedButton(
        onClick = { expanded = true; search = "" },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.inputBorder),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "$label: ${currencyFlag(selected)} $selected${if (customRates.containsKey(selected)) " ★" else ""}",
            fontSize = 13.sp, maxLines = 1
        )
    }

    if (expanded) {
        Dialog(onDismissRequest = { expanded = false; search = "" }) {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        if (filteredRecents.isNotEmpty()) {
                            item {
                                Text("RECENT", fontSize = 10.sp, color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            }
                            items(filteredRecents, key = { it }) { code ->
                                CurrencyPickerRow(code = code, colors = colors, star = customRates.containsKey(code)) {
                                    onSelected(code); expanded = false; search = ""
                                }
                            }
                            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = colors.divider) }
                        }
                        items(filteredOthers, key = { it }) { code ->
                            CurrencyPickerRow(code = code, colors = colors, star = customRates.containsKey(code)) {
                                onSelected(code); expanded = false; search = ""
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CurrencyPickerRow(code: String, colors: AppColors, star: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(currencyFlag(code), fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(code, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Text(currencyName(code), fontSize = 11.sp, color = colors.textSecondary)
        }
        if (star) Text("★", color = colors.warningColor, fontSize = 12.sp)
    }
}

// ── Distance conversion row ───────────────────────────────────────────────────

@Composable
private fun DistanceConversionRow(
    pair: DistancePair,
    reversed: Boolean,
    enabledPairs: Set<DistancePair>,
    rateLabel: String,
    onSwap: () -> Unit,
    onSelectPair: (DistancePair) -> Unit
) {
    val colors = LocalAppColors.current
    val fromLabel = if (reversed) "${pair.toLabel} (${pair.toAbbr})" else "${pair.fromLabel} (${pair.fromAbbr})"
    val toLabel   = if (reversed) "${pair.fromLabel} (${pair.fromAbbr})" else "${pair.toLabel} (${pair.toAbbr})"
    val visiblePairs = DistancePair.entries.filter { it in enabledPairs }
    Column(modifier = Modifier.fillMaxWidth()) {
        // Pair selector chips — only shown when more than one pair is enabled
        if (visiblePairs.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visiblePairs.forEach { p ->
                    val selected = p == pair
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPair(p) },
                        label = { Text("${p.fromAbbr}↔${p.toAbbr}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.buttonDigit,
                            labelColor = colors.textPrimary,
                            selectedContainerColor = colors.operator,
                            selectedLabelColor = colors.operatorContent
                        )
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.inputBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = "From: $fromLabel", color = colors.textPrimary, fontSize = 13.sp, maxLines = 1)
            }
            IconButton(onClick = onSwap) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap units", tint = colors.textSecondary)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.inputBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = "To: $toLabel", color = colors.textPrimary, fontSize = 13.sp, maxLines = 1)
            }
        }
        if (rateLabel.isNotEmpty()) {
            Text(
                text = rateLabel,
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Temperature conversion row ────────────────────────────────────────────────

@Composable
private fun TempConversionRow(unit: TempUnit, rateLabel: String, onSwap: () -> Unit) {
    val colors = LocalAppColors.current
    val toUnit = if (unit == TempUnit.CELSIUS) TempUnit.FAHRENHEIT else TempUnit.CELSIUS
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.inputBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("From: ${unit.label} (${unit.abbr})", color = colors.textPrimary, fontSize = 13.sp, maxLines = 1)
            }
            IconButton(onClick = onSwap) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap units", tint = colors.textSecondary)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, colors.inputBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("To: ${toUnit.label} (${toUnit.abbr})", color = colors.textPrimary, fontSize = 13.sp, maxLines = 1)
            }
        }
        if (rateLabel.isNotEmpty()) {
            Text(rateLabel, color = colors.textSecondary, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
    }
}

// ── Tip controls row ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipControlsRow(
    tipPercent: Double,
    customTipPercent: Double,
    people: Int,
    onSetPercent: (Double) -> Unit,
    onSetCustomPercent: (Double) -> Unit,
    onSetPeople: (Int) -> Unit
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState()
    var showCustomDialog by remember { mutableStateOf(false) }

    if (showCustomDialog) {
        CustomTipPercentDialog(
            current = customTipPercent,
            onSave = { onSetCustomPercent(it); showCustomDialog = false },
            onDismiss = { showCustomDialog = false }
        )
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.operator,
        selectedLabelColor = colors.operatorContent
    )
    val customLabel = if (customTipPercent == customTipPercent.toLong().toDouble())
        "${customTipPercent.toLong()}%" else "${"%.1f".format(customTipPercent)}%"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("The last option is your custom tip %. Tap to select it, tap again to change the value. You can also set it in Settings.") } },
                state = tooltipState
            ) {
                IconButton(onClick = { scope.launch { tooltipState.show() } }) {
                    Icon(Icons.Default.Info, contentDescription = "Custom tip info", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                }
            }
            listOf(10.0, 15.0, 20.0).forEach { pct ->
                FilterChip(
                    selected = tipPercent == pct,
                    onClick = { onSetPercent(pct) },
                    label = { Text("${pct.toInt()}%", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = chipColors
                )
            }
            FilterChip(
                selected = tipPercent == customTipPercent,
                onClick = {
                    if (tipPercent == customTipPercent) showCustomDialog = true
                    else onSetPercent(customTipPercent)
                },
                label = { Text(customLabel, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                colors = chipColors
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Split", color = colors.textSecondary, fontSize = 13.sp, modifier = Modifier.width(36.dp))
            OutlinedButton(
                onClick = { onSetPeople(people - 1) },
                enabled = people > 1,
                modifier = Modifier.size(28.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape
            ) { Text("−", fontSize = 14.sp) }
            Text(
                text = if (people == 1) "Just me" else "$people people",
                color = colors.textPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = { onSetPeople(people + 1) },
                enabled = people < 20,
                modifier = Modifier.size(28.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape
            ) { Text("+", fontSize = 14.sp) }
        }
    }
}

// ── Custom tip % dialog ───────────────────────────────────────────────────────

@Composable
private fun CustomTipPercentDialog(current: Double, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    var text by remember {
        mutableStateOf(
            if (current == current.toLong().toDouble()) current.toLong().toString()
            else "%.1f".format(current)
        )
    }
    val value = text.toDoubleOrNull()
    val valid = value != null && value > 0 && value <= 100

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Custom tip %", color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                suffix = { Text("%", color = colors.textSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onSave(it) } }, enabled = valid) {
                Text("Set", color = colors.fromAmountColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
        }
    )
}

// ── Tip breakdown display ─────────────────────────────────────────────────────

@Composable
private fun TipBreakdownDisplay(bill: Double, tipPercent: Double, people: Int, onConvertToFX: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    val tipAmount = bill * tipPercent / 100.0
    val total = bill + tipAmount
    val perPerson = if (people > 1) total / people else null
    val pctLabel = if (tipPercent == tipPercent.toLong().toDouble())
        tipPercent.toLong().toString() else "%.1f".format(tipPercent)
    val shareText = buildString {
        append("Tip ($pctLabel%): ${"%.2f".format(tipAmount)}\n")
        append("Total: ${"%.2f".format(total)}")
        if (perPerson != null) append("\nEach: ${"%.2f".format(perPerson)}")
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        CopyableAmount(text = "Tip ($pctLabel%):  ${"%.2f".format(tipAmount)}", color = colors.textSecondary, shareText = shareText)
        if (onConvertToFX != null && bill > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onConvertToFX,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (people > 1) "💱 Convert my share" else "💱 Convert to FX",
                        fontSize = 12.sp,
                        color = colors.operator
                    )
                }
                CopyableAmount(text = "Total:  ${"%.2f".format(total)}", color = colors.fromAmountColor, shareText = shareText)
            }
        } else {
            CopyableAmount(text = "Total:  ${"%.2f".format(total)}", color = colors.fromAmountColor, shareText = shareText)
        }
        if (perPerson != null) {
            CopyableAmount(text = "Each:  ${"%.2f".format(perPerson)}", color = colors.toAmountColor, shareText = shareText)
        }
    }
}

// ── Fuel economy conversion row ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelConversionRow(
    inputIsMpg: Boolean,
    useUkGallons: Boolean,
    fuelMode: FuelMode,
    rateLabel: String,
    onSwap: () -> Unit,
    onSetFuelMode: (FuelMode) -> Unit
) {
    val colors = LocalAppColors.current
    val (fromLabel, toLabel) = when (fuelMode) {
        FuelMode.KWH -> if (inputIsMpg) Pair("mi/kWh", "kWh/100km") else Pair("kWh/100km", "mi/kWh")
        FuelMode.MPG -> {
            val mpgLabel = if (useUkGallons) "mpg (UK)" else "mpg (US)"
            if (inputIsMpg) Pair(mpgLabel, "L/100km") else Pair("L/100km", mpgLabel)
        }
    }
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fuelMode == FuelMode.MPG) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Change UK or US gallons in Settings") } },
                    state = tooltipState
                ) {
                    IconButton(onClick = { scope.launch { tooltipState.show() } }) {
                        Icon(Icons.Default.Info, contentDescription = "Gallons info", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f).border(1.dp, colors.inputBorder, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
            ) { Text("From: $fromLabel", color = colors.textPrimary, fontSize = 13.sp, maxLines = 1) }
            IconButton(onClick = onSwap) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap direction", tint = colors.textSecondary)
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f).border(1.dp, colors.inputBorder, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp)
            ) { Text("To: $toLabel", color = colors.textPrimary, fontSize = 13.sp, maxLines = 1) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FuelMode.entries.forEach { mode ->
                val label = if (mode == FuelMode.MPG) "mpg / L" else "mi/kWh"
                val selected = fuelMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { onSetFuelMode(mode) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.operator,
                        selectedLabelColor = colors.operatorContent,
                        containerColor = colors.surface,
                        labelColor = colors.textSecondary
                    )
                )
            }
            if (rateLabel.isNotEmpty()) {
                Text(rateLabel, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
            }
        }
    }
}

// ── Copyable amount ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CopyableAmount(text: String, color: Color, shareText: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Text(
            text = text,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { menuExpanded = true })
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = { clipboard.setText(AnnotatedString(text)); menuExpanded = false }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    context.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }, null)
                    )
                    menuExpanded = false
                }
            )
        }
    }
}

// ── Custom rate dialogs ───────────────────────────────────────────────────────

@Composable
private fun CustomRateWarningDialog(
    currency: String,
    entry: CustomRateEntry,
    onKeep: () -> Unit,
    onClear: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onKeep,
        containerColor = colors.surface,
        title = { Text("Custom Rate Active ★", color = colors.textPrimary) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currencyFlag(entry.base), fontSize = 20.sp)
                    Text(" → ", color = colors.textSecondary, fontSize = 13.sp)
                    Text(currencyFlag(currency), fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("1 ${entry.base} = ${"%.4f".format(entry.rate)} $currency", color = colors.warningColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(10.dp))
                Text("$currency has a custom rate override. The conversion will use this instead of the live rate.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClear) { Text("Clear rate", color = colors.errorColor) } },
        dismissButton = { TextButton(onClick = onKeep) { Text("Keep", color = colors.fromAmountColor) } }
    )
}

@Composable
private fun RefreshCustomRateDialog(
    state: CalculatorUiState,
    onKeepAndRefresh: () -> Unit,
    onDropAndRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val activeEntries = buildList {
        state.customRates[state.fromCurrency]?.let { add(state.fromCurrency to it) }
        state.customRates[state.toCurrency]?.let {
            if (state.toCurrency != state.fromCurrency) add(state.toCurrency to it)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Custom Rate Active ★", color = colors.textPrimary) },
        text = {
            Column {
                Text(
                    "The following custom rate override${if (activeEntries.size > 1) "s are" else " is"} in use:",
                    color = colors.textSecondary, fontSize = 14.sp
                )
                Spacer(Modifier.height(10.dp))
                activeEntries.forEach { (target, entry) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currencyFlag(entry.base), fontSize = 18.sp)
                        Text(" → ", color = colors.textSecondary, fontSize = 13.sp)
                        Text(currencyFlag(target), fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("1 ${entry.base} = ${"%.4f".format(entry.rate)} $target", color = colors.warningColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("Refreshing will fetch the latest live rates. Your custom override will stay active unless you drop it.", color = colors.textMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDropAndRefresh) { Text("Drop & Refresh", color = colors.errorColor) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
                TextButton(onClick = onKeepAndRefresh) { Text("Keep & Refresh", color = colors.fromAmountColor) }
            }
        }
    )
}

// ── Button grid ───────────────────────────────────────────────────────────────

private data class BtnDef(val label: String, val bg: Color, val fg: Color = Color.White, val action: CalculatorAction)

@Composable
fun ButtonGrid(onAction: (CalculatorAction) -> Unit, hapticEnabled: Boolean = true, swapZeroDot: Boolean = false, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    val rows = listOf(
        listOf(
            BtnDef("AC",  colors.buttonFunction, colors.buttonFunctionContent, CalculatorAction.Clear),
            BtnDef("( )", colors.buttonFunction, colors.buttonFunctionContent, CalculatorAction.SmartBracket),
            BtnDef("%",   colors.buttonFunction, colors.buttonFunctionContent, CalculatorAction.Percent),
            BtnDef("÷",   colors.operator,       colors.operatorContent,       CalculatorAction.Operation('/'))
        ),
        listOf(
            BtnDef("7", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(7)),
            BtnDef("8", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(8)),
            BtnDef("9", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(9)),
            BtnDef("×", colors.operator,   colors.operatorContent,    CalculatorAction.Operation('*'))
        ),
        listOf(
            BtnDef("4", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(4)),
            BtnDef("5", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(5)),
            BtnDef("6", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(6)),
            BtnDef("−", colors.operator,   colors.operatorContent,    CalculatorAction.Operation('-'))
        ),
        listOf(
            BtnDef("1", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(1)),
            BtnDef("2", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(2)),
            BtnDef("3", colors.buttonDigit, colors.buttonDigitContent, CalculatorAction.Digit(3)),
            BtnDef("+", colors.operator,   colors.operatorContent,    CalculatorAction.Operation('+'))
        )
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { btn ->
                    CalcButton(
                        label = btn.label, bg = btn.bg, fg = btn.fg,
                        hapticEnabled = hapticEnabled,
                        onClick = { onAction(btn.action) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        // Bottom row: 0, ., ⌫, = (order of first two swappable via setting)
        Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val firstLabel  = if (swapZeroDot) "." else "0"
            val secondLabel = if (swapZeroDot) "0" else "."
            val firstAction  = if (swapZeroDot) CalculatorAction.Decimal else CalculatorAction.Digit(0)
            val secondAction = if (swapZeroDot) CalculatorAction.Digit(0) else CalculatorAction.Decimal
            CalcButton(firstLabel,  colors.buttonDigit, fg = colors.buttonDigitContent, hapticEnabled = hapticEnabled, onClick = { onAction(firstAction) },  modifier = Modifier.weight(1f))
            CalcButton(secondLabel, colors.buttonDigit, fg = colors.buttonDigitContent, hapticEnabled = hapticEnabled, onClick = { onAction(secondAction) }, modifier = Modifier.weight(1f))
            CalcButton("⌫", colors.buttonDigit, fg = colors.buttonDigitContent, hapticEnabled = hapticEnabled, onClick = { onAction(CalculatorAction.Delete) },     modifier = Modifier.weight(1f))
            CalcButton("=", colors.equals,      fg = colors.equalsContent,      hapticEnabled = hapticEnabled, onClick = { onAction(CalculatorAction.Calculate) },  modifier = Modifier.weight(1f))
        }
    }
}

// ── Size converter screen ─────────────────────────────────────────────────────

@Composable
fun SizeConverterScreen(vm: CalculatorViewModel = viewModel(), modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    val table: List<Map<String, String>>
    val countries: List<SizeCountry>
    val fromCountry: String
    val toCountry: String

    when (state.sizeCategory) {
        SizeCategory.SHOE -> {
            table = if (state.shoeIsMens) MEN_SHOE_TABLE else WOMEN_SHOE_TABLE
            countries = SHOE_COUNTRIES
            fromCountry = state.shoeFromCountry
            toCountry = state.shoeToCountry
        }
        SizeCategory.WOMENS -> {
            table = WOMENS_CLOTHING_TABLE
            countries = WOMENS_COUNTRIES
            fromCountry = state.womensFromCountry
            toCountry = state.womensToCountry
        }
        SizeCategory.MENS -> {
            table = MENS_CLOTHING_TABLE
            countries = MENS_COUNTRIES
            fromCountry = state.mensFromCountry
            toCountry = state.mensToCountry
        }
    }

    var selectedSize by remember(state.sizeCategory, state.shoeIsMens, fromCountry) { mutableStateOf("") }
    val availableSizes = remember(table, fromCountry) { table.mapNotNull { it[fromCountry] } }
    val result = if (selectedSize.isNotEmpty()) lookupSize(table, fromCountry, toCountry, selectedSize) else null
    val fromDef = countries.find { it.key == fromCountry }
    val toDef = countries.find { it.key == toCountry }

    fun setCountries(from: String, to: String) = when (state.sizeCategory) {
        SizeCategory.SHOE   -> vm.onAction(CalculatorAction.SetShoeCountries(from, to))
        SizeCategory.WOMENS -> vm.onAction(CalculatorAction.SetWomensCountries(from, to))
        SizeCategory.MENS   -> vm.onAction(CalculatorAction.SetMensCountries(from, to))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            "Clothing & Shoe Size Converter",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Category chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SizeCategory.entries.forEach { cat ->
                FilterChip(
                    selected = state.sizeCategory == cat,
                    onClick = { vm.onAction(CalculatorAction.SetSizeCategory(cat)) },
                    label = { Text(cat.label, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.operator,
                        selectedLabelColor = colors.operatorContent,
                        containerColor = colors.surface,
                        labelColor = colors.textPrimary
                    )
                )
            }
        }

        // Men's / Women's toggle (shoes only)
        if (state.sizeCategory == SizeCategory.SHOE) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(true to "Men's", false to "Women's").forEach { (isMens, label) ->
                    FilterChip(
                        selected = state.shoeIsMens == isMens,
                        onClick = { vm.onAction(CalculatorAction.SetShoeIsMens(isMens)) },
                        label = { Text(label, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.buttonFunction,
                            selectedLabelColor = colors.buttonFunctionContent,
                            containerColor = colors.surface,
                            labelColor = colors.textPrimary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Country selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SizeCountrySelector(
                label = "From",
                selected = fromCountry,
                countries = countries,
                modifier = Modifier.weight(1f),
                onSelected = { setCountries(it, toCountry) }
            )
            IconButton(onClick = { setCountries(toCountry, fromCountry) }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = colors.textSecondary)
            }
            SizeCountrySelector(
                label = "To",
                selected = toCountry,
                countries = countries,
                modifier = Modifier.weight(1f),
                onSelected = { setCountries(fromCountry, it) }
            )
        }

        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))

        Text(
            "Select your ${fromDef?.displayName ?: fromCountry} size:",
            color = colors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Horizontal scrollable size chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableSizes) { size ->
                FilterChip(
                    selected = selectedSize == size,
                    onClick = { selectedSize = if (selectedSize == size) "" else size },
                    label = {
                        Text(
                            size,
                            fontSize = 15.sp,
                            fontWeight = if (selectedSize == size) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.equals,
                        selectedLabelColor = colors.equalsContent,
                        containerColor = colors.surface,
                        labelColor = colors.textPrimary
                    )
                )
            }
        }

        // Result card
        if (selectedSize.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                if (result != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FROM", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(selectedSize, color = colors.textPrimary, fontSize = 48.sp, fontWeight = FontWeight.Light)
                            Text(fromDef?.displayName ?: fromCountry, color = colors.textSecondary, fontSize = 13.sp)
                            if (fromDef?.unit?.isNotEmpty() == true)
                                Text(fromDef.unit, color = colors.textMuted, fontSize = 11.sp)
                        }
                        Text("→", color = colors.textMuted, fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TO", color = colors.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            Text(result, color = colors.toAmountColor, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                            Text(toDef?.displayName ?: toCountry, color = colors.textSecondary, fontSize = 13.sp)
                            if (toDef?.unit?.isNotEmpty() == true)
                                Text(toDef.unit, color = colors.textMuted, fontSize = 11.sp)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Size not available for this region",
                            color = colors.textMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Sizes are approximate and may vary between brands",
            color = colors.textMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SizeCountrySelector(
    label: String,
    selected: String,
    countries: List<SizeCountry>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    val selectedCountry = countries.find { it.key == selected }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.inputBorder),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontSize = 10.sp, color = colors.textMuted)
                Text(
                    selectedCountry?.displayName ?: selected,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            countries.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(country.displayName, color = colors.textPrimary)
                            if (country.unit.isNotEmpty()) {
                                Spacer(Modifier.width(4.dp))
                                Text("(${country.unit})", fontSize = 11.sp, color = colors.textMuted)
                            }
                        }
                    },
                    onClick = { onSelected(country.key); expanded = false }
                )
            }
        }
    }
}

@Composable
fun CalcButton(
    label: String,
    bg: Color,
    fg: Color = Color.White,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Button(
        onClick = {
            if (hapticEnabled) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(VibratorManager::class.java)).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            onClick()
        },
        modifier = modifier.fillMaxHeight(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Help screen ───────────────────────────────────────────────────────────────

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Help", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        HelpSection("Currency Converter") {
            HelpItem("Enter an amount", "Type a number on the keypad — the home-currency equivalent updates as you type. The entered amount is always treated as the local (From) currency.")
            HelpItem("Change currencies", "Tap either currency button to open the picker. Recent currencies appear at the top. Tap ⇄ to swap From and To.")
            HelpItem("Custom rate (★)", "A ★ next to the exchange rate means a custom or card-specific rate is active instead of the live rate.")
            HelpItem("Offline", "If rates can't be fetched, the last cached rates are used and an offline indicator is shown. Rates are refreshed every 24 hours.")
            HelpItem("Online", "Rates are from er-api.com and are refreshed every 24 hours or when the refresh button is pressed. (Visa and Mastercard rates are not officially possible - sorry!).")
        }

        HelpSection("Card Profiles") {
            HelpItem("What they are", "Named profiles for your payment cards, each with its own foreign transaction fee and optional exchange rate overrides.")
            HelpItem("Adding a card", "Go to Settings → Card Profiles → Add card. Set a name and a fee percentage (e.g. 2.75%).")
            HelpItem("Minimum fee", "Some cards charge a minimum fee (e.g. 3% or £3, whichever is greater). Set this in the card editor — when the minimum is higher than the percentage fee it is applied instead, and the rate label shows 'min. £3.00 GBP'.")
            HelpItem("Per-card rates", "Open the card editor and use the Custom exchange rates section to set rates for specific currencies. Useful when a card offers a fixed or non-live rate.")
            HelpItem("Rate order", "When a card is active, the rate used is: card-specific rate → global custom rate (if 'Use global custom rates' is enabled on the card) → live rate.")
            HelpItem("Selecting a card", "When cards exist, a chip row appears above the rate label on the Currency tab. Tap a chip to apply that card's fees and rates, or tap 'No card fee' to use the main calculator rates.")
        }

        HelpSection("Custom Exchange Rates") {
            HelpItem("Global overrides", "Settings → Exchange Rates → Add rate. Enter a base currency, target currency, and your rate. The ★ symbol appears whenever a custom rate is in use.")
            HelpItem("Format", "Rates are entered as '1 base = X target', e.g. 1 USD = 0.85 GBP. The live rate is pre-filled as a starting point.")
            HelpItem("Rate order (no card)", "Card-specific rate → global custom rate → live rate.")
            HelpItem("Cards & global rates", "By default, cards ignore global custom rates and only use their own rates or the live rate. Enable 'Use global custom rates as fallback' in the card editor to change this.")
        }

        HelpSection("Tip Calculator") {
            HelpItem("Basic use", "Enter the bill amount then select a tip percentage from the buttons. The tip and total update instantly.")
            HelpItem("Custom tip %", "The last tip button shows your custom percentage. Tap it once to select it, tap it again to edit the value. You can also set it in Settings.")
            HelpItem("Send to FX", "Tap 'Send total to FX' to pass the tipped total straight to the Currency Converter.")
            HelpItem("Default tip %", "Sets the tip percentage pre-selected when you open the Tip tab. Set in Settings → Default tip %")
        }

        HelpSection("Fuel Calculator") {
            HelpItem("Mode", "Use the mpg / L chip for petrol and diesel cars, or mi/kWh for electric vehicles.")
            HelpItem("mpg / L", "Converts between miles per gallon and litres per 100 km. Tap ⇄ to flip direction.")
            HelpItem("mi/kWh", "Converts between miles per kWh and kWh per 100 km — the two most common EV efficiency units.")
            HelpItem("UK vs US gallons", "Toggle between UK imperial gallons and US gallons in Settings → Fuel (mpg mode only).")
        }

        HelpSection("Distance Converter") {
            HelpItem("Converting", "Enter a distance in the From unit; the To unit updates live. Tap ⇄ to swap direction.")
            HelpItem("Additional Pairs", "Enable or disable additional units (miles↔km, inch↔cm, feet↔metre, SqFeet↔SqMetre) in Settings → Distance.")
        }

        HelpSection("Temperature Converter") {
            HelpItem("Units", "Converts between °C and °F. Tap ⇄ to swap direction.")
        }

        HelpSection("Size Converter") {
            HelpItem("Opening it", "Tap the menu (☰) → Size Converter.")
            HelpItem("Categories", "Switch between Shoes (men's or women's), Women's Clothing, and Men's Clothing using the tabs at the top.")
            HelpItem("From / To country", "Tap the country buttons to change. Your selections are remembered separately for each category.")
            HelpItem("Finding your size", "Tap a size in the From column — the matching To size is highlighted. Tap again to deselect.")
        }

        HelpSection("History") {
            HelpItem("Logging", "Every time you press = on the Currency tab, the conversion is saved (up to 50 entries).")
            HelpItem("Restoring", "Tap any history entry to restore the currencies and amount to the calculator.")
            HelpItem("Annotating", "Tap the 📝 icon on any history entry to add a note — useful for labelling what a conversion was for.")
            HelpItem("Opening it", "Tap the menu (☰) → History.")
        }

        HelpSection("Settings") {
            HelpItem("Appearance", "Choose light, dark, or system theme, and pick an accent colour.")
            HelpItem("Haptics", "Toggle vibration feedback on button presses.")
            HelpItem("Tab visibility", "Show or hide individual calculator tabs (Tip, Fuel, Distance, Temperature) to keep the tab bar uncluttered.")
            HelpItem("Default tip %", "Sets the tip percentage pre-selected when you open the Tip tab.")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Spacer(Modifier.height(8.dp))
    Text(title, color = colors.operator, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
    content()
    Spacer(Modifier.height(8.dp))
}

// ── OCR overlay ──────────────────────────────────────────────────────────────

@Composable
private fun OcrOverlayScreen(
    bitmap: Bitmap,
    uiState: CalculatorUiState,
    modifier: Modifier = Modifier,
    onUseAmount: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalAppColors.current
    var detections by remember { mutableStateOf<List<DetectedNumber>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var lastTappedValue by remember { mutableStateOf<String?>(null) }

    BackHandler { onClose() }

    LaunchedEffect(bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { result ->
                val currencyRe = Regex("[£\$€¥₹₩₪฿₫₦]")
                val found = mutableListOf<DetectedNumber>()
                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val box = element.boundingBox ?: continue
                            val clean = element.text.replace(currencyRe, "").replace(",", "").trim()
                            val value = clean.toDoubleOrNull() ?: continue
                            if (value <= 0) continue
                            found.add(DetectedNumber(rawText = element.text, value = value, imageBox = box))
                        }
                    }
                }
                detections = found
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    val exchangeRate = uiState.exchangeRate
    val fromCurrency = uiState.fromCurrency
    val toCurrency = uiState.toCurrency
    val selectedItems = detections.filter { it.selected }
    val selectedTotal = selectedItems.sumOf { it.value }

    Column(modifier = modifier.background(Color.Black)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xCC000000)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            Text("$fromCurrency → $toCurrency", color = Color.White, fontSize = 15.sp,
                fontWeight = FontWeight.Medium)
            if (exchangeRate != null) {
                Text("  (1 = ${"%.4f".format(exchangeRate)})", color = Color.White.copy(0.55f), fontSize = 11.sp)
            }
        }

        // Image + overlay
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerW = constraints.maxWidth.toFloat()
                    val containerH = constraints.maxHeight.toFloat()
                    val imgW = bitmap.width.toFloat()
                    val imgH = bitmap.height.toFloat()
                    val scale = min(containerW / imgW, containerH / imgH)
                    val offsetX = (containerW - imgW * scale) / 2f
                    val offsetY = (containerH - imgH * scale) / 2f

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bounding box outlines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (det in detections) {
                            val r = det.imageBox
                            drawRoundRect(
                                color = if (det.selected) Color(0xFFFFD700) else Color.White.copy(0.65f),
                                topLeft = Offset(r.left * scale + offsetX, r.top * scale + offsetY),
                                size = Size(r.width() * scale, r.height() * scale),
                                cornerRadius = CornerRadius(4.dp.toPx()),
                                style = Stroke(width = if (det.selected) 3.dp.toPx() else 1.5f.dp.toPx())
                            )
                        }
                    }

                    // Converted value bubbles for selected items
                    if (exchangeRate != null) {
                        for (det in selectedItems) {
                            val r = det.imageBox
                            val bubbleX = (r.left * scale + offsetX).roundToInt()
                            val bubbleY = (r.bottom * scale + offsetY + 4).roundToInt()
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(bubbleX, bubbleY) }
                                    .background(Color(0xDD000000), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "$toCurrency ${"%.2f".format(det.value * exchangeRate)}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Tap handler
                    Box(modifier = Modifier.fillMaxSize().pointerInput(detections) {
                        detectTapGestures { tap ->
                            val imgX = ((tap.x - offsetX) / scale).toInt()
                            val imgY = ((tap.y - offsetY) / scale).toInt()
                            var hitIndex = -1
                            detections.forEachIndexed { i, det ->
                                if (hitIndex < 0 && det.imageBox.contains(imgX, imgY)) hitIndex = i
                            }
                            if (hitIndex >= 0) {
                                detections = detections.mapIndexed { i, det ->
                                    if (i == hitIndex) {
                                        val newSelected = !det.selected
                                        if (newSelected) {
                                            lastTappedValue = if (det.value == kotlin.math.floor(det.value))
                                                det.value.toLong().toString()
                                            else "%.2f".format(det.value)
                                        }
                                        det.copy(selected = newSelected)
                                    } else det
                                }
                            }
                        }
                    })

                    if (!isLoading && detections.isEmpty()) {
                        Text(
                            "No numbers detected\nTry a clearer photo",
                            color = Color.White.copy(0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp)
                        )
                    }
                }
            }
        }

        // Bottom bar — only shown when items are selected
        if (selectedItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEE000000))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Selected: $fromCurrency ${"%.2f".format(selectedTotal)}",
                        color = Color.White, fontSize = 12.sp
                    )
                    if (exchangeRate != null) {
                        Text(
                            "= $toCurrency ${"%.2f".format(selectedTotal * exchangeRate)}",
                            color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                val useVal = lastTappedValue
                if (useVal != null) {
                    Button(
                        onClick = { onUseAmount(useVal) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.equals)
                    ) {
                        Text("Use $useVal", color = colors.equalsContent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpItem(label: String, detail: String) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(130.dp))
        Text(detail, color = colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}
