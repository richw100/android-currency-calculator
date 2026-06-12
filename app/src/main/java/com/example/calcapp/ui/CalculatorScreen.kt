package com.example.calcapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import java.util.Currency as JavaCurrency
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calcapp.R
import com.example.calcapp.ui.theme.CalcAppTheme

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

// ── Root composable ──────────────────────────────────────────────────────────

private enum class Screen { Calculator, History, Settings, About }

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

    CalcAppTheme(darkTheme = effectiveIsDark) {
        CompositionLocalProvider(LocalAppColors provides colors) {
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
                        text = "Currency Calculator",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (screen == Screen.Calculator) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.textPrimary)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
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

        Text(text = "Currency Calc", color = colors.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(text = "Version 1.0", color = colors.textSecondary, fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))

        Text(
            text = "A simple, ad-free calculator with live dual-currency conversion. " +
                    "Enter any amount and see the equivalent in your chosen currency pair as you type.",
            color = colors.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(vm: CalculatorViewModel = viewModel(), modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showRefreshDialog by remember { mutableStateOf(false) }
    var warningCurrency by remember { mutableStateOf("") }
    var displayMenuExpanded by remember { mutableStateOf(false) }

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
        // Currency selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Swap currencies", tint = colors.textSecondary)
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
        }

        // Exchange rate info + refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (state.exchangeRateLabel.isNotEmpty()) {
                    Text(text = state.exchangeRateLabel, color = colors.textSecondary, fontSize = 12.sp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isOffline) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline",
                            tint = colors.warningColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = state.lastUpdated,
                        color = if (state.isOffline) colors.warningColor else colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
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
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh rates", tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Currency conversion display
        val shareText = "${currencyFlag(state.fromCurrency)} ${state.fromAmount} = ${currencyFlag(state.toCurrency)} ${state.toAmount}"
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            CopyableAmount(text = "${currencyFlag(state.fromCurrency)} ${state.fromAmount}", color = colors.fromAmountColor, shareText = shareText)
            CopyableAmount(text = "${currencyFlag(state.toCurrency)} ${state.toAmount}", color = colors.toAmountColor, shareText = shareText)
        }

        // Expression line
        if (state.expression.isNotEmpty()) {
            Text(
                text = state.expression,
                color = colors.textSecondary,
                fontSize = 26.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                textAlign = TextAlign.End
            )
        }

        // Main display
        Box {
            Text(
                text = if (state.isError) "Error" else state.display,
                color = colors.textPrimary,
                fontSize = 72.sp,
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
                                        putExtra(Intent.EXTRA_TEXT, "${state.display} ${state.fromCurrency} = ${state.toAmount}")
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

        // Button grid
        ButtonGrid(
            onAction = vm::onAction,
            hapticEnabled = state.hapticEnabled,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rww_100"))) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.promoColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.promoColor)
        ) {
            Text(text = "Like using this ad-free app? Buy me a coffee to say thanks! ☕", fontSize = 11.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(4.dp))
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

    fun matches(code: String): Boolean {
        if (search.isEmpty()) return true
        val name = try { JavaCurrency.getInstance(code).displayName } catch (e: Exception) { "" }
        return code.contains(search, ignoreCase = true) || name.contains(search, ignoreCase = true)
    }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true; search = "" },
            modifier = Modifier.fillMaxWidth(),
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search...", fontSize = 13.sp) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                singleLine = true
            )
            val filteredRecents = recentCurrencies.filter { matches(it) }
            if (filteredRecents.isNotEmpty()) {
                Text(text = "RECENT", fontSize = 10.sp, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                filteredRecents.forEach { code ->
                    val name = try { JavaCurrency.getInstance(code).displayName } catch (e: Exception) { code }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFlag(code), fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(code, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(name, fontSize = 11.sp, color = colors.textSecondary)
                                }
                                if (customRates.containsKey(code)) Text("★", color = colors.warningColor, fontSize = 12.sp)
                            }
                        },
                        onClick = { onSelected(code); expanded = false }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            val recentSet = recentCurrencies.toSet()
            available.filter { it !in recentSet && matches(it) }.forEach { code ->
                val name = try { JavaCurrency.getInstance(code).displayName } catch (e: Exception) { code }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currencyFlag(code), fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(code, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(name, fontSize = 11.sp, color = colors.textSecondary)
                            }
                            if (customRates.containsKey(code)) Text("★", color = colors.warningColor, fontSize = 12.sp)
                        }
                    },
                    onClick = { onSelected(code); expanded = false }
                )
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
fun ButtonGrid(onAction: (CalculatorAction) -> Unit, hapticEnabled: Boolean = true, modifier: Modifier = Modifier) {
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

    Column(modifier = modifier) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { btn ->
                    CalcButton(
                        label = btn.label, bg = btn.bg, fg = btn.fg,
                        hapticEnabled = hapticEnabled,
                        onClick = { onAction(btn.action) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Bottom row: 0, ., ⌫, =
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcButton("0", colors.buttonDigit, fg = colors.buttonDigitContent, hapticEnabled = hapticEnabled, onClick = { onAction(CalculatorAction.Digit(0)) },   modifier = Modifier.weight(1f))
            CalcButton(".", colors.buttonDigit, fg = colors.buttonDigitContent, hapticEnabled = hapticEnabled, onClick = { onAction(CalculatorAction.Decimal) },    modifier = Modifier.weight(1f))
            CalcButton("⌫", colors.buttonDigit, fg = colors.buttonDigitContent, hapticEnabled = hapticEnabled, onClick = { onAction(CalculatorAction.Delete) },     modifier = Modifier.weight(1f))
            CalcButton("=", colors.equals,      fg = colors.equalsContent,      hapticEnabled = hapticEnabled, onClick = { onAction(CalculatorAction.Calculate) },  modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
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
        modifier = modifier.height(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}
