package com.example.calcapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Currency as JavaCurrency

@Composable
fun SettingsScreen(vm: CalculatorViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf("") }

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
        Text(
            text = "GENERAL",
            fontSize = 12.sp,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Haptic feedback", color = Color.White, fontSize = 15.sp)
                    Text("Vibrate on button tap", color = Color(0xFF8E8E93), fontSize = 12.sp)
                }
                Switch(
                    checked = state.hapticEnabled,
                    onCheckedChange = { vm.onAction(CalculatorAction.SetHaptic(it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "CUSTOM EXCHANGE RATES",
            fontSize = 12.sp,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Override any live rate with your own value. Useful for locked-in travel money or your bank's specific rate.",
            fontSize = 13.sp,
            color = Color(0xFFAEAEB2),
            lineHeight = 19.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.customRates.isEmpty()) {
            Text(
                text = "No custom rates set.",
                fontSize = 14.sp,
                color = Color(0xFF636366),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            state.customRates.entries.sortedBy { it.key }.forEach { (target, entry) ->
                val targetName = try { JavaCurrency.getInstance(target).displayName } catch (e: Exception) { target }
                val livePairRate = run {
                    val b = state.liveRates[entry.base] ?: return@run null
                    val t = state.liveRates[target] ?: return@run null
                    if (b == 0.0) null else t / b
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFlag(entry.base), fontSize = 20.sp)
                                Text(
                                    " ${entry.base} → ",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(currencyFlag(target), fontSize = 20.sp)
                                Text(
                                    " $target",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "1 ${entry.base} = ${"%.4f".format(entry.rate)} $target",
                                color = Color(0xFFFF9F0A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (livePairRate != null) {
                                Text(
                                    "Live: ${"%.4f".format(livePairRate)}",
                                    color = Color(0xFF636366),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                targetName,
                                color = Color(0xFF636366),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { editingTarget = target; showDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { vm.onAction(CalculatorAction.ClearCustomRate(target)) }) {
                            Icon(Icons.Default.Close, "Remove", tint = Color(0xFF8E8E93), modifier = Modifier.size(18.dp))
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0A84FF)),
            border = BorderStroke(1.dp, Color(0xFF0A84FF))
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add custom rate", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
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
    val isEditing = initialTarget.isNotEmpty()
    var baseCode by remember { mutableStateOf(initialBase) }
    var targetCode by remember { mutableStateOf(if (isEditing) initialTarget else defaultTarget) }
    var rateInput by remember { mutableStateOf(initialRate) }

    // When both currencies are set and rate is empty, pre-fill with live pair rate
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
        containerColor = Color(0xFF1C1C1E),
        title = {
            Text(if (isEditing) "Edit Custom Rate" else "Add Custom Rate", color = Color.White)
        },
        text = {
            Column {
                Text("From (base)", fontSize = 12.sp, color = Color(0xFF8E8E93))
                Spacer(Modifier.height(4.dp))
                CurrencyDropdown(
                    code = baseCode,
                    availableCurrencies = availableCurrencies,
                    onSelected = { baseCode = it }
                )

                Spacer(Modifier.height(12.dp))

                Text("To (converted)", fontSize = 12.sp, color = Color(0xFF8E8E93))
                Spacer(Modifier.height(4.dp))
                CurrencyDropdown(
                    code = targetCode,
                    availableCurrencies = availableCurrencies,
                    enabled = !isEditing,
                    onSelected = { targetCode = it }
                )

                if (baseCode.isNotEmpty() && targetCode.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Rate", fontSize = 12.sp, color = Color(0xFF8E8E93))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1 $baseCode =", color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = { rateInput = it },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(targetCode, color = Color.White, fontSize = 14.sp)
                    }
                    if (livePairRate != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Live: 1 $baseCode = ${"%.4f".format(livePairRate)} $targetCode",
                            fontSize = 11.sp,
                            color = Color(0xFF636366)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val r = rateInput.toDoubleOrNull()
                    if (r != null && r > 0) onSave(baseCode, targetCode, r)
                },
                enabled = saveEnabled
            ) {
                Text("Save", color = Color(0xFF0A84FF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8E8E93))
            }
        }
    )
}

@Composable
private fun CurrencyDropdown(
    code: String,
    availableCurrencies: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    Box {
        OutlinedButton(
            onClick = { if (enabled) { expanded = true; search = "" } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, if (enabled) Color(0xFF48484A) else Color(0xFF38383A)),
            enabled = enabled
        ) {
            Text(
                if (code.isEmpty()) "Select currency…" else "${currencyFlag(code)} $code",
                fontSize = 14.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search…", fontSize = 13.sp) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                singleLine = true
            )
            availableCurrencies
                .filter { c ->
                    val name = try { JavaCurrency.getInstance(c).displayName } catch (e: Exception) { "" }
                    search.isEmpty() || c.contains(search, true) || name.contains(search, true)
                }
                .forEach { c ->
                    val name = try { JavaCurrency.getInstance(c).displayName } catch (e: Exception) { c }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFlag(c), fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(c, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(name, fontSize = 11.sp, color = Color(0xFF8E8E93))
                                }
                            }
                        },
                        onClick = { onSelected(c); expanded = false; search = "" }
                    )
                }
        }
    }
}
