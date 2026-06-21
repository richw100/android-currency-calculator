import Foundation

struct CardProfile: Codable, Identifiable {
    var id: String
    var name: String
    var markupPercent: Double
    var minFeeAmount: Double
    var minFeeCurrency: String
    var useGlobalRates: Bool
    var customRates: [String: CustomRate]

    init(id: String = UUID().uuidString, name: String, markupPercent: Double,
         minFeeAmount: Double = 0, minFeeCurrency: String = "",
         useGlobalRates: Bool = false, customRates: [String: CustomRate] = [:]) {
        self.id = id
        self.name = name
        self.markupPercent = markupPercent
        self.minFeeAmount = minFeeAmount
        self.minFeeCurrency = minFeeCurrency
        self.useGlobalRates = useGlobalRates
        self.customRates = customRates
    }
}
