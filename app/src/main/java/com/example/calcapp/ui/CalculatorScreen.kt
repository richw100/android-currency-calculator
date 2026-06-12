package com.example.calcapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import java.util.Currency as JavaCurrency
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calcapp.R

private val BgColor = Color(0xFF1C1C1E)
private val ButtonDark = Color(0xFF333333)
private val ButtonLight = Color(0xFFA5A5A5)
private val ButtonOrange = Color(0xFFFF9F0A)

private fun currencyFlag(code: String): String {
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

@Composable
fun AppScreen() {
    var showAbout by remember { mutableStateOf(false) }
    val vm: CalculatorViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
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
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showAbout = !showAbout }) {
                Icon(
                    imageVector = if (showAbout) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = if (showAbout) "Back to calculator" else "About",
                    tint = Color.White
                )
            }
        }

        HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 1.dp)

        if (showAbout) {
            AboutScreen(modifier = Modifier.weight(1f))
        } else {
            CalculatorScreen(vm = vm, modifier = Modifier.weight(1f))
        }
    }
}

// ── About screen ─────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

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

        Text(
            text = "Currency Calc",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Version 1.0",
            color = Color(0xFF8E8E93),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "A simple, ad-free calculator with live dual-currency conversion. " +
                    "Enter any amount and see the equivalent in your chosen currency pair as you type.",
            color = Color(0xFFAEAEB2),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Exchange rates from open.er-api.com\nUpdated every 24 hours · 161 currencies",
            color = Color(0xFF636366),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rww_100"))
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9F0A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9F0A))
        ) {
            Text("Buy me a coffee ☕", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Calculator screen ─────────────────────────────────────────────────────────

@Composable
fun CalculatorScreen(vm: CalculatorViewModel = viewModel(), modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                onSelected = { vm.onAction(CalculatorAction.SetFromCurrency(it)) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.onAction(CalculatorAction.SwapCurrencies) }) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap currencies",
                    tint = Color(0xFF8E8E93)
                )
            }
            CurrencySelector(
                label = "To",
                selected = state.toCurrency,
                available = state.availableCurrencies,
                recentCurrencies = state.recentCurrencies,
                onSelected = { vm.onAction(CalculatorAction.SetToCurrency(it)) },
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
                    Text(
                        text = state.exchangeRateLabel,
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = state.lastUpdated,
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = { vm.onAction(CalculatorAction.RefreshRates) },
                enabled = !state.isRefreshing
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF8E8E93),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh rates",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Currency conversion display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${currencyFlag(state.fromCurrency)} ${state.fromAmount}",
                color = Color(0xFF0A84FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${currencyFlag(state.toCurrency)} ${state.toAmount}",
                color = Color(0xFF34C759),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Expression line
        if (state.expression.isNotEmpty()) {
            Text(
                text = state.expression,
                color = Color(0xFF8E8E93),
                fontSize = 26.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.End
            )
        }

        // Main display
        Text(
            text = if (state.isError) "Error" else state.display,
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Button grid
        ButtonGrid(
            onAction = vm::onAction,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/rww_100"))
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9F0A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9F0A))
        ) {
            Text(
                text = "Like using this ad-free app? Buy me a coffee to say thanks! ☕",
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
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
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF48484A)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("$label: ${currencyFlag(selected)} $selected", fontSize = 13.sp, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search...", fontSize = 13.sp) },
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                singleLine = true
            )

            // Recent currencies section
            val filteredRecents = recentCurrencies.filter { matches(it) }
            if (filteredRecents.isNotEmpty()) {
                Text(
                    text = "RECENT",
                    fontSize = 10.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                filteredRecents.forEach { code ->
                    val name = try { JavaCurrency.getInstance(code).displayName } catch (e: Exception) { code }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFlag(code), fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(code, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(name, fontSize = 11.sp, color = Color(0xFF8E8E93))
                                }
                            }
                        },
                        onClick = { onSelected(code); expanded = false }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // Full sorted list (excluding recents to avoid duplication)
            val recentSet = recentCurrencies.toSet()
            available
                .filter { it !in recentSet && matches(it) }
                .forEach { code ->
                    val name = try { JavaCurrency.getInstance(code).displayName } catch (e: Exception) { code }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFlag(code), fontSize = 22.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(code, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(name, fontSize = 11.sp, color = Color(0xFF8E8E93))
                                }
                            }
                        },
                        onClick = { onSelected(code); expanded = false }
                    )
                }
        }
    }
}

// ── Button grid ───────────────────────────────────────────────────────────────

private data class BtnDef(
    val label: String,
    val bg: Color,
    val fg: Color = Color.White,
    val action: CalculatorAction
)

@Composable
fun ButtonGrid(onAction: (CalculatorAction) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf(
            BtnDef("AC", ButtonLight, Color.Black, CalculatorAction.Clear),
            BtnDef("( )", ButtonLight, Color.Black, CalculatorAction.SmartBracket),
            BtnDef("%", ButtonLight, Color.Black, CalculatorAction.Percent),
            BtnDef("÷", ButtonOrange, action = CalculatorAction.Operation('/'))
        ),
        listOf(
            BtnDef("7", ButtonDark, action = CalculatorAction.Digit(7)),
            BtnDef("8", ButtonDark, action = CalculatorAction.Digit(8)),
            BtnDef("9", ButtonDark, action = CalculatorAction.Digit(9)),
            BtnDef("×", ButtonOrange, action = CalculatorAction.Operation('*'))
        ),
        listOf(
            BtnDef("4", ButtonDark, action = CalculatorAction.Digit(4)),
            BtnDef("5", ButtonDark, action = CalculatorAction.Digit(5)),
            BtnDef("6", ButtonDark, action = CalculatorAction.Digit(6)),
            BtnDef("−", ButtonOrange, action = CalculatorAction.Operation('-'))
        ),
        listOf(
            BtnDef("1", ButtonDark, action = CalculatorAction.Digit(1)),
            BtnDef("2", ButtonDark, action = CalculatorAction.Digit(2)),
            BtnDef("3", ButtonDark, action = CalculatorAction.Digit(3)),
            BtnDef("+", ButtonOrange, action = CalculatorAction.Operation('+'))
        )
    )

    Column(modifier = modifier) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { btn ->
                    CalcButton(
                        label = btn.label,
                        bg = btn.bg,
                        fg = btn.fg,
                        onClick = { onAction(btn.action) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Bottom row: 0, ., ⌫, =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalcButton("0", ButtonDark, onClick = { onAction(CalculatorAction.Digit(0)) }, modifier = Modifier.weight(1f))
            CalcButton(".", ButtonDark, onClick = { onAction(CalculatorAction.Decimal) }, modifier = Modifier.weight(1f))
            CalcButton("⌫", ButtonDark, onClick = { onAction(CalculatorAction.Delete) }, modifier = Modifier.weight(1f))
            CalcButton("=", ButtonOrange, onClick = { onAction(CalculatorAction.Calculate) }, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun CalcButton(
    label: String,
    bg: Color,
    fg: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}
