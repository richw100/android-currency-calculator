package com.tripcalc.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import kotlin.math.roundToInt
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: CalculatorViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    var showDialog by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf("") }
    var showCardDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CardProfile?>(null) }
    var customTipInput by remember(state.customTipPercent) {
        mutableStateOf(
            if (state.customTipPercent == state.customTipPercent.toLong().toDouble())
                state.customTipPercent.toLong().toString()
            else "%.1f".format(state.customTipPercent)
        )
    }
    val customTipValue = customTipInput.toDoubleOrNull()
    val customTipValid = customTipValue != null && customTipValue > 0 && customTipValue <= 100

    if (showCardDialog) {
        val liveCard = editingCard?.let { c -> state.cardProfiles.find { it.id == c.id } } ?: editingCard
        CardProfileDialog(
            existing = liveCard,
            availableCurrencies = state.availableCurrencies,
            defaultCurrency = state.toCurrency,
            liveRates = state.liveRates,
            defaultRateTarget = state.fromCurrency,
            onSave = { name, pct, minFee, minCur ->
                if (editingCard == null) vm.onAction(CalculatorAction.AddCardProfile(name, pct, minFee, minCur))
                else vm.onAction(CalculatorAction.UpdateCardProfile(editingCard!!.id, name, pct, minFee, minCur))
                showCardDialog = false; editingCard = null
            },
            onRateSet = { target, base, rate -> editingCard?.let { vm.onAction(CalculatorAction.SetCardCustomRate(it.id, target, base, rate)) } },
            onRateDelete = { target -> editingCard?.let { vm.onAction(CalculatorAction.DeleteCardCustomRate(it.id, target)) } },
            onDismiss = { showCardDialog = false; editingCard = null }
        )
    }

    if (showDialog) {
        val existing = state.customRates[editingTarget]
        CustomRateDialog(
            initialTarget = editingTarget,
            initialBase = existing?.base ?: state.fromCurrency,
            initialRate = existing?.rate?.let { "%.4f".format(it) } ?: "",
            defaultTarget = state.toCurrency,
            availableCurrencies = state.availableCurrencies,
            liveRates = state.liveRates,
            onSave = { base, target, rate ->
                vm.onAction(CalculatorAction.SetCustomRate(target, base, rate))
                showDialog = false
                editingTarget = ""
            },
            onDismiss = { showDialog = false; editingTarget = "" }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Custom exchange rates ─────────────────────────────────────────────
        Text("CUSTOM EXCHANGE RATES", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        Text(
            "Override any live rate with your own value. Useful for locked-in travel money or your bank's specific rate.",
            fontSize = 13.sp, color = colors.textMuted, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.customRates.isEmpty()) {
            Text("No custom rates set.", fontSize = 14.sp, color = colors.textMuted, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            state.customRates.entries.sortedBy { it.key }.forEach { (target, entry) ->
                val targetName = currencyName(target)
                val livePairRate = run {
                    val b = state.liveRates[entry.base] ?: return@run null
                    val t = state.liveRates[target] ?: return@run null
                    if (b == 0.0) null else t / b
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFlag(entry.base), fontSize = 20.sp)
                                Text(" ${entry.base} → ", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(currencyFlag(target), fontSize = 20.sp)
                                Text(" $target", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("1 ${entry.base} = ${"%.4f".format(entry.rate)} $target", color = colors.warningColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (livePairRate != null) {
                                Text("Live: ${"%.4f".format(livePairRate)}", color = colors.textMuted, fontSize = 11.sp)
                            }
                            Text(targetName, color = colors.textMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { editingTarget = target; showDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { vm.onAction(CalculatorAction.ClearCustomRate(target)) }) {
                            Icon(Icons.Default.Close, "Remove", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { editingTarget = ""; showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.fromAmountColor),
            border = BorderStroke(1.dp, colors.fromAmountColor)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add custom rate", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))

        // ── Card profiles ─────────────────────────────────────────────────────
        Text("CARD PROFILES", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        Text(
            "Save each card's foreign transaction fee. Select a card on the calculator to include its fee in conversions.",
            fontSize = 13.sp, color = colors.textMuted, lineHeight = 19.sp, modifier = Modifier.padding(bottom = 12.dp)
        )

        state.cardProfiles.forEach { card ->
            val pctPart = if (card.markupPercent == 0.0) "0%" else "${"%.2f".format(card.markupPercent).trimEnd('0').trimEnd('.')}%"
            val minPart = if (card.minFeeAmount > 0 && card.minFeeCurrency.isNotEmpty())
                " · min. ${"%.2f".format(card.minFeeAmount).trimEnd('0').trimEnd('.')} ${card.minFeeCurrency}" else ""
            val ratesPart = if (card.customRates.isNotEmpty()) " · ${card.customRates.size} custom rate${if (card.customRates.size == 1) "" else "s"}" else ""
            val feeText = "$pctPart fee$minPart$ratesPart"
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CreditCard, null, tint = colors.operator, modifier = Modifier.size(20.dp).padding(end = 0.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(card.name, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(feeText, color = if (card.markupPercent > 0 || card.minFeeAmount > 0) colors.warningColor else colors.positiveColor, fontSize = 12.sp)
                    }
                    IconButton(onClick = { editingCard = card; showCardDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { vm.onAction(CalculatorAction.DeleteCardProfile(card.id)) }) {
                        Icon(Icons.Default.Close, "Delete", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { editingCard = null; showCardDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.operator),
            border = BorderStroke(1.dp, colors.operator)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add card", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exchange rates", color = colors.textPrimary, fontSize = 15.sp)
                    Text(
                        text = if (state.isOffline) "Offline — using cached rates" else state.lastUpdated,
                        color = if (state.isOffline) colors.warningColor else colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "${state.availableCurrencies.size} currencies",
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Appearance ──────────────────────────────────────────────────────
        Text("APPEARANCE", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Appearance mode", color = colors.textPrimary, fontSize = 15.sp)
                Text("Override system dark/light setting", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DarkModePref.values().forEachIndexed { index, pref ->
                        SegmentedButton(
                            selected = state.darkModePref == pref,
                            onClick = { vm.onAction(CalculatorAction.SetDarkMode(pref)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = DarkModePref.values().size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor   = colors.operator,
                                activeContentColor     = colors.operatorContent,
                                inactiveContainerColor = colors.buttonDigit,
                                inactiveContentColor   = colors.textPrimary,
                                activeBorderColor      = colors.operator,
                                inactiveBorderColor    = colors.divider
                            )
                        ) {
                            Text(pref.label, fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = colors.divider)

                Text("Accent colour", color = colors.textPrimary, fontSize = 15.sp)
                Text("Button colour scheme", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AccentScheme.values().forEach { scheme ->
                        val isSelected = state.accentScheme == scheme
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(scheme.swatch)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, colors.textPrimary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { vm.onAction(CalculatorAction.SetAccentScheme(scheme)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(scheme.label, fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── General ──────────────────────────────────────────────────────────
        Text("GENERAL", fontSize = 12.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Haptic feedback", color = colors.textPrimary, fontSize = 15.sp)
                    Text("Vibrate on button tap", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = state.hapticEnabled,
                    onCheckedChange = { vm.onAction(CalculatorAction.SetHaptic(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.positiveColor)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Swap 0 and . keys", color = colors.textPrimary, fontSize = 15.sp)
                    Text("Put zero before decimal point in bottom row", color = colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = state.swapZeroDot,
                    onCheckedChange = { vm.onAction(CalculatorAction.SetSwapZeroDot(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.positiveColor)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Active converters", color = colors.textPrimary, fontSize = 15.sp)
                Text("Choose which modes appear in the tab bar", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                ConversionMode.entries.forEach { mode ->
                    val isEnabled = mode in state.enabledModes
                    val isCurrency = mode == ConversionMode.CURRENCY
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.tabLabel, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            mode.settingsLabel,
                            color = if (isCurrency) colors.textMuted else colors.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isEnabled,
                            enabled = !isCurrency,
                            onCheckedChange = { checked ->
                                val newModes = if (checked) state.enabledModes + mode else state.enabledModes - mode
                                vm.onAction(CalculatorAction.SetEnabledModes(newModes))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.positiveColor)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Distance unit pairs", color = colors.textPrimary, fontSize = 15.sp)
                Text("Choose which conversions appear in the Distance tab", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                DistancePair.entries.forEach { dp ->
                    val isEnabled = dp in state.enabledDistancePairs
                    val isMilesKm = dp == DistancePair.MILES_KM
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dp.settingsLabel,
                            color = if (isMilesKm) colors.textMuted else colors.textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isEnabled,
                            enabled = !isMilesKm,
                            onCheckedChange = { checked ->
                                val newPairs = if (checked) state.enabledDistancePairs + dp else state.enabledDistancePairs - dp
                                vm.onAction(CalculatorAction.SetEnabledDistancePairs(newPairs))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.positiveColor)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Default tip %", color = colors.textPrimary, fontSize = 15.sp)
                Text("Applied when you open the Tip calculator", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10.0, 15.0, 20.0).forEach { pct ->
                        FilterChip(
                            selected = state.defaultTipPercent == pct,
                            onClick = { vm.onAction(CalculatorAction.SetDefaultTipPercent(pct)) },
                            label = { Text("${pct.toInt()}%", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.buttonDigit,
                                labelColor = colors.textPrimary,
                                selectedContainerColor = colors.positiveColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Custom tip %", color = colors.textPrimary, fontSize = 15.sp)
                Text("Shown as a 4th option in the tip calculator", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customTipInput,
                        onValueChange = { customTipInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        suffix = { Text("%", color = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                    TextButton(
                        onClick = { customTipValue?.let { vm.onAction(CalculatorAction.SetCustomTipPercent(it)) } },
                        enabled = customTipValid && customTipValue != state.customTipPercent
                    ) {
                        Text("Set", color = if (customTipValid && customTipValue != state.customTipPercent) colors.fromAmountColor else colors.textMuted)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Fuel economy — gallons", color = colors.textPrimary, fontSize = 15.sp)
                Text("Which gallon size to use for mpg conversions", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to "UK (Imperial)", false to "US").forEach { (isUk, label) ->
                        FilterChip(
                            selected = state.fuelUseUkGallons == isUk,
                            onClick = { vm.onAction(CalculatorAction.SetFuelUseUkGallons(isUk)) },
                            label = { Text(label, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.buttonDigit,
                                labelColor = colors.textPrimary,
                                selectedContainerColor = colors.positiveColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

        }

    }
}

@Composable
private fun CardProfileDialog(
    existing: CardProfile?,
    availableCurrencies: List<String>,
    defaultCurrency: String,
    liveRates: Map<String, Double>,
    defaultRateTarget: String,
    onSave: (name: String, markupPercent: Double, minFeeAmount: Double, minFeeCurrency: String) -> Unit,
    onRateSet: (target: String, base: String, rate: Double) -> Unit,
    onRateDelete: (target: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var markup by remember(existing?.id) { mutableStateOf(existing?.markupPercent?.toFloat() ?: 0f) }
    var minFeeInput by remember(existing?.id) { mutableStateOf(
        if ((existing?.minFeeAmount ?: 0.0) > 0) "%.2f".format(existing!!.minFeeAmount).trimEnd('0').trimEnd('.') else ""
    ) }
    var minFeeCurrency by remember(existing?.id, defaultCurrency) { mutableStateOf(
        existing?.minFeeCurrency?.takeIf { it.isNotEmpty() } ?: defaultCurrency
    ) }
    var showAddRate by remember { mutableStateOf(false) }
    var editingRateTarget by remember { mutableStateOf("") }

    val snappedMarkup = ((markup / 0.25f).roundToInt() * 0.25).coerceIn(0.0, 5.0)
    val markupLabel = if (snappedMarkup == 0.0) "Off (0%)" else "${"%.2f".format(snappedMarkup).trimEnd('0').trimEnd('.')}%"
    val minFeeAmount = minFeeInput.toDoubleOrNull() ?: 0.0

    if (showAddRate && existing != null) {
        val existingRate = existing.customRates[editingRateTarget]
        CustomRateDialog(
            initialTarget = editingRateTarget,
            initialBase = existingRate?.base ?: defaultCurrency,
            initialRate = if (existingRate != null) "%.4f".format(existingRate.rate).trimEnd('0').trimEnd('.') else "",
            defaultTarget = defaultRateTarget,
            availableCurrencies = availableCurrencies,
            liveRates = liveRates,
            onSave = { base, target, rate -> onRateSet(target, base, rate); showAddRate = false; editingRateTarget = "" },
            onDismiss = { showAddRate = false; editingRateTarget = "" }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (existing == null) "Add card" else "Edit card",
                    color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card name", fontSize = 13.sp) },
                    placeholder = { Text("e.g. Starling, Wise, Chase", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Foreign transaction fee", color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(markupLabel, color = if (snappedMarkup > 0) colors.warningColor else colors.textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Slider(
                    value = markup,
                    onValueChange = { markup = it },
                    valueRange = 0f..5f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = colors.operator, activeTrackColor = colors.operator, inactiveTrackColor = colors.divider)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0% (off)", fontSize = 11.sp, color = colors.textMuted)
                    Text("5%", fontSize = 11.sp, color = colors.textMuted)
                }
                Spacer(Modifier.height(16.dp))
                Text("Minimum fee", color = colors.textPrimary, fontSize = 14.sp)
                Text("Applied when the % fee is lower (e.g. 3% or min. £3)", color = colors.textMuted, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = minFeeInput,
                    onValueChange = { v -> if (v.matches(Regex("""^\d{0,6}(\.\d{0,2})?$"""))) minFeeInput = v },
                    label = { Text("Amount (0 = off)", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text("Currency", fontSize = 12.sp, color = colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                CurrencyDropdown(
                    code = minFeeCurrency,
                    availableCurrencies = availableCurrencies,
                    onSelected = { minFeeCurrency = it }
                )

                if (existing != null) {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(12.dp))
                    Text("Custom exchange rates", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Override the live rate for specific currencies. Falls back to live rate if not set.", color = colors.textMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    if (existing.customRates.isEmpty()) {
                        Text("No custom rates yet.", color = colors.textMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        existing.customRates.forEach { (target, entry) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "1 ${entry.base} = ${"%.4f".format(entry.rate).trimEnd('0').trimEnd('.')} $target",
                                    color = colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { editingRateTarget = target; showAddRate = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                }
                                IconButton(onClick = { onRateDelete(target) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Delete", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { editingRateTarget = ""; showAddRate = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.operator),
                        border = BorderStroke(1.dp, colors.operator)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add rate", fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { if (name.isNotBlank()) onSave(name.trim(), snappedMarkup, minFeeAmount, if (minFeeAmount > 0) minFeeCurrency else "") },
                        enabled = name.isNotBlank()
                    ) { Text("Save", color = colors.fromAmountColor) }
                }
            }
        }
    }
}

@Composable
private fun CustomRateDialog(
    initialTarget: String,
    initialBase: String,
    initialRate: String,
    defaultTarget: String,
    availableCurrencies: List<String>,
    liveRates: Map<String, Double>,
    onSave: (base: String, target: String, rate: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val isEditing = initialTarget.isNotEmpty()
    var baseCode by remember { mutableStateOf(initialBase) }
    var targetCode by remember { mutableStateOf(if (isEditing) initialTarget else defaultTarget) }
    var rateInput by remember { mutableStateOf(initialRate) }

    LaunchedEffect(baseCode, targetCode) {
        if (rateInput.isEmpty() && baseCode.isNotEmpty() && targetCode.isNotEmpty() && baseCode != targetCode) {
            val b = liveRates[baseCode]
            val t = liveRates[targetCode]
            if (b != null && t != null && b > 0) rateInput = "%.4f".format(t / b)
        }
    }

    val livePairRate = remember(baseCode, targetCode, liveRates) {
        val b = liveRates[baseCode] ?: return@remember null
        val t = liveRates[targetCode] ?: return@remember null
        if (b == 0.0) null else t / b
    }

    val saveEnabled = rateInput.toDoubleOrNull()?.let { it > 0 } == true
        && baseCode.isNotEmpty() && targetCode.isNotEmpty() && baseCode != targetCode

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(if (isEditing) "Edit Custom Rate" else "Add Custom Rate", color = colors.textPrimary) },
        text = {
            Column {
                Text("From (base)", fontSize = 12.sp, color = colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                CurrencyDropdown(code = baseCode, availableCurrencies = availableCurrencies, onSelected = { baseCode = it })

                Spacer(Modifier.height(12.dp))

                Text("To (converted)", fontSize = 12.sp, color = colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                CurrencyDropdown(code = targetCode, availableCurrencies = availableCurrencies, enabled = !isEditing, onSelected = { targetCode = it })

                if (baseCode.isNotEmpty() && targetCode.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Rate", fontSize = 12.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1 $baseCode =", color = colors.textPrimary, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(targetCode, color = colors.textPrimary, fontSize = 14.sp)
                    }
                    if (livePairRate != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Live: 1 $baseCode = ${"%.4f".format(livePairRate)} $targetCode", fontSize = 11.sp, color = colors.textMuted)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { val r = rateInput.toDoubleOrNull(); if (r != null && r > 0) onSave(baseCode, targetCode, r) },
                enabled = saveEnabled
            ) { Text("Save", color = colors.fromAmountColor) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textSecondary) } }
    )
}

@Composable
private fun CurrencyDropdown(
    code: String,
    availableCurrencies: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(search, availableCurrencies) {
        availableCurrencies.filter { c ->
            search.isEmpty() || c.contains(search, true) || currencyName(c).contains(search, true)
        }
    }

    OutlinedButton(
        onClick = { if (enabled) { expanded = true; search = "" } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
        border = BorderStroke(1.dp, if (enabled) colors.inputBorder else colors.textMuted),
        enabled = enabled
    ) {
        Text(if (code.isEmpty()) "Select currency…" else "${currencyFlag(code)} $code", fontSize = 14.sp)
    }

    if (expanded) {
        Dialog(onDismissRequest = { expanded = false; search = "" }) {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search…", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(filtered, key = { it }) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelected(c); expanded = false; search = "" }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currencyFlag(c), fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(c, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(currencyName(c), fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
