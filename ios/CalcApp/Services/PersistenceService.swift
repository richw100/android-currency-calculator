import Foundation

final class PersistenceService {
    static let shared = PersistenceService()
    private let defaults = UserDefaults.standard
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private enum Key {
        static let rates             = "cachedRates"
        static let history           = "calcHistory"
        static let customRates       = "customRates"
        static let fromCurrency      = "fromCurrency"
        static let toCurrency        = "toCurrency"
        static let recentCurrencies  = "recentCurrencies"
        static let darkModePref      = "darkModePref"
        static let accentScheme      = "accentScheme"
        static let haptic            = "hapticEnabled"
        static let enabledModes      = "enabledModes"
        static let enabledDistPairs  = "enabledDistancePairs"
        static let distancePair      = "distancePair"
        static let distanceReversed  = "distanceReversed"
        static let tempUnit          = "tempUnit"
        static let fuelMode          = "fuelMode"
        static let fuelUkGallons     = "fuelUkGallons"
        static let fuelInputIsFirst  = "fuelInputIsFirst"
        static let tipPercent        = "tipPercent"
        static let defaultTipPercent = "defaultTipPercent"
        static let customTipPercent  = "customTipPercent"
        static let tipPeople         = "tipPeople"
        static let cardProfiles      = "cardProfiles"
        static let activeCardId      = "activeCardId"
        static let swapZeroDot       = "swapZeroDot"
        static let sizeCategory      = "sizeCategory"
        static let shoeIsMens        = "shoeIsMens"
        static let shoeFrom          = "shoeFrom"
        static let shoeTo            = "shoeTo"
        static let womensFrom        = "womensFrom"
        static let womensTo          = "womensTo"
        static let mensFrom          = "mensFrom"
        static let mensTo            = "mensTo"
        static let helpHintSeen      = "helpHintSeen"
    }

    // MARK: - Rates
    func saveRates(_ rates: CachedRates) { save(rates, key: Key.rates) }
    func loadRates() -> CachedRates? { load(CachedRates.self, key: Key.rates) }

    // MARK: - History
    func saveHistory(_ history: [HistoryEntry]) { save(history, key: Key.history) }
    func loadHistory() -> [HistoryEntry] { load([HistoryEntry].self, key: Key.history) ?? [] }

    // MARK: - Custom rates
    func saveCustomRates(_ rates: [String: CustomRate]) { save(rates, key: Key.customRates) }
    func loadCustomRates() -> [String: CustomRate] { load([String: CustomRate].self, key: Key.customRates) ?? [:] }

    // MARK: - Currency
    func saveFromCurrency(_ code: String) { defaults.set(code, forKey: Key.fromCurrency) }
    func loadFromCurrency() -> String { defaults.string(forKey: Key.fromCurrency) ?? "GBP" }
    func saveToCurrency(_ code: String) { defaults.set(code, forKey: Key.toCurrency) }
    func loadToCurrency() -> String { defaults.string(forKey: Key.toCurrency) ?? "EUR" }
    func saveRecentCurrencies(_ codes: [String]) { save(codes, key: Key.recentCurrencies) }
    func loadRecentCurrencies() -> [String] { load([String].self, key: Key.recentCurrencies) ?? [] }

    // MARK: - Appearance
    func saveDarkModePref(_ pref: DarkModePref) { defaults.set(pref.rawValue, forKey: Key.darkModePref) }
    func loadDarkModePref() -> DarkModePref {
        guard let raw = defaults.string(forKey: Key.darkModePref), let v = DarkModePref(rawValue: raw) else { return .system }
        return v
    }
    func saveAccentScheme(_ scheme: AccentScheme) { defaults.set(scheme.rawValue, forKey: Key.accentScheme) }
    func loadAccentScheme() -> AccentScheme {
        guard let raw = defaults.string(forKey: Key.accentScheme), let v = AccentScheme(rawValue: raw) else { return .tealGreen }
        return v
    }
    func saveHaptic(_ enabled: Bool) { defaults.set(enabled, forKey: Key.haptic) }
    func loadHaptic() -> Bool { defaults.object(forKey: Key.haptic) as? Bool ?? true }

    // MARK: - Modes
    func saveEnabledModes(_ modes: Set<ConversionMode>) { save(Array(modes.map { $0.rawValue }), key: Key.enabledModes) }
    func loadEnabledModes() -> Set<ConversionMode> {
        guard let arr = load([String].self, key: Key.enabledModes) else { return Set(ConversionMode.allCases) }
        return Set(arr.compactMap { ConversionMode(rawValue: $0) })
    }

    // MARK: - Distance
    func saveEnabledDistancePairs(_ pairs: Set<DistancePair>) { save(Array(pairs.map { $0.rawValue }), key: Key.enabledDistPairs) }
    func loadEnabledDistancePairs() -> Set<DistancePair> {
        guard let arr = load([String].self, key: Key.enabledDistPairs) else { return [.milesKm] }
        return Set(arr.compactMap { DistancePair(rawValue: $0) })
    }
    func saveDistancePair(_ pair: DistancePair) { defaults.set(pair.rawValue, forKey: Key.distancePair) }
    func loadDistancePair() -> DistancePair {
        guard let raw = defaults.string(forKey: Key.distancePair), let v = DistancePair(rawValue: raw) else { return .milesKm }
        return v
    }
    func saveDistanceReversed(_ v: Bool) { defaults.set(v, forKey: Key.distanceReversed) }
    func loadDistanceReversed() -> Bool { defaults.bool(forKey: Key.distanceReversed) }

    // MARK: - Temperature
    func saveTempUnit(_ unit: TempUnit) { defaults.set(unit.rawValue, forKey: Key.tempUnit) }
    func loadTempUnit() -> TempUnit {
        guard let raw = defaults.string(forKey: Key.tempUnit), let v = TempUnit(rawValue: raw) else { return .celsius }
        return v
    }

    // MARK: - Fuel
    func saveFuelMode(_ mode: FuelMode) { defaults.set(mode.rawValue, forKey: Key.fuelMode) }
    func loadFuelMode() -> FuelMode {
        guard let raw = defaults.string(forKey: Key.fuelMode), let v = FuelMode(rawValue: raw) else { return .mpg }
        return v
    }
    func saveFuelUkGallons(_ v: Bool) { defaults.set(v, forKey: Key.fuelUkGallons) }
    func loadFuelUkGallons() -> Bool { defaults.object(forKey: Key.fuelUkGallons) as? Bool ?? true }
    func saveFuelInputIsFirst(_ v: Bool) { defaults.set(v, forKey: Key.fuelInputIsFirst) }
    func loadFuelInputIsFirst() -> Bool { defaults.object(forKey: Key.fuelInputIsFirst) as? Bool ?? true }

    // MARK: - Tip
    func saveDefaultTipPercent(_ v: Double) { defaults.set(v, forKey: Key.defaultTipPercent) }
    func loadDefaultTipPercent() -> Double { defaults.object(forKey: Key.defaultTipPercent) as? Double ?? 10.0 }
    func saveCustomTipPercent(_ v: Double) { defaults.set(v, forKey: Key.customTipPercent) }
    func loadCustomTipPercent() -> Double { defaults.object(forKey: Key.customTipPercent) as? Double ?? 12.5 }
    func saveTipPeople(_ v: Int) { defaults.set(v, forKey: Key.tipPeople) }
    func loadTipPeople() -> Int { defaults.object(forKey: Key.tipPeople) as? Int ?? 1 }

    // MARK: - Cards
    func saveCardProfiles(_ cards: [CardProfile]) { save(cards, key: Key.cardProfiles) }
    func loadCardProfiles() -> [CardProfile] { load([CardProfile].self, key: Key.cardProfiles) ?? [] }
    func saveActiveCardId(_ id: String?) { if let id { defaults.set(id, forKey: Key.activeCardId) } else { defaults.removeObject(forKey: Key.activeCardId) } }
    func loadActiveCardId() -> String? { defaults.string(forKey: Key.activeCardId) }

    // MARK: - Misc
    func saveSwapZeroDot(_ v: Bool) { defaults.set(v, forKey: Key.swapZeroDot) }
    func loadSwapZeroDot() -> Bool { defaults.object(forKey: Key.swapZeroDot) as? Bool ?? true }

    // MARK: - Size
    func saveSizeCategory(_ cat: SizeCategory) { defaults.set(cat.rawValue, forKey: Key.sizeCategory) }
    func loadSizeCategory() -> SizeCategory {
        guard let raw = defaults.string(forKey: Key.sizeCategory), let v = SizeCategory(rawValue: raw) else { return .shoe }
        return v
    }
    func saveShoeIsMens(_ v: Bool) { defaults.set(v, forKey: Key.shoeIsMens) }
    func loadShoeIsMens() -> Bool { defaults.object(forKey: Key.shoeIsMens) as? Bool ?? true }
    func saveShoeFrom(_ v: String) { defaults.set(v, forKey: Key.shoeFrom) }
    func loadShoeFrom() -> String { defaults.string(forKey: Key.shoeFrom) ?? "EU" }
    func saveShoeTo(_ v: String) { defaults.set(v, forKey: Key.shoeTo) }
    func loadShoeTo() -> String { defaults.string(forKey: Key.shoeTo) ?? "UK" }
    func saveWomensFrom(_ v: String) { defaults.set(v, forKey: Key.womensFrom) }
    func loadWomensFrom() -> String { defaults.string(forKey: Key.womensFrom) ?? "US" }
    func saveWomensTo(_ v: String) { defaults.set(v, forKey: Key.womensTo) }
    func loadWomensTo() -> String { defaults.string(forKey: Key.womensTo) ?? "UK" }
    func saveMensFrom(_ v: String) { defaults.set(v, forKey: Key.mensFrom) }
    func loadMensFrom() -> String { defaults.string(forKey: Key.mensFrom) ?? "US/UK" }
    func saveMensTo(_ v: String) { defaults.set(v, forKey: Key.mensTo) }
    func loadMensTo() -> String { defaults.string(forKey: Key.mensTo) ?? "EU" }

    func saveHelpHintSeen(_ v: Bool) { defaults.set(v, forKey: Key.helpHintSeen) }
    func loadHelpHintSeen() -> Bool { defaults.bool(forKey: Key.helpHintSeen) }

    // MARK: - Helpers
    private func save<T: Encodable>(_ value: T, key: String) {
        guard let data = try? encoder.encode(value) else { return }
        defaults.set(data, forKey: key)
    }
    private func load<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? decoder.decode(type, from: data)
    }
}
