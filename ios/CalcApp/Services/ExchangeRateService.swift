import Foundation

actor ExchangeRateService {
    private let cacheTTL: TimeInterval = 86_400
    private let apiURL = URL(string: "https://open.er-api.com/v6/latest/USD")!

    func getRates(forceRefresh: Bool = false) async throws -> [String: Double] {
        if !forceRefresh, let cached = PersistenceService.shared.loadRates(), !isCacheExpired(cached) {
            return cached.rates
        }
        let (data, _) = try await URLSession.shared.data(from: apiURL)
        let response = try JSONDecoder().decode(ExchangeRateResponse.self, from: data)
        let cached = CachedRates(rates: response.rates, fetchedAt: Date())
        PersistenceService.shared.saveRates(cached)
        return response.rates
    }

    private func isCacheExpired(_ cached: CachedRates) -> Bool {
        Date().timeIntervalSince(cached.fetchedAt) > cacheTTL
    }
}
