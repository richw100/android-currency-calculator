package com.example.calcapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Column(modifier = modifier.fillMaxSize()) {
        if (state.history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No calculations yet.\nPress = to record an entry.",
                    color = Color(0xFF636366),
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
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp
                )
                TextButton(onClick = { vm.onAction(CalculatorAction.ClearHistory) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF453A)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Clear all", color = Color(0xFFFF453A), fontSize = 13.sp)
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
                            vm.onAction(CalculatorAction.RestoreHistory(entry))
                            onEntryRestored()
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: HistoryEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (entry.expression.isNotEmpty()) {
                Text(
                    text = entry.expression,
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp
                )
            }
            Text(
                text = entry.display,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${currencyFlag(entry.fromCurrency)} ${entry.fromAmount}  →  ${currencyFlag(entry.toCurrency)} ${entry.toAmount}",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatHistoryTimestamp(entry.timestamp),
                    color = Color(0xFF48484A),
                    fontSize = 11.sp
                )
            }
        }
    }
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
