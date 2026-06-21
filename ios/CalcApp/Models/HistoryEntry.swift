import Foundation

struct HistoryEntry: Codable, Identifiable {
    let id: UUID
    let expression: String
    let result: String
    let fromAmount: String
    let fromCurrency: String
    let toAmount: String
    let toCurrency: String
    let timestamp: Date
    let conversionMode: String
    var note: String?
    let cardName: String?
    let cardMarkupPercent: Double?
    let cardMinFeeAmount: Double?
    let cardMinFeeCurrency: String?

    init(expression: String, result: String, fromAmount: String, fromCurrency: String,
         toAmount: String, toCurrency: String, conversionMode: String = "currency",
         note: String? = nil, cardName: String? = nil, cardMarkupPercent: Double? = nil,
         cardMinFeeAmount: Double? = nil, cardMinFeeCurrency: String? = nil) {
        self.id = UUID()
        self.expression = expression
        self.result = result
        self.fromAmount = fromAmount
        self.fromCurrency = fromCurrency
        self.toAmount = toAmount
        self.toCurrency = toCurrency
        self.timestamp = Date()
        self.conversionMode = conversionMode
        self.note = note
        self.cardName = cardName
        self.cardMarkupPercent = cardMarkupPercent
        self.cardMinFeeAmount = cardMinFeeAmount
        self.cardMinFeeCurrency = cardMinFeeCurrency
    }
}
