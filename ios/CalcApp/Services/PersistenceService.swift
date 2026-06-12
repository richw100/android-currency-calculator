import Foundation

final class PersistenceService {
    static let shared = PersistenceService()
    private let defaults = UserDefaults.standard
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    private enum Key {
        static let rates        = "cachedRates"
        static let history      = "calcHistory"
        static let customRates  = "customRates"
        static let fromCurrency = "fromCurrency"
        static let toCurrency   = "toCurrency"
        static let recentCurrencies = "recentCurrencies"
        static let darkModePref = "darkModePref"
        static let accentScheme = "accentScheme"
        static let haptic       = "hapticEnabled"
    }

    func saveRates(_ rates: CachedRates) { save(rates, key: Key.rates) }
    func loadRates() -> CachedRates? { load(CachedRates.self, key: Key.rates) }

    func saveHistory(_ history: [HistoryEntry]) { save(history, key: Key.history) }
    func loadHistory() -> [HistoryEntry] { load([HistoryEntry].self, key: Key.history) ?? [] }

    func saveCustomRates(_ rates: [String: CustomRate]) { save(rates, key: Key.customRates) }
    func loadCustomRates() -> [String: CustomRate] { load([String: CustomRate].self, key: Key.customRates) ?? [:] }

    func saveFromCurrency(_ code: String) { defaults.set(code, forKey: Key.fromCurrency) }
    func loadFromCurrency() -> String { defaults.string(forKey: Key.fromCurrency) ?? "USD" }

    func saveToCurrency(_ code: String) { defaults.set(code, forKey: Key.toCurrency) }
    func loadToCurrency() -> String { defaults.string(forKey: Key.toCurrency) ?? "GBP" }

    func saveRecentCurrencies(_ codes: [String]) { save(codes, key: Key.recentCurrencies) }
    func loadRecentCurrencies() -> [String] { load([String].self, key: Key.recentCurrencies) ?? [] }

    func saveDarkModePref(_ pref: DarkModePref) { defaults.set(pref.rawValue, forKey: Key.darkModePref) }
    func loadDarkModePref() -> DarkModePref {
        guard let raw = defaults.string(forKey: Key.darkModePref),
              let pref = DarkModePref(rawValue: raw) else { return .system }
        return pref
    }

    func saveAccentScheme(_ scheme: AccentScheme) { defaults.set(scheme.rawValue, forKey: Key.accentScheme) }
    func loadAccentScheme() -> AccentScheme {
        guard let raw = defaults.string(forKey: Key.accentScheme),
              let scheme = AccentScheme(rawValue: raw) else { return .tealGreen }
        return scheme
    }

    func saveHaptic(_ enabled: Bool) { defaults.set(enabled, forKey: Key.haptic) }
    func loadHaptic() -> Bool { defaults.object(forKey: Key.haptic) as? Bool ?? true }

    private func save<T: Encodable>(_ value: T, key: String) {
        guard let data = try? encoder.encode(value) else { return }
        defaults.set(data, forKey: key)
    }

    private func load<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? decoder.decode(type, from: data)
    }
}
