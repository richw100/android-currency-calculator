package com.tripcalc.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tripcalc.app.data.COUNTRY_LIST
import com.tripcalc.app.data.CountryLocalisationInfo
import com.tripcalc.app.data.DrivingInfo
import com.tripcalc.app.data.plugTypeDescription

@Composable
fun LocalisationScreen(vm: CalculatorViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    var showCountryPicker by remember { mutableStateOf(false) }

    // Load country info when screen first shows (or if code changed but info is null)
    LaunchedEffect(state.localisationCountryCode) {
        if (state.localisationInfo == null && !state.localisationLoading) {
            vm.onAction(CalculatorAction.SetLocalisationCountry(state.localisationCountryCode))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Country picker button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, colors.inputBorder, RoundedCornerShape(10.dp))
                    .clickable { showCountryPicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Country", fontSize = 11.sp, color = colors.textMuted)
                    val displayName = state.localisationInfo?.name
                        ?: COUNTRY_LIST.find { it.first == state.localisationCountryCode }?.second
                        ?: state.localisationCountryCode
                    Text(
                        text = "${currencyFlag(state.localisationCountryCode)}  $displayName",
                        fontSize = 16.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.localisationLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.fromAmountColor, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { vm.onAction(CalculatorAction.RefreshLocalisation) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("▼", fontSize = 12.sp, color = colors.textMuted)
                }
            }

            if (state.localisationError && state.localisationInfo == null) {
                InfoCard(title = "Could not load data", colors = colors) {
                    Text(
                        "Check your internet connection and tap refresh to try again. Static data (plug type, tap water, driving info) is still shown below.",
                        fontSize = 13.sp, color = colors.textSecondary, lineHeight = 19.sp
                    )
                }
            }

            val info = state.localisationInfo

            // Static data always comes from the bundled maps — never from cache —
            // so updates to the maps take effect immediately without a cache bust.
            val code = state.localisationCountryCode
            val drivingInfo = com.tripcalc.app.data.DRIVING_INFO[code]
            val plugInfo = com.tripcalc.app.data.PLUG_TYPES[code]
            val tapWaterSafe = com.tripcalc.app.data.TAP_WATER_SAFE[code]

            // Essentials card (API data)
            if (info != null) {
                InfoCard(title = "Essentials", colors = colors) {
                    InfoRow("Emergency", if (info.emergencyNumbers.isNotEmpty()) info.emergencyNumbers.joinToString(" / ") else "—", colors)
                    if (info.callingCode != null) InfoRow("Calling code", info.callingCode, colors)
                    if (info.currencies.isNotEmpty()) InfoRow("Currency", info.currencies.joinToString(", "), colors)
                    if (info.languages.isNotEmpty()) InfoRow("Languages", info.languages.joinToString(", "), colors)
                }
            } else if (!state.localisationLoading) {
                // Show partial essentials with just emergency from static driving data
                drivingInfo?.let {
                    InfoCard(title = "Essentials", colors = colors) {
                        Text("Connect to internet to load calling code, currency and language info.", fontSize = 12.sp, color = colors.textMuted, fontStyle = FontStyle.Italic)
                    }
                }
            }

            // Driving card
            if (info != null || drivingInfo != null) {
                InfoCard(title = "Driving", colors = colors) {
                    if (info?.drivingSide != null) {
                        InfoRow("Drives on", info.drivingSide.replaceFirstChar { it.uppercaseChar() }, colors)
                    }
                    if (drivingInfo != null) {
                        DrivingSpeedTable(drivingInfo, colors)
                        if (drivingInfo.bacLimit != null) {
                            val bacDisplay = if (drivingInfo.bacLimit == "0.00%") "Zero tolerance"
                                             else "${drivingInfo.bacLimit} BAC"
                            InfoRow("Drink-drive limit", bacDisplay, colors,
                                valueColor = if (drivingInfo.bacLimit == "0.00%") colors.errorColor else colors.textPrimary)
                        }
                    }
                    if (info?.drivingSide == null && drivingInfo == null) {
                        Text("No driving data available.", fontSize = 13.sp, color = colors.textMuted)
                    }
                }
            }

            // Practical card
            val context = LocalContext.current
            InfoCard(title = "Practical", colors = colors) {
                if (tapWaterSafe != null) {
                    InfoRow("Tap water", if (tapWaterSafe) "Generally safe to drink" else "Not recommended — use bottled", colors,
                        valueColor = if (tapWaterSafe) colors.positiveColor else colors.warningColor)
                } else {
                    InfoRow("Tap water", "Unknown — check locally", colors, valueColor = colors.textMuted)
                }
                if (plugInfo != null) {
                    InfoRow("Plug type", plugInfo.type, colors)
                    val desc = plugTypeDescription(plugInfo.type)
                    if (desc.isNotEmpty()) {
                        Text(desc, fontSize = 11.sp, color = colors.textMuted, lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(
                        "More info: worldstandards.eu",
                        fontSize = 11.sp, color = colors.fromAmountColor,
                        modifier = Modifier
                            .clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.worldstandards.eu/electricity/plugs-and-sockets/")))
                            }
                            .padding(top = 2.dp)
                    )
                    InfoRow("Voltage", "${plugInfo.voltage}V / ${plugInfo.frequency}Hz", colors)
                } else {
                    InfoRow("Plug type", "Unknown", colors, valueColor = colors.textMuted)
                }
                if (drivingInfo?.alcoholAge != null) InfoRow("Alcohol purchase age", "${drivingInfo.alcoholAge}+", colors)
            }

        }

        // Disclaimer — pinned outside the scroll area so it's always visible
        HorizontalDivider(color = colors.divider)
        Text(
            text = "Information sourced from Wikidata (CC0) and restcountries.com. Laws change — verify with official local sources before travelling. Developer accepts no liability for accuracy.",
            fontSize = 11.sp, color = colors.textMuted, lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            currentCode = state.localisationCountryCode,
            recentCodes = state.localisationRecentCountries,
            onSelect = { code ->
                showCountryPicker = false
                vm.onAction(CalculatorAction.SetLocalisationCountry(code))
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@Composable
private fun InfoCard(title: String, colors: AppColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textMuted,
            modifier = Modifier.padding(bottom = 2.dp))
        content()
    }
}

@Composable
private fun InfoRow(
    label: String, value: String, colors: AppColors,
    valueColor: androidx.compose.ui.graphics.Color = colors.textPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = colors.textSecondary,
            modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f))
    }
}

@Composable
private fun DrivingSpeedTable(info: DrivingInfo, colors: AppColors) {
    val u = info.speedUnit ?: "km/h"
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Speed limits ($u)", fontSize = 12.sp, color = colors.textMuted)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeedCell("Urban", info.speedUrban?.let { "$it $u" } ?: "—", colors, Modifier.weight(1f))
            SpeedCell("Rural", info.speedRural?.let { "$it $u" } ?: "—", colors, Modifier.weight(1f))
            SpeedCell("Motorway", info.speedMotorway?.let { "$it $u" } ?: "None", colors, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SpeedCell(label: String, value: String, colors: AppColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.buttonDigit.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
        Text(label, fontSize = 10.sp, color = colors.textMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CountryPickerDialog(
    currentCode: String,
    recentCodes: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var search by remember { mutableStateOf("") }
    val isSearching = search.isNotBlank()
    val filtered = remember(search) {
        if (isSearching) COUNTRY_LIST.filter { (_, name) -> name.contains(search, ignoreCase = true) }
        else COUNTRY_LIST
    }
    val recentEntries = remember(recentCodes) {
        recentCodes.mapNotNull { code -> COUNTRY_LIST.find { it.first == code } }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(16.dp)
        ) {
            Text("Select Country", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary, modifier = Modifier.padding(bottom = 10.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search…", color = colors.textMuted) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.fromAmountColor,
                    unfocusedBorderColor = colors.inputBorder,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.fromAmountColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                if (!isSearching && recentEntries.isNotEmpty()) {
                    item {
                        Text("Recent", fontSize = 11.sp, color = colors.textMuted, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    items(recentEntries) { (code, name) ->
                        CountryRow(code, name, currentCode, colors, onSelect)
                    }
                    item {
                        HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                items(filtered) { (code, name) ->
                    CountryRow(code, name, currentCode, colors, onSelect)
                }
            }
        }
    }
}

@Composable
private fun CountryRow(code: String, name: String, currentCode: String, colors: AppColors, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(code) }
            .background(if (code == currentCode) colors.fromAmountColor.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(currencyFlag(code), fontSize = 22.sp)
        Text(name, fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
        if (code == currentCode) {
            Text("✓", fontSize = 14.sp, color = colors.fromAmountColor)
        }
    }
}
