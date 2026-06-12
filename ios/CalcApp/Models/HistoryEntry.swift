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

    init(expression: String, result: String, fromAmount: String, fromCurrency: String,
         toAmount: String, toCurrency: String) {
        self.id = UUID()
        self.expression = expression
        self.result = result
        self.fromAmount = fromAmount
        self.fromCurrency = fromCurrency
        self.toAmount = toAmount
        self.toCurrency = toCurrency
        self.timestamp = Date()
    }
}
