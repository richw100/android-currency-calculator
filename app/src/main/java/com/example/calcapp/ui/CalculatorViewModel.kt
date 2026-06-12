package com.example.calcapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calcapp.data.ExchangeRateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Currency as JavaCurrency
import kotlin.math.abs
import kotlin.math.floor

data class CustomRateEntry(val base: String, val rate: Double)

data class HistoryEntry(
    val display: String,
    val expression: String,
    val fromAmount: String,
    val toAmount: String,
    val fromCurrency: String,
    val toCurrency: String,
    val timestamp: Long
)

data class CalculatorUiState(
    val display: String = "0",
    val expression: String = "",
    val toCurrency: String = "EUR",
    val fromCurrency: String = "GBP",
    val toAmount: String = "EUR 0.00",
    val fromAmount: String = "GBP 0.00",
    val exchangeRateLabel: String = "",
    val lastUpdated: String = "",
    val isRefreshing: Boolean = false,
    val availableCurrencies: List<String> = emptyList(),
    val recentCurrencies: List<String> = emptyList(),
    val customRates: Map<String, CustomRateEntry> = emptyMap(),
    val liveRates: Map<String, Double> = emptyMap(),
    val customRatePctDiff: Double? = null,
    val hapticEnabled: Boolean = true,
    val isError: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val isOffline: Boolean = false,
    val darkModePref: DarkModePref = DarkModePref.SYSTEM,
    val accentScheme: AccentScheme = AccentScheme.TEAL_GREEN
)

sealed class CalculatorAction {
    data class Digit(val value: Int) : CalculatorAction()
    object Decimal : CalculatorAction()
    object Clear : CalculatorAction()
    object Delete : CalculatorAction()
    object Calculate : CalculatorAction()
    object Percent : CalculatorAction()
    object SmartBracket : CalculatorAction()
    object OpenBracket : CalculatorAction()
    object CloseBracket : CalculatorAction()
    object RefreshRates : CalculatorAction()
    object SwapCurrencies : CalculatorAction()
    data class Operation(val symbol: Char) : CalculatorAction()
    data class SetToCurrency(val code: String) : CalculatorAction()
    data class SetFromCurrency(val code: String) : CalculatorAction()
    data class SetCustomRate(val target: String, val base: String, val rate: Double) : CalculatorAction()
    data class ClearCustomRate(val code: String) : CalculatorAction()
    data class SetHaptic(val enabled: Boolean) : CalculatorAction()
    data class PasteValue(val text: String) : CalculatorAction()
    data class RestoreHistory(val entry: HistoryEntry) : CalculatorAction()
    object ClearHistory : CalculatorAction()
    data class SetDarkMode(val pref: DarkModePref) : CalculatorAction()
    data class SetAccentScheme(val scheme: AccentScheme) : CalculatorAction()
}

private data class BracketState(val firstOperand: Double?, val pendingOp: Char?)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExchangeRateRepository(application)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var currentInput = "0"
    private var firstOperand: Double? = null
    private var pendingOp: Char? = null
    private var justCalculated = false
    private var shouldResetInput = false

    // Each ( pushes the current (firstOperand, pendingOp) so ) can restore it
    private val bracketStack = mutableListOf<BracketState>()

    // Drives the small expression line above the main display
    private val expressionDisplay = StringBuilder()

    private var rates: Map<String, Double> = emptyMap()

    init {
        viewModelScope.launch {
            val (to, from) = repository.loadCurrencyPrefs()
            val recents = repository.loadRecentCurrencies()
            val customRates = repository.loadCustomRates()
            val hapticEnabled = repository.loadHapticEnabled()
            val history = repository.loadHistory()
            val darkModePref = repository.loadDarkModePref()
            val accentScheme = repository.loadAccentScheme()
            _uiState.update { it.copy(toCurrency = to, fromCurrency = from, recentCurrencies = recents, customRates = customRates, hapticEnabled = hapticEnabled, history = history, darkModePref = darkModePref, accentScheme = accentScheme) }
            fetchRates(forceRefresh = false)
        }
    }

    private fun updateRecents(code: String) {
        val updated = (_uiState.value.recentCurrencies.toMutableList()
            .also { it.remove(code) }
            .also { it.add(0, code) })
            .take(5)
        _uiState.update { it.copy(recentCurrencies = updated) }
        viewModelScope.launch { repository.saveRecentCurrencies(updated) }
    }

    private fun fetchRates(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                rates = repository.getRates("USD", forceRefresh)
                val updated = repository.getLastUpdated()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        lastUpdated = "Updated $updated",
                        availableCurrencies = rates.keys.sorted(),
                        liveRates = rates,
                        isOffline = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, lastUpdated = "Offline", isOffline = true) }
            }
            updateCurrencyDisplay()
        }
    }

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Digit -> handleDigit(action.value)
            is CalculatorAction.Decimal -> handleDecimal()
            is CalculatorAction.Clear -> handleClear()
            is CalculatorAction.Delete -> handleDelete()
            is CalculatorAction.Calculate -> {
                handleCalculate()
                updateDisplay()
                if (!_uiState.value.isError && _uiState.value.display != "0") recordHistory()
                return
            }
            is CalculatorAction.Percent -> handlePercent()
            is CalculatorAction.SmartBracket -> {
                val atStart = firstOperand == null && pendingOp == null
                    && currentInput == "0" && bracketStack.isEmpty() && !justCalculated
                if (shouldResetInput || atStart) handleOpenBracket()
                else if (bracketStack.isNotEmpty()) handleCloseBracket()
            }
            is CalculatorAction.OpenBracket -> handleOpenBracket()
            is CalculatorAction.CloseBracket -> handleCloseBracket()
            is CalculatorAction.Operation -> handleOperation(action.symbol)
            is CalculatorAction.RefreshRates -> { fetchRates(forceRefresh = true); return }
            is CalculatorAction.SwapCurrencies -> {
                val s = _uiState.value
                _uiState.update { it.copy(toCurrency = s.fromCurrency, fromCurrency = s.toCurrency) }
                viewModelScope.launch { repository.saveCurrencyPrefs(s.fromCurrency, s.toCurrency) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetToCurrency -> {
                _uiState.update { it.copy(toCurrency = action.code) }
                updateRecents(action.code)
                viewModelScope.launch { repository.saveCurrencyPrefs(action.code, _uiState.value.fromCurrency) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetFromCurrency -> {
                _uiState.update { it.copy(fromCurrency = action.code) }
                updateRecents(action.code)
                viewModelScope.launch { repository.saveCurrencyPrefs(_uiState.value.toCurrency, action.code) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetCustomRate -> {
                val updated = _uiState.value.customRates.toMutableMap()
                    .also { it[action.target] = CustomRateEntry(action.base, action.rate) }
                _uiState.update { it.copy(customRates = updated) }
                viewModelScope.launch { repository.saveCustomRates(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.ClearCustomRate -> {
                val updated = _uiState.value.customRates.toMutableMap().also { it.remove(action.code) }
                _uiState.update { it.copy(customRates = updated) }
                viewModelScope.launch { repository.saveCustomRates(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetHaptic -> {
                _uiState.update { it.copy(hapticEnabled = action.enabled) }
                viewModelScope.launch { repository.saveHapticEnabled(action.enabled) }
                return
            }
            is CalculatorAction.PasteValue -> {
                val cleaned = action.text.replace(",", "").trim()
                if (cleaned.toDoubleOrNull() != null) {
                    currentInput = cleaned
                    firstOperand = null; pendingOp = null
                    bracketStack.clear(); expressionDisplay.clear()
                    justCalculated = false; shouldResetInput = false
                    _uiState.update { it.copy(isError = false) }
                    updateDisplay()
                }
                return
            }
            is CalculatorAction.RestoreHistory -> {
                currentInput = action.entry.display
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
                justCalculated = true; shouldResetInput = false
                _uiState.update { it.copy(isError = false) }
                updateDisplay(); return
            }
            is CalculatorAction.ClearHistory -> {
                _uiState.update { it.copy(history = emptyList()) }
                viewModelScope.launch { repository.saveHistory(emptyList()) }
                return
            }
            is CalculatorAction.SetDarkMode -> {
                _uiState.update { it.copy(darkModePref = action.pref) }
                viewModelScope.launch { repository.saveDarkModePref(action.pref) }
                return
            }
            is CalculatorAction.SetAccentScheme -> {
                _uiState.update { it.copy(accentScheme = action.scheme) }
                viewModelScope.launch { repository.saveAccentScheme(action.scheme) }
                return
            }
        }
        updateDisplay()
    }

    private fun handleDigit(digit: Int) {
        if (justCalculated || shouldResetInput) {
            if (justCalculated) {
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
            }
            currentInput = digit.toString()
            justCalculated = false; shouldResetInput = false
            return
        }
        currentInput = if (currentInput == "0") digit.toString()
        else if (currentInput.length < 12) currentInput + digit.toString()
        else currentInput
    }

    private fun handleDecimal() {
        if (justCalculated || shouldResetInput) {
            if (justCalculated) {
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
            }
            currentInput = "0."
            justCalculated = false; shouldResetInput = false
            return
        }
        if (!currentInput.contains('.')) currentInput += "."
    }

    private fun handleClear() {
        currentInput = "0"
        firstOperand = null; pendingOp = null
        justCalculated = false; shouldResetInput = false
        bracketStack.clear(); expressionDisplay.clear()
        _uiState.update { it.copy(isError = false) }
    }

    private fun handleDelete() {
        if (justCalculated || _uiState.value.isError) { handleClear(); return }
        if (shouldResetInput) {
            // Undo the last operator: trim it from the expression display
            if (pendingOp != null) {
                val expr = expressionDisplay.toString().trimEnd()
                val lastSpace = expr.lastIndexOf(' ')
                expressionDisplay.clear()
                if (lastSpace > 0) expressionDisplay.append(expr.substring(0, lastSpace).trimEnd())
                // Restore currentInput to show firstOperand value
                currentInput = formatResult(firstOperand ?: 0.0)
                if (firstOperand != null && lastSpace <= 0) firstOperand = null
                pendingOp = null
            }
            shouldResetInput = false
            return
        }
        currentInput = if (currentInput.length <= 1) "0" else currentInput.dropLast(1)
    }

    private fun handlePercent() {
        if (shouldResetInput) return
        val value = currentInput.toDoubleOrNull() ?: return
        currentInput = formatResult(value / 100.0)
        justCalculated = false
    }

    private fun handleOperation(op: Char) {
        if (_uiState.value.isError) return
        justCalculated = false
        val sym = opSymbol(op)

        if (shouldResetInput) {
            // Replace pending operator or set new one after bracket close
            if (pendingOp != null) {
                val expr = expressionDisplay.toString().trimEnd()
                expressionDisplay.clear()
                expressionDisplay.append(expr.substring(0, expr.lastIndexOf(' ')).trimEnd())
                expressionDisplay.append(" $sym ")
            } else {
                // After bracket close: firstOperand holds the result, just attach new op
                expressionDisplay.append(" $sym ")
            }
            pendingOp = op
            return
        }

        val current = currentInput.toDoubleOrNull() ?: return
        if (firstOperand != null && pendingOp != null) {
            val result = applyOp(firstOperand!!, current, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true, display = "Error") }; return
            }
            firstOperand = result
            currentInput = formatResult(result)
        } else {
            firstOperand = current
        }
        expressionDisplay.append("$currentInput $sym ")
        pendingOp = op
        shouldResetInput = true
    }

    private fun handleOpenBracket() {
        // Only open when we're waiting for a new operand (after operator or at fresh start)
        val atStart = firstOperand == null && pendingOp == null
            && currentInput == "0" && bracketStack.isEmpty() && !justCalculated
        if (!shouldResetInput && !atStart) return

        justCalculated = false
        bracketStack.add(BracketState(firstOperand, pendingOp))
        firstOperand = null; pendingOp = null
        shouldResetInput = false
        currentInput = "0"
        expressionDisplay.append("(")
    }

    private fun handleCloseBracket() {
        if (bracketStack.isEmpty()) return

        val current = currentInput.toDoubleOrNull() ?: return
        val bracketResult = if (firstOperand != null && pendingOp != null) {
            applyOp(firstOperand!!, current, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true) }; return
            }
        } else current

        expressionDisplay.append("$currentInput)")

        val (outerFirst, outerOp) = bracketStack.removeLast()
        firstOperand = outerFirst
        pendingOp = outerOp

        // Apply the outer pending operation to the bracket result
        val result = if (firstOperand != null && pendingOp != null) {
            applyOp(firstOperand!!, bracketResult, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true) }; return
            }
        } else bracketResult

        firstOperand = result
        pendingOp = null
        currentInput = formatResult(result)
        shouldResetInput = true
    }

    private fun handleCalculate() {
        if (_uiState.value.isError) { handleClear(); return }

        // Auto-close any unclosed brackets
        var value = currentInput.toDoubleOrNull() ?: return

        if (firstOperand != null && pendingOp != null) {
            value = applyOp(firstOperand!!, value, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true, display = "Error") }; return
            }
        } else if (firstOperand != null && shouldResetInput) {
            value = firstOperand!!
        }

        for ((stackFirst, stackOp) in bracketStack.asReversed()) {
            if (stackFirst != null && stackOp != null) {
                value = applyOp(stackFirst, value, stackOp) ?: continue
            }
        }

        currentInput = formatResult(value)
        firstOperand = null; pendingOp = null
        bracketStack.clear(); expressionDisplay.clear()
        justCalculated = true
    }

    private fun recordHistory() {
        val state = _uiState.value
        val entry = HistoryEntry(
            display = state.display,
            expression = state.expression,
            fromAmount = state.fromAmount,
            toAmount = state.toAmount,
            fromCurrency = state.fromCurrency,
            toCurrency = state.toCurrency,
            timestamp = System.currentTimeMillis()
        )
        val updated = (listOf(entry) + state.history).take(50)
        _uiState.update { it.copy(history = updated) }
        viewModelScope.launch { repository.saveHistory(updated) }
    }

    private fun applyOp(a: Double, b: Double, op: Char): Double? = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0.0) null else a / b
        else -> null
    }

    private fun opSymbol(op: Char) = when (op) {
        '*' -> "×"; '/' -> "÷"; '-' -> "−"; else -> op.toString()
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        return if (value == floor(value) && abs(value) < 1e12) value.toLong().toString()
        else "%.10g".format(value).trimEnd('0').trimEnd('.')
    }

    private fun updateDisplay() {
        val expression = when {
            expressionDisplay.isNotEmpty() -> expressionDisplay.toString()
            firstOperand != null && pendingOp != null ->
                "${formatResult(firstOperand!!)} ${opSymbol(pendingOp!!)}"
            else -> ""
        }
        _uiState.update { it.copy(display = currentInput, expression = expression, isError = false) }
        updateCurrencyDisplay()
    }

    // Returns the effective "1 USD = X [code]" rate, preferring custom pair entries over live rates.
    // Custom entry "1 base = rate target" → effectiveUsdRate(target) = liveUsdRate(base) * rate
    private fun effectiveUsdRate(code: String, state: CalculatorUiState): Double? {
        val entry = state.customRates[code]
        return if (entry != null) {
            val baseRate = rates[entry.base] ?: return null
            baseRate * entry.rate
        } else {
            rates[code]
        }
    }

    private fun updateCurrencyDisplay() {
        val value = currentInput.toDoubleOrNull() ?: 0.0
        val state = _uiState.value
        val fromRate = effectiveUsdRate(state.fromCurrency, state)
        val toRate = effectiveUsdRate(state.toCurrency, state)

        if (fromRate != null && toRate != null && fromRate != 0.0) {
            val toValue = value * toRate / fromRate
            val fromName = try { JavaCurrency.getInstance(state.fromCurrency).displayName } catch (e: Exception) { state.fromCurrency }
            val toName = try { JavaCurrency.getInstance(state.toCurrency).displayName } catch (e: Exception) { state.toCurrency }
            val usingCustom = state.customRates.containsKey(state.fromCurrency) || state.customRates.containsKey(state.toCurrency)
            val rateLabel = "1 ${state.fromCurrency} ($fromName) = ${"%.4f".format(toRate / fromRate)} ${state.toCurrency} ($toName)" +
                if (usingCustom) " ★" else ""
            val pctDiff = if (usingCustom) {
                val liveFrom = rates[state.fromCurrency]
                val liveTo = rates[state.toCurrency]
                if (liveFrom != null && liveTo != null && liveFrom != 0.0) {
                    val liveRate = liveTo / liveFrom
                    val customRate = toRate / fromRate
                    (customRate - liveRate) / liveRate * 100.0
                } else null
            } else null
            _uiState.update {
                it.copy(
                    fromAmount = "${state.fromCurrency} ${"%.2f".format(value)}",
                    toAmount = "${state.toCurrency} ${"%.2f".format(toValue)}",
                    exchangeRateLabel = rateLabel,
                    customRatePctDiff = pctDiff
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    fromAmount = "${state.fromCurrency} —",
                    toAmount = "${state.toCurrency} —",
                    exchangeRateLabel = "",
                    customRatePctDiff = null
                )
            }
        }
    }
}
