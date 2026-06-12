import Foundation

struct ExchangeRateResponse: Decodable {
    let result: String
    let rates: [String: Double]
}

struct CachedRates: Codable {
    let rates: [String: Double]
    let fetchedAt: Date
}
