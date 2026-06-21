import Foundation
import UIKit

@MainActor
final class CalculatorViewModel: ObservableObject {

    // MARK: - Calculator core
    @Published var display: String = "0"
    @Published var expression: String = ""

    // MARK: - Mode
    @Published var conversionMode: ConversionMode = .currency
    @Published var enabledModes: Set<ConversionMode> = Set(ConversionMode.allCases)

    // MARK: - Currency
    @Published var fromCurrency: String = "GBP"
    @Published var toCurrency: String = "EUR"
    @Published var fromAmount: String = ""
    @Published var toAmount: String = ""
    @Published var exchangeRateLabel: String = ""
    @Published var lastUpdated: String = ""
    @Published var isRefreshing: Bool = false
    @Published var isOffline: Bool = false
    @Published var availableCurrencies: [String] = []
    @Published var recentCurrencies: [String] = []
    @Published var customRates: [String: CustomRate] = [:]
    @Published var liveRates: [String: Double] = [:]
    @Published var customRatePctDiff: Double? = nil

    // MARK: - Card profiles
    @Published var cardProfiles: [CardProfile] = []
    @Published var activeCardId: String? = nil

    // MARK: - Distance
    @Published var distancePair: DistancePair = .milesKm
    @Published var distanceReversed: Bool = false
    @Published var enabledDistancePairs: Set<DistancePair> = [.milesKm]

    // MARK: - Temperature
    @Published var tempUnit: TempUnit = .celsius

    // MARK: - Tip
    @Published var tipPercent: Double = 10.0
    @Published var defaultTipPercent: Double = 10.0
    @Published var customTipPercent: Double = 12.5
    @Published var tipPeopleCount: Int = 1

    // MARK: - Fuel
    @Published var fuelMode: FuelMode = .mpg
    @Published var fuelUseUkGallons: Bool = true
    @Published var fuelInputIsFirst: Bool = true  // true = mpg/mi-kWh side; false = L/100km/kWh-100km side

    // MARK: - Size
    @Published var sizeCategory: SizeCategory = .shoe
    @Published var shoeIsMens: Bool = true
    @Published var shoeFromCountry: String = "EU"
    @Published var shoeToCountry: String = "UK"
    @Published var womensFromCountry: String = "US"
    @Published var womensToCountry: String = "UK"
    @Published var mensFromCountry: String = "US/UK"
    @Published var mensToCountry: String = "EU"

    // MARK: - History
    @Published var history: [HistoryEntry] = []

    // MARK: - Preferences
    @Published var hapticEnabled: Bool = true
    @Published var darkModePref: DarkModePref = .system
    @Published var accentScheme: AccentScheme = .tealGreen
    @Published var swapZeroDot: Bool = true
    @Published var helpHintSeen: Bool = false

    // MARK: - Private engine state
    private struct BracketState {
        var firstOperand: Double?
        var pendingOp: Character?
        var expressionSoFar: String
    }

    private var currentInput: String = "0"
    private var firstOperand: Double? = nil
    private var pendingOp: Character? = nil
    private var shouldResetInput: Bool = false
    private var justCalculated: Bool = false
    private var expressionDisplay: String = ""
    private var bracketStack: [BracketState] = []
    private var tipSavedBill: Double? = nil

    private var rates: [String: Double] = [:]
    private let service = ExchangeRateService()
    private let persistence = PersistenceService.shared

    // MARK: - Init

    init() {
        fromCurrency      = persistence.loadFromCurrency()
        toCurrency        = persistence.loadToCurrency()
        recentCurrencies  = persistence.loadRecentCurrencies()
        customRates       = persistence.loadCustomRates()
        history           = persistence.loadHistory()
        darkModePref      = persistence.loadDarkModePref()
        accentScheme      = persistence.loadAccentScheme()
        hapticEnabled     = persistence.loadHaptic()
        enabledModes      = persistence.loadEnabledModes().union([.currency])
        enabledDistancePairs = persistence.loadEnabledDistancePairs().union([.milesKm])
        distancePair      = persistence.loadDistancePair()
        distanceReversed  = persistence.loadDistanceReversed()
        tempUnit          = persistence.loadTempUnit()
        fuelMode          = persistence.loadFuelMode()
        fuelUseUkGallons  = persistence.loadFuelUkGallons()
        fuelInputIsFirst  = persistence.loadFuelInputIsFirst()
        defaultTipPercent = persistence.loadDefaultTipPercent()
        customTipPercent  = persistence.loadCustomTipPercent()
        tipPeopleCount    = persistence.loadTipPeople()
        tipPercent        = defaultTipPercent
        cardProfiles      = persistence.loadCardProfiles()
        activeCardId      = persistence.loadActiveCardId()
        swapZeroDot       = persistence.loadSwapZeroDot()
        sizeCategory      = persistence.loadSizeCategory()
        shoeIsMens        = persistence.loadShoeIsMens()
        shoeFromCountry   = persistence.loadShoeFrom()
        shoeToCountry     = persistence.loadShoeTo()
        womensFromCountry = persistence.loadWomensFrom()
        womensToCountry   = persistence.loadWomensTo()
        mensFromCountry   = persistence.loadMensFrom()
        mensToCountry     = persistence.loadMensTo()
        helpHintSeen      = persistence.loadHelpHintSeen()

        if !enabledDistancePairs.contains(distancePair) { distancePair = .milesKm }
        if !enabledModes.contains(conversionMode) { conversionMode = .currency }

        Task { await fetchRates() }
    }

    // MARK: - Button input

    func tap(_ button: CalcButton) {
        triggerHaptic()
        switch button {
        case .digit(let d):      handleDigit(d)
        case .decimal:           handleDecimal()
        case .operator_(let op): handleOperator(op)
        case .equals:            handleEquals()
        case .clear:             handleClear()
        case .negate:            handleNegate()
        case .percent:           handlePercent()
        case .bracket:           handleSmartBracket()
        case .delete:            handleDelete()
        }
        updateDisplay()
    }

    func pasteValue(_ text: String) {
        let cleaned = text.replacingOccurrences(of: ",", with: "")
        guard Double(cleaned) != nil else { return }
        currentInput = cleaned
        shouldResetInput = false
        justCalculated = true
        updateDisplay()
    }

    // MARK: - Input handlers

    private func handleDigit(_ d: String) {
        if justCalculated {
            currentInput = "0"; expression = ""; expressionDisplay = ""
            firstOperand = nil; pendingOp = nil; bracketStack = []
            justCalculated = false; shouldResetInput = false
        }
        if shouldResetInput {
            currentInput = "0"
            shouldResetInput = false
        }
        if currentInput == "0" {
            currentInput = d
        } else {
            if currentInput.count < 12 { currentInput += d }
        }
    }

    private func handleDecimal() {
        if justCalculated {
            currentInput = "0"; expression = ""; expressionDisplay = ""
            firstOperand = nil; pendingOp = nil; bracketStack = []
            justCalculated = false; shouldResetInput = false
        }
        if shouldResetInput { currentInput = "0"; shouldResetInput = false }
        if !currentInput.contains(".") { currentInput += "." }
    }

    private func handleOperator(_ op: Character) {
        if justCalculated { justCalculated = false }
        let value = Double(currentInput) ?? 0.0

        if shouldResetInput {
            pendingOp = op
            expressionDisplay = formatResult(firstOperand ?? value) + " " + opSymbol(op)
            shouldResetInput = false
            return
        }

        if let fo = firstOperand, let pop = pendingOp {
            if let result = applyOp(fo, value, pop) {
                firstOperand = result
                expressionDisplay = formatResult(result) + " " + opSymbol(op)
            } else {
                currentInput = "Error"; firstOperand = nil; pendingOp = nil
                expressionDisplay = ""; shouldResetInput = false
                return
            }
        } else {
            firstOperand = value
            expressionDisplay = formatResult(value) + " " + opSymbol(op)
        }
        pendingOp = op
        shouldResetInput = true
    }

    private func handleEquals() {
        if conversionMode == .tip { tipSavedBill = nil }
        let value = Double(currentInput) ?? 0.0

        if let fo = firstOperand, let pop = pendingOp {
            let fullExpr = expressionDisplay + " " + currentInput
            if let result = applyOp(fo, value, pop) {
                recordHistory(expression: fullExpr, result: formatResult(result), inputValue: result)
                currentInput = formatResult(result)
                firstOperand = nil; pendingOp = nil
                expressionDisplay = ""
                justCalculated = true
                shouldResetInput = false
            } else {
                currentInput = "Error"; firstOperand = nil; pendingOp = nil
                expressionDisplay = ""; shouldResetInput = false
            }
        } else {
            let fullExpr = expressionDisplay.isEmpty ? currentInput : expressionDisplay + " " + currentInput
            // Close any open brackets
            var result = value
            while let frame = bracketStack.last {
                bracketStack.removeLast()
                if let fo = frame.firstOperand, let pop = frame.pendingOp {
                    result = applyOp(fo, result, pop) ?? result
                }
                expressionDisplay = frame.expressionSoFar
            }
            recordHistory(expression: fullExpr, result: formatResult(result), inputValue: result)
            currentInput = formatResult(result)
            firstOperand = nil; pendingOp = nil
            expressionDisplay = ""
            justCalculated = true
            shouldResetInput = false
        }
    }

    private func handleClear() {
        currentInput = "0"
        firstOperand = nil; pendingOp = nil
        expressionDisplay = ""
        bracketStack = []
        justCalculated = false
        shouldResetInput = false
        if conversionMode == .tip { tipSavedBill = nil }
    }

    private func handleNegate() {
        if let v = Double(currentInput), v != 0 {
            currentInput = formatResult(-v)
        }
    }

    private func handlePercent() {
        if let v = Double(currentInput) {
            currentInput = formatResult(v / 100.0)
        }
    }

    private func handleSmartBracket() {
        justCalculated = false
        let openCount  = expressionDisplay.filter { $0 == "(" }.count + bracketStack.count
        let closeCount = expressionDisplay.filter { $0 == ")" }.count
        let hasOpen    = openCount > closeCount || !bracketStack.isEmpty

        if !hasOpen || currentInput == "0" && expressionDisplay.isEmpty {
            bracketStack.append(BracketState(firstOperand: firstOperand, pendingOp: pendingOp, expressionSoFar: expressionDisplay))
            firstOperand = nil; pendingOp = nil
            expressionDisplay += (expressionDisplay.isEmpty ? "" : " ") + "("
            currentInput = "0"
        } else {
            if let frame = bracketStack.last {
                let innerValue = Double(currentInput) ?? 0.0
                var result = innerValue
                if let fo = firstOperand, let pop = pendingOp {
                    result = applyOp(fo, innerValue, pop) ?? innerValue
                }
                bracketStack.removeLast()
                firstOperand = frame.firstOperand
                pendingOp = frame.pendingOp
                expressionDisplay = frame.expressionSoFar + (frame.expressionSoFar.isEmpty ? "" : " ") + formatResult(result) + ")"
                currentInput = formatResult(result)
                shouldResetInput = true
            }
        }
    }

    private func handleDelete() {
        if justCalculated { handleClear(); return }
        if shouldResetInput {
            pendingOp = nil
            if let expr = expressionDisplay.split(separator: " ").dropLast().joined(separator: " ").isEmpty ? nil : expressionDisplay.split(separator: " ").dropLast().joined(separator: " ") {
                expressionDisplay = expr
            } else {
                expressionDisplay = formatResult(firstOperand ?? 0)
                firstOperand = nil
            }
            shouldResetInput = false
            currentInput = "0"
            return
        }
        if currentInput.count > 1 {
            currentInput.removeLast()
        } else {
            currentInput = "0"
        }
    }

    // MARK: - Engine helpers

    private func applyOp(_ a: Double, _ b: Double, _ op: Character) -> Double? {
        switch op {
        case "+": return a + b
        case "-": return a - b
        case "×": return a * b
        case "÷": return b == 0 ? nil : a / b
        default:  return nil
        }
    }

    private func opSymbol(_ op: Character) -> String {
        switch op {
        case "*": return "×"
        case "/": return "÷"
        default:  return String(op)
        }
    }

    private func formatResult(_ value: Double) -> String {
        if value.isInfinite || value.isNaN { return "Error" }
        if value == value.rounded() && abs(value) < 1e12 { return String(Int64(value)) }
        let s = String(format: "%.10g", value)
        return s
    }

    // MARK: - Display update

    private func updateDisplay() {
        let expr: String
        if !expressionDisplay.isEmpty {
            expr = expressionDisplay
        } else if let fo = firstOperand, let pop = pendingOp {
            expr = formatResult(fo) + " " + opSymbol(pop)
        } else {
            expr = ""
        }
        display = currentInput
        expression = expr

        switch conversionMode {
        case .currency:    updateCurrencyDisplay()
        case .distance:    updateDistanceDisplay()
        case .temperature: updateTempDisplay()
        case .tip:         updateTipDisplay()
        case .fuel:        updateFuelDisplay()
        }
    }

    // MARK: - Currency display

    private func effectiveUsdRate(for code: String, cardRates: [String: CustomRate]?, useGlobal: Bool) -> Double? {
        if let cr = cardRates, let entry = cr[code] {
            guard let base = rates[entry.base] else { return nil }
            return base * entry.rate
        }
        if useGlobal, let entry = customRates[code] {
            guard let base = rates[entry.base] else { return nil }
            return base * entry.rate
        }
        return rates[code]
    }

    private func updateCurrencyDisplay() {
        let value = Double(currentInput) ?? 0.0
        let activeCard = cardProfiles.first { $0.id == activeCardId }
        let cardRates: [String: CustomRate]? = activeCard?.customRates.isEmpty == false ? activeCard?.customRates : nil
        let useGlobal = activeCard == nil || activeCard!.useGlobalRates

        guard let fromRate = effectiveUsdRate(for: fromCurrency, cardRates: cardRates, useGlobal: useGlobal),
              let toRate   = effectiveUsdRate(for: toCurrency,   cardRates: cardRates, useGlobal: useGlobal),
              fromRate != 0 else {
            fromAmount = "\(fromCurrency) \(String(format: "%.2f", value))"
            toAmount = "\(toCurrency) 0.00"
            exchangeRateLabel = ""
            customRatePctDiff = nil
            return
        }

        let baseToValue = value * toRate / fromRate
        let usingCustom = customRates[fromCurrency] != nil || customRates[toCurrency] != nil ||
            (cardRates != nil && (cardRates![fromCurrency] != nil || cardRates![toCurrency] != nil))

        var toValue = baseToValue
        var cardLabel = ""
        if let card = activeCard {
            let percentFee = baseToValue * card.markupPercent / 100.0
            let pctStr = card.markupPercent == 0 ? "0%" : "+\(formatPct(card.markupPercent))%"
            if value > 0 && card.minFeeAmount > 0 && !card.minFeeCurrency.isEmpty,
               let minFeeRate = rates[card.minFeeCurrency], minFeeRate > 0 {
                let minFeeInTo = card.minFeeAmount * toRate / minFeeRate
                if minFeeInTo > percentFee {
                    toValue = baseToValue + minFeeInTo
                    cardLabel = "  •  💳 \(card.name) min. \(String(format: "%.2f", card.minFeeAmount)) \(card.minFeeCurrency)"
                } else {
                    toValue = baseToValue + percentFee
                    cardLabel = "  •  💳 \(card.name) \(pctStr)"
                }
            } else {
                toValue = baseToValue + percentFee
                cardLabel = "  •  💳 \(card.name) \(pctStr)"
            }
        }

        let fromName = currencyDisplayName(fromCurrency)
        let toName   = currencyDisplayName(toCurrency)
        let rate     = toRate / fromRate
        let starStr  = usingCustom ? " ★" : ""
        exchangeRateLabel = "1 \(fromCurrency) (\(fromName)) = \(String(format: "%.4f", rate)) \(toCurrency) (\(toName))\(starStr)\(cardLabel)"

        if usingCustom {
            let liveFrom = rates[fromCurrency]
            let liveTo   = rates[toCurrency]
            if let lf = liveFrom, let lt = liveTo, lf != 0 {
                let liveRate   = lt / lf
                let customRate = rate
                customRatePctDiff = (customRate - liveRate) / liveRate * 100.0
            } else { customRatePctDiff = nil }
        } else { customRatePctDiff = nil }

        fromAmount = "\(fromCurrency) \(String(format: "%.2f", value))"
        toAmount   = "\(toCurrency) \(String(format: "%.2f", toValue))"
    }

    private func formatPct(_ v: Double) -> String {
        let s = String(format: "%.2f", v)
        return s.hasSuffix("0") ? String(s.dropLast()) : s
    }

    private func currencyDisplayName(_ code: String) -> String {
        Locale.current.localizedString(forCurrencyCode: code) ?? code
    }

    // MARK: - Distance display

    private func updateDistanceDisplay() {
        let value = Double(currentInput) ?? 0.0
        let pair = distancePair
        let fromAbbr = distanceReversed ? pair.toAbbr   : pair.fromAbbr
        let toAbbr   = distanceReversed ? pair.fromAbbr : pair.toAbbr
        let factor   = distanceReversed ? 1.0 / pair.factor : pair.factor
        let converted = value * factor
        fromAmount = "\(fromAbbr) \(String(format: "%.3f", value))"
        toAmount   = "\(toAbbr) \(String(format: "%.3f", converted))"
        exchangeRateLabel = "1 \(fromAbbr) = \(String(format: "%.5f", factor)) \(toAbbr)"
        customRatePctDiff = nil
    }

    // MARK: - Temperature display

    private func updateTempDisplay() {
        let value = Double(currentInput) ?? 0.0
        let (fromAbbr, toAbbr, converted): (String, String, Double)
        switch tempUnit {
        case .celsius:
            fromAbbr = "°C"; toAbbr = "°F"
            converted = value * 9.0 / 5.0 + 32.0
        case .fahrenheit:
            fromAbbr = "°F"; toAbbr = "°C"
            converted = (value - 32.0) * 5.0 / 9.0
        }
        fromAmount = "\(fromAbbr) \(formatResult(value))"
        toAmount   = "\(toAbbr) \(formatResult(converted))"
        exchangeRateLabel = tempUnit == .celsius ? "°F = °C × 9/5 + 32" : "°C = (°F − 32) × 5/9"
        customRatePctDiff = nil
    }

    // MARK: - Tip display

    private func updateTipDisplay() {
        let bill = Double(currentInput) ?? 0.0
        let tip  = bill * tipPercent / 100.0
        let total = bill + tip
        let perPerson = tipPeopleCount > 1 ? total / Double(tipPeopleCount) : nil
        let pctStr = tipPercent == tipPercent.rounded() ? String(Int(tipPercent)) : String(format: "%.1f", tipPercent)
        fromAmount = "Bill \(String(format: "%.2f", bill))"
        if let pp = perPerson {
            toAmount = "Total \(String(format: "%.2f", total))  Each \(String(format: "%.2f", pp))"
        } else {
            toAmount = "Total \(String(format: "%.2f", total))"
        }
        let pplStr = tipPeopleCount > 1 ? " · \(tipPeopleCount) people" : ""
        exchangeRateLabel = "\(pctStr)% tip\(pplStr)"
        customRatePctDiff = nil
    }

    // MARK: - Fuel display

    private func updateFuelDisplay() {
        let value = Double(currentInput) ?? 0.0
        if fuelMode == .kwh {
            let kwhFactor = 62.1371
            let (fromAbbr, toAbbr) = fuelInputIsFirst ? ("mi/kWh", "kWh/100km") : ("kWh/100km", "mi/kWh")
            let converted = value > 0 ? kwhFactor / value : 0.0
            fromAmount = "\(fromAbbr) \(String(format: "%.2f", value))"
            toAmount   = "\(toAbbr) \(String(format: "%.2f", converted))"
            exchangeRateLabel = fuelInputIsFirst ? "62.14 ÷ mi/kWh = kWh/100km" : "62.14 ÷ kWh/100km = mi/kWh"
        } else {
            let factor = fuelUseUkGallons ? 282.481 : 235.214
            let mpgLabel = fuelUseUkGallons ? "mpg (UK)" : "mpg (US)"
            let (fromAbbr, toAbbr) = fuelInputIsFirst ? (mpgLabel, "L/100km") : ("L/100km", mpgLabel)
            let converted = value > 0 ? factor / value : 0.0
            fromAmount = "\(fromAbbr) \(String(format: "%.2f", value))"
            toAmount   = "\(toAbbr) \(String(format: "%.2f", converted))"
            exchangeRateLabel = fuelInputIsFirst ? "\(Int(factor)) ÷ mpg = L/100km" : "\(Int(factor)) ÷ L/100km = \(mpgLabel)"
        }
        customRatePctDiff = nil
    }

    // MARK: - History recording

    private func recordHistory(expression: String, result: String, inputValue: Double) {
        let activeCard = cardProfiles.first { $0.id == activeCardId }
        let (fA, tA, fC, tC) = historyAmounts(inputValue: inputValue)
        let entry = HistoryEntry(
            expression: expression,
            result: result,
            fromAmount: fA,
            fromCurrency: fC,
            toAmount: tA,
            toCurrency: tC,
            conversionMode: conversionMode.rawValue,
            cardName: activeCard?.name,
            cardMarkupPercent: activeCard?.markupPercent,
            cardMinFeeAmount: (activeCard?.minFeeAmount ?? 0) > 0 ? activeCard?.minFeeAmount : nil,
            cardMinFeeCurrency: activeCard?.minFeeCurrency.isEmpty == false ? activeCard?.minFeeCurrency : nil
        )
        history.insert(entry, at: 0)
        if history.count > 50 { history = Array(history.prefix(50)) }
        persistence.saveHistory(history)
    }

    private func historyAmounts(inputValue: Double) -> (String, String, String, String) {
        switch conversionMode {
        case .currency:
            return (fromAmount, toAmount, fromCurrency, toCurrency)
        case .distance:
            let pair = distancePair
            let fromAbbr = distanceReversed ? pair.toAbbr   : pair.fromAbbr
            let toAbbr   = distanceReversed ? pair.fromAbbr : pair.toAbbr
            return (fromAmount, toAmount, fromAbbr, toAbbr)
        case .temperature:
            let fromAbbr = tempUnit.abbr
            let toAbbr   = tempUnit == .celsius ? "°F" : "°C"
            return (fromAmount, toAmount, fromAbbr, toAbbr)
        case .tip:
            let pctStr = tipPercent == tipPercent.rounded() ? "\(Int(tipPercent))%" : "\(String(format: "%.1f", tipPercent))%"
            let pplStr = "\(tipPeopleCount)p"
            return (fromAmount, toAmount, pctStr, pplStr)
        case .fuel:
            if fuelMode == .kwh {
                let (fa, ta) = fuelInputIsFirst ? ("mi/kWh", "kWh/100km") : ("kWh/100km", "mi/kWh")
                return (fromAmount, toAmount, fa, ta)
            } else {
                let mpg = fuelUseUkGallons ? "mpg (UK)" : "mpg (US)"
                let (fa, ta) = fuelInputIsFirst ? (mpg, "L/100km") : ("L/100km", mpg)
                return (fromAmount, toAmount, fa, ta)
            }
        }
    }

    // MARK: - Mode switching

    func setConversionMode(_ mode: ConversionMode) {
        if mode == .tip, let saved = tipSavedBill {
            currentInput = formatResult(saved)
            tipSavedBill = nil
        } else if conversionMode == .tip {
            tipSavedBill = nil
        }
        conversionMode = mode
        updateDisplay()
    }

    // MARK: - Currency

    func selectFromCurrency(_ code: String) {
        fromCurrency = code
        addRecent(code)
        persistence.saveFromCurrency(code)
        updateDisplay()
    }

    func selectToCurrency(_ code: String) {
        toCurrency = code
        addRecent(code)
        persistence.saveToCurrency(code)
        updateDisplay()
    }

    func swapCurrencies() {
        swap(&fromCurrency, &toCurrency)
        persistence.saveFromCurrency(fromCurrency)
        persistence.saveToCurrency(toCurrency)
        updateDisplay()
    }

    private func addRecent(_ code: String) {
        var r = recentCurrencies.filter { $0 != code }
        r.insert(code, at: 0)
        recentCurrencies = Array(r.prefix(6))
        persistence.saveRecentCurrencies(recentCurrencies)
    }

    func fetchRates(forceRefresh: Bool = false) async {
        isRefreshing = true
        do {
            let fetched = try await service.getRates(forceRefresh: forceRefresh)
            rates = fetched
            liveRates = fetched
            availableCurrencies = fetched.keys.sorted()
            isOffline = false
            lastUpdated = "Updated just now"
        } catch {
            isOffline = rates.isEmpty
            lastUpdated = "Offline"
        }
        isRefreshing = false
        updateDisplay()
    }

    // MARK: - Custom rates (global)

    func setCustomRate(target: String, base: String, rate: Double) {
        customRates[target] = CustomRate(base: base, rate: rate)
        persistence.saveCustomRates(customRates)
        updateDisplay()
    }

    func removeCustomRate(target: String) {
        customRates.removeValue(forKey: target)
        persistence.saveCustomRates(customRates)
        updateDisplay()
    }

    // MARK: - Card profiles

    func addCardProfile(name: String, markupPercent: Double, minFeeAmount: Double,
                        minFeeCurrency: String, useGlobalRates: Bool, customRates: [String: CustomRate] = [:]) {
        let card = CardProfile(name: name, markupPercent: markupPercent,
                               minFeeAmount: minFeeAmount, minFeeCurrency: minFeeCurrency,
                               useGlobalRates: useGlobalRates, customRates: customRates)
        cardProfiles.append(card)
        persistence.saveCardProfiles(cardProfiles)
    }

    func updateCardProfile(id: String, name: String, markupPercent: Double, minFeeAmount: Double,
                           minFeeCurrency: String, useGlobalRates: Bool, customRates: [String: CustomRate]) {
        guard let idx = cardProfiles.firstIndex(where: { $0.id == id }) else { return }
        cardProfiles[idx] = CardProfile(id: id, name: name, markupPercent: markupPercent,
                                        minFeeAmount: minFeeAmount, minFeeCurrency: minFeeCurrency,
                                        useGlobalRates: useGlobalRates, customRates: customRates)
        persistence.saveCardProfiles(cardProfiles)
        updateDisplay()
    }

    func deleteCardProfile(id: String) {
        cardProfiles.removeAll { $0.id == id }
        if activeCardId == id { activeCardId = nil; persistence.saveActiveCardId(nil) }
        persistence.saveCardProfiles(cardProfiles)
        updateDisplay()
    }

    func setActiveCard(id: String?) {
        activeCardId = id
        persistence.saveActiveCardId(id)
        updateDisplay()
    }

    func setCardCustomRate(cardId: String, target: String, base: String, rate: Double) {
        guard let idx = cardProfiles.firstIndex(where: { $0.id == cardId }) else { return }
        cardProfiles[idx].customRates[target] = CustomRate(base: base, rate: rate)
        persistence.saveCardProfiles(cardProfiles)
        updateDisplay()
    }

    func deleteCardCustomRate(cardId: String, target: String) {
        guard let idx = cardProfiles.firstIndex(where: { $0.id == cardId }) else { return }
        cardProfiles[idx].customRates.removeValue(forKey: target)
        persistence.saveCardProfiles(cardProfiles)
        updateDisplay()
    }

    // MARK: - Distance

    func swapDistanceUnits() {
        distanceReversed.toggle()
        persistence.saveDistanceReversed(distanceReversed)
        updateDisplay()
    }

    func setDistancePair(_ pair: DistancePair) {
        distancePair = pair
        distanceReversed = false
        persistence.saveDistancePair(pair)
        persistence.saveDistanceReversed(false)
        updateDisplay()
    }

    func setEnabledDistancePairs(_ pairs: Set<DistancePair>) {
        let withRequired = pairs.union([.milesKm])
        enabledDistancePairs = withRequired
        if !withRequired.contains(distancePair) { setDistancePair(.milesKm) }
        persistence.saveEnabledDistancePairs(withRequired)
    }

    // MARK: - Temperature

    func swapTempUnits() {
        tempUnit = tempUnit == .celsius ? .fahrenheit : .celsius
        persistence.saveTempUnit(tempUnit)
        updateDisplay()
    }

    // MARK: - Tip

    func setTipPercent(_ percent: Double) {
        tipPercent = percent
        updateDisplay()
    }

    func setCustomTipPercent(_ percent: Double) {
        customTipPercent = percent
        tipPercent = percent
        persistence.saveCustomTipPercent(percent)
        updateDisplay()
    }

    func setDefaultTipPercent(_ percent: Double) {
        defaultTipPercent = percent
        persistence.saveDefaultTipPercent(percent)
    }

    func setTipPeople(_ count: Int) {
        tipPeopleCount = max(1, min(20, count))
        updateDisplay()
    }

    func sendTipToFX() {
        let bill = Double(currentInput) ?? 0.0
        let tip  = bill * tipPercent / 100.0
        let total = bill + tip
        let perPerson = tipPeopleCount > 1 ? total / Double(tipPeopleCount) : total
        tipSavedBill = bill
        currentInput = formatResult(perPerson)
        conversionMode = .currency
        updateDisplay()
    }

    // MARK: - Fuel

    func swapFuelDirection() {
        fuelInputIsFirst.toggle()
        persistence.saveFuelInputIsFirst(fuelInputIsFirst)
        updateDisplay()
    }

    func setFuelUseUkGallons(_ useUk: Bool) {
        fuelUseUkGallons = useUk
        persistence.saveFuelUkGallons(useUk)
        updateDisplay()
    }

    func setFuelMode(_ mode: FuelMode) {
        fuelMode = mode
        fuelInputIsFirst = true
        persistence.saveFuelMode(mode)
        persistence.saveFuelInputIsFirst(true)
        updateDisplay()
    }

    // MARK: - Size

    func setSizeCategory(_ cat: SizeCategory) {
        sizeCategory = cat
        persistence.saveSizeCategory(cat)
    }

    func setShoeIsMens(_ isMens: Bool) {
        shoeIsMens = isMens
        persistence.saveShoeIsMens(isMens)
    }

    func setShoeCountries(from: String, to: String) {
        shoeFromCountry = from; shoeToCountry = to
        persistence.saveShoeFrom(from); persistence.saveShoeTo(to)
    }

    func setWomensCountries(from: String, to: String) {
        womensFromCountry = from; womensToCountry = to
        persistence.saveWomensFrom(from); persistence.saveWomensTo(to)
    }

    func setMensCountries(from: String, to: String) {
        mensFromCountry = from; mensToCountry = to
        persistence.saveMensFrom(from); persistence.saveMensTo(to)
    }

    // MARK: - History

    func restoreHistory(_ entry: HistoryEntry) {
        currentInput = entry.result
        firstOperand = nil; pendingOp = nil
        expressionDisplay = ""; bracketStack = []
        justCalculated = true; shouldResetInput = false
        if let mode = ConversionMode(rawValue: entry.conversionMode) {
            conversionMode = mode
        }
        updateDisplay()
    }

    func clearHistory() {
        history = []
        persistence.saveHistory(history)
    }

    func deleteHistoryEntry(id: UUID) {
        history.removeAll { $0.id == id }
        persistence.saveHistory(history)
    }

    func setHistoryNote(id: UUID, note: String?) {
        guard let idx = history.firstIndex(where: { $0.id == id }) else { return }
        history[idx].note = note
        persistence.saveHistory(history)
    }

    // MARK: - Settings

    func setEnabledModes(_ modes: Set<ConversionMode>) {
        let withRequired = modes.union([.currency])
        enabledModes = withRequired
        if !withRequired.contains(conversionMode) { conversionMode = .currency; updateDisplay() }
        persistence.saveEnabledModes(withRequired)
    }

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

    func setSwapZeroDot(_ enabled: Bool) {
        swapZeroDot = enabled
        persistence.saveSwapZeroDot(enabled)
    }

    func dismissHelpHint() {
        helpHintSeen = true
        persistence.saveHelpHintSeen(true)
    }

    // MARK: - Haptic

    private func triggerHaptic() {
        guard hapticEnabled else { return }
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }
}

// MARK: - Button model

enum CalcButton: Hashable {
    case digit(String)
    case decimal
    case `operator_`(Character)
    case equals
    case clear
    case negate
    case percent
    case bracket
    case delete
}
