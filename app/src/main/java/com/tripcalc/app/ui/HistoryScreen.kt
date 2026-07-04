package com.tripcalc.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    vm: CalculatorViewModel,
    modifier: Modifier = Modifier,
    onEntryRestored: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    var editingNoteTimestamp by remember { mutableStateOf<Long?>(null) }
    var pendingRestore by remember { mutableStateOf<HistoryEntry?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = colors.surface,
            title = { Text("Clear all history?", color = colors.textPrimary) },
            text = {
                Text(
                    "This will delete all ${state.history.size} entr${if (state.history.size == 1) "y" else "ies"}. This cannot be undone.",
                    color = colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.onAction(CalculatorAction.ClearHistory)
                    showClearConfirm = false
                }) { Text("Clear all", color = colors.errorColor) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    editingNoteTimestamp?.let { ts ->
        val entry = state.history.find { it.timestamp == ts }
        if (entry != null) {
            NoteEditDialog(
                initialNote = entry.note,
                onSave = { note: String? ->
                    vm.onAction(CalculatorAction.SetHistoryNote(ts, note))
                    editingNoteTimestamp = null
                },
                onDismiss = { editingNoteTimestamp = null }
            )
        }
    }

    pendingRestore?.let { entry ->
        val colors = LocalAppColors.current
        val currenciesChange = entry.fromCurrency != state.fromCurrency || entry.toCurrency != state.toCurrency
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            containerColor = colors.surface,
            title = { Text("Restore calculation?", color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currenciesChange) {
                        Text(
                            "This will switch your currencies from ${state.fromCurrency} / ${state.toCurrency} to ${entry.fromCurrency} / ${entry.toCurrency}.",
                            color = colors.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
                        )
                    }
                    if (entry.cardName != null) {
                        val pctStr = entry.cardMarkupPercent?.let {
                            if (it == 0.0) "0%" else "${"%.2f".format(Locale.US, it).trimEnd('0').trimEnd('.')}%"
                        } ?: "0%"
                        val minStr = if (entry.cardMinFeeAmount != null && entry.cardMinFeeCurrency != null)
                            " or min. ${"%.2f".format(Locale.US, entry.cardMinFeeAmount).trimEnd('0').trimEnd('.')} ${entry.cardMinFeeCurrency}"
                        else ""
                        Text(
                            "Originally calculated with card \"${entry.cardName}\" ($pctStr fee$minStr). Your current card selection will not change.",
                            color = colors.warningColor, fontSize = 14.sp, lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.onAction(CalculatorAction.RestoreHistory(entry))
                    pendingRestore = null
                    onEntryRestored()
                }) { Text("Restore", color = colors.fromAmountColor) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No calculations yet.\nPress = to record an entry.",
                    color = colors.textMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.history.size} entr${if (state.history.size == 1) "y" else "ies"}",
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
                TextButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.errorColor)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear all", color = colors.errorColor, fontSize = 13.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.history, key = { it.timestamp }) { entry ->
                    HistoryEntryCard(
                        entry = entry,
                        onClick = {
                            // Non-currency entries hold unit labels ("10%", "mi") in the currency
                            // fields — restoring them never changes currencies, so skip the dialog
                            val isCurrencyEntry = entry.conversionMode == ConversionMode.CURRENCY.name
                            val currenciesMatch = entry.fromCurrency == state.fromCurrency && entry.toCurrency == state.toCurrency
                            val hasCardInfo = entry.cardName != null
                            if (!isCurrencyEntry || (currenciesMatch && !hasCardInfo)) {
                                vm.onAction(CalculatorAction.RestoreHistory(entry))
                                onEntryRestored()
                            } else {
                                pendingRestore = entry
                            }
                        },
                        onDelete = { vm.onAction(CalculatorAction.DeleteHistoryEntry(entry.timestamp)) },
                        onEditNote = { editingNoteTimestamp = entry.timestamp }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp)) {
            if (entry.expression.isNotEmpty()) {
                Text(text = entry.expression, color = colors.textSecondary, fontSize = 13.sp)
            }
            Text(text = entry.display, color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (entry.conversionMode) {
                    "DISTANCE"    -> "📏 ${entry.fromAmount}  →  ${entry.toAmount}"
                    "TEMPERATURE" -> "🌡 ${entry.fromAmount}  →  ${entry.toAmount}"
                    "TIP"         -> "🧾 ${entry.fromAmount}  ·  ${entry.toAmount}"
                    "FUEL"        -> "⛽ ${entry.fromAmount}  →  ${entry.toAmount}"
                    else          -> "${currencyFlag(entry.fromCurrency)} ${entry.fromAmount}  →  ${currencyFlag(entry.toCurrency)} ${entry.toAmount}"
                },
                color = colors.textSecondary,
                fontSize = 13.sp
            )

            if (!entry.note.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "\" ${entry.note} \"",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatHistoryTimestamp(entry.timestamp),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 0.dp)
                )
                Row {
                    IconButton(onClick = onEditNote, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Add note",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete entry",
                            tint = colors.errorColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditDialog(
    initialNote: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var text by remember { mutableStateOf(initialNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Add note", color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("e.g. Holiday spending money", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim().ifEmpty { null }) }) {
                Text("Save", color = colors.fromAmountColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

private fun formatHistoryTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(ts))
    }
}
