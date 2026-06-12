import Foundation

struct CustomRate: Codable {
    let base: String
    let rate: Double

    func effectiveUsdRate(usdRates: [String: Double]) -> Double? {
        guard let baseUsd = usdRates[base], baseUsd > 0 else { return nil }
        return rate * baseUsd
    }
}
