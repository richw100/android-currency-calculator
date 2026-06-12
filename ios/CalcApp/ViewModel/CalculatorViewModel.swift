import Foundation
import UIKit

@MainActor
final class CalculatorViewModel: ObservableObject {
    @Published var display: String = "0"
    @Published var expression: String = ""
    @Published var fromCurrency: String = "USD"
    @Published var toCurrency: String = "GBP"
    @Published var convertedAmount: String = ""
    @Published var fromLabel: String = ""
    @Published var rates: [String: Double] = [:]
    @Published var availableCurrencies: [String] = []
    @Published var recentCurrencies: [String] = []
    @Published var customRates: [String: CustomRate] = [:]
    @Published var history: [HistoryEntry] = []
    @Published var isOffline: Bool = false
    @Published var isLoadingRates: Bool = false
    @Published var darkModePref: DarkModePref = .system
    @Published var accentScheme: AccentScheme = .tealGreen
    @Published var hapticEnabled: Bool = true
    @Published var liveRates: [String: Double] = [:]

    private let service = ExchangeRateService()
    private let persistence = PersistenceService.shared
    private var justCalculated = false
    private var hasDecimal = false

    init() {
        fromCurrency     = persistence.loadFromCurrency()
        toCurrency       = persistence.loadToCurrency()
        recentCurrencies = persistence.loadRecentCurrencies()
        customRates      = persistence.loadCustomRates()
        history          = persistence.loadHistory()
        darkModePref     = persistence.loadDarkModePref()
        accentScheme     = persistence.loadAccentScheme()
        hapticEnabled    = persistence.loadHaptic()
        Task { await fetchRates() }
    }

    // MARK: - Button actions

    func tap(_ button: CalcButton) {
        triggerHaptic()
        switch button {
        case .digit(let d):       handleDigit(d)
        case .decimal:            handleDecimal()
        case .operator_(let op): handleOperator(op)
        case .equals:             handleEquals()
        case .clear:              handleClear()
        case .negate:             handleNegate()
        case .percent:            handlePercent()
        case .bracket:            handleBracket()
        case .delete:             handleDelete()
        }
        updateConversion()
    }

    // MARK: - Input handlers

    private func handleDigit(_ d: String) {
        if justCalculated { expression = ""; display = "0"; justCalculated = false; hasDecimal = false }
        if display == "0" && d != "." {
            display = d
        } else {
            if display.count < 15 { display += d }
        }
        syncExpressionTail()
    }

    private func handleDecimal() {
        if justCalculated { expression = ""; display = "0"; justCalculated = false; hasDecimal = false }
        if !hasDecimal {
            display += "."
            hasDecimal = true
            syncExpressionTail()
        }
    }

    private func handleOperator(_ op: String) {
        justCalculated = false
        hasDecimal = false
        let tail = expression.isEmpty ? display : replaceTrailingOperator(in: expression, with: op)
        if tail != expression {
            expression = tail
        } else {
            expression = (expression.isEmpty ? display : expression) + " \(op) "
            display = "0"
        }
    }

    private func handleEquals() {
        let full = expression.isEmpty ? display : expression + display
        guard let result = evaluate(full) else { display = "Error"; expression = ""; return }
        let resultStr = formatResult(result)
        let fromAmtStr = resultStr
        let toAmtStr = convertedString(value: result)
        history.insert(
            HistoryEntry(
                expression: full,
                result: resultStr,
                fromAmount: fromAmtStr,
                fromCurrency: fromCurrency,
                toAmount: toAmtStr,
                toCurrency: toCurrency
            ),
            at: 0
        )
        if history.count > 50 { history = Array(history.prefix(50)) }
        persistence.saveHistory(history)
        expression = ""
        display = resultStr
        justCalculated = true
        hasDecimal = resultStr.contains(".")
    }

    private func handleClear() {
        display = "0"
        expression = ""
        justCalculated = false
        hasDecimal = false
    }

    private func handleNegate() {
        if let v = Double(display) {
            display = formatResult(-v)
            syncExpressionTail()
        }
    }

    private func handlePercent() {
        if let v = Double(display) {
            display = formatResult(v / 100)
            hasDecimal = display.contains(".")
            syncExpressionTail()
        }
    }

    private func handleBracket() {
        justCalculated = false
        let openCount  = expression.filter { $0 == "(" }.count
        let closeCount = expression.filter { $0 == ")" }.count
        if openCount == closeCount || display != "0" {
            expression += (expression.isEmpty ? "" : display) + "("
            display = "0"
        } else {
            expression += display + ")"
            display = "0"
        }
        hasDecimal = false
    }

    private func handleDelete() {
        if justCalculated { handleClear(); return }
        if display.count > 1 {
            let removed = display.removeLast()
            if removed == "." { hasDecimal = false }
        } else {
            display = "0"
            hasDecimal = false
        }
        syncExpressionTail()
    }

    private func syncExpressionTail() {
        // expression already has everything except the current number being typed
    }

    private func replaceTrailingOperator(in expr: String, with op: String) -> String {
        let ops = ["+", "-", "×", "÷"]
        let trimmed = expr.trimmingCharacters(in: .whitespaces)
        for o in ops {
            if trimmed.hasSuffix(o) {
                let base = String(trimmed.dropLast(o.count)).trimmingCharacters(in: .whitespaces)
                return base + " \(op) "
            }
        }
        return expr
    }

    // MARK: - Currency conversion

    private func updateConversion() {
        guard let value = Double(display), value != 0 else {
            convertedAmount = ""
            fromLabel = ""
            return
        }
        convertedAmount = convertedString(value: value)
        fromLabel = formatAmount(value)
    }

    private func convertedString(value: Double) -> String {
        guard let converted = convert(value: value, from: fromCurrency, to: toCurrency) else { return "—" }
        return formatAmount(converted)
    }

    func convert(value: Double, from: String, to: String) -> Double? {
        let fromRate = effectiveRate(for: from)
        let toRate   = effectiveRate(for: to)
        guard let f = fromRate, let t = toRate, f > 0 else { return nil }
        return value * f / t
    }

    private func effectiveRate(for code: String) -> Double? {
        if let custom = customRates[code] { return custom.effectiveUsdRate(usdRates: liveRates) }
        return rates[code]
    }

    private func formatAmount(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.string(from: NSNumber(value: value)) ?? String(format: "%.2f", value)
    }

    // MARK: - Expression evaluator

    func evaluate(_ expr: String) -> Double? {
        let sanitised = expr
            .replacingOccurrences(of: "×", with: "*")
            .replacingOccurrences(of: "÷", with: "/")
            .replacingOccurrences(of: " ", with: "")
        let exp = NSExpression(format: sanitised)
        return exp.expressionValue(with: nil, context: nil) as? Double
    }

    private func formatResult(_ value: Double) -> String {
        if value.truncatingRemainder(dividingBy: 1) == 0 && abs(value) < 1e12 {
            return String(Int64(value))
        }
        let s = String(format: "%.10g", value)
        return s
    }

    // MARK: - Rates

    func fetchRates(forceRefresh: Bool = false) async {
        isLoadingRates = true
        do {
            let fetched = try await service.getRates(forceRefresh: forceRefresh)
            rates = fetched
            liveRates = fetched
            availableCurrencies = fetched.keys.sorted()
            isOffline = false
        } catch {
            isOffline = true
        }
        isLoadingRates = false
        updateConversion()
    }

    // MARK: - Currency selection

    func selectFromCurrency(_ code: String) {
        fromCurrency = code
        addRecent(code)
        persistence.saveFromCurrency(code)
        updateConversion()
    }

    func selectToCurrency(_ code: String) {
        toCurrency = code
        addRecent(code)
        persistence.saveToCurrency(code)
        updateConversion()
    }

    private func addRecent(_ code: String) {
        var r = recentCurrencies.filter { $0 != code }
        r.insert(code, at: 0)
        recentCurrencies = Array(r.prefix(6))
        persistence.saveRecentCurrencies(recentCurrencies)
    }

    // MARK: - History

    func restoreHistory(_ entry: HistoryEntry) {
        display = entry.result
        expression = ""
        justCalculated = true
        hasDecimal = entry.result.contains(".")
        updateConversion()
    }

    func clearHistory() {
        history = []
        persistence.saveHistory(history)
    }

    // MARK: - Custom rates

    func setCustomRate(target: String, base: String, rate: Double) {
        customRates[target] = CustomRate(base: base, rate: rate)
        persistence.saveCustomRates(customRates)
        updateConversion()
    }

    func removeCustomRate(target: String) {
        customRates.removeValue(forKey: target)
        persistence.saveCustomRates(customRates)
        updateConversion()
    }

    // MARK: - Settings

    func setDarkMode(_ pref: DarkModePref) {
        darkModePref = pref
        persistence.saveDarkModePref(pref)
    }

    func setAccentScheme(_ scheme: AccentScheme) {
        accentScheme = scheme
        persistence.saveAccentScheme(scheme)
    }

    func setHaptic(_ enabled: Bool) {
        hapticEnabled = enabled
        persistence.saveHaptic(enabled)
    }

    // MARK: - Haptic

    private func triggerHaptic() {
        guard hapticEnabled else { return }
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }
}

// MARK: - Button model

enum CalcButton: Hashable {
    case digit(String)
    case decimal
    case `operator_`(String)
    case equals
    case clear
    case negate
    case percent
    case bracket
    case delete
}
