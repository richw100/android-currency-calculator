import SwiftUI

struct CardProfileView: View {
    let existingCard: CardProfile?
    let availableCurrencies: [String]
    let liveRates: [String: Double]
    let onSave: (String, Double, Double, String, Bool, [String: CustomRate]) -> Void

    @Environment(\.dismiss) var dismiss
    @State private var name: String
    @State private var markupText: String
    @State private var minFeeText: String
    @State private var minFeeCurrency: String
    @State private var useGlobalRates: Bool
    @State private var cardCustomRates: [String: CustomRate]
    @State private var showMinFeeCurrencyPicker = false
    @State private var showCardRateSheet = false
    @State private var editingRateTarget = ""

    init(existingCard: CardProfile?, availableCurrencies: [String], liveRates: [String: Double],
         onSave: @escaping (String, Double, Double, String, Bool, [String: CustomRate]) -> Void) {
        self.existingCard = existingCard
        self.availableCurrencies = availableCurrencies
        self.liveRates = liveRates
        self.onSave = onSave
        _name            = State(initialValue: existingCard?.name ?? "")
        _markupText      = State(initialValue: existingCard.map { String(format: "%.2f", $0.markupPercent) } ?? "0")
        _minFeeText      = State(initialValue: existingCard.map { $0.minFeeAmount > 0 ? String(format: "%.2f", $0.minFeeAmount) : "" } ?? "")
        _minFeeCurrency  = State(initialValue: existingCard?.minFeeCurrency ?? "GBP")
        _useGlobalRates  = State(initialValue: existingCard?.useGlobalRates ?? false)
        _cardCustomRates = State(initialValue: existingCard?.customRates ?? [:])
    }

    private var markupPercent: Double { Double(markupText) ?? 0 }
    private var minFeeAmount: Double  { Double(minFeeText) ?? 0 }
    private var canSave: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        NavigationView {
            Form {
                Section("Card name") {
                    TextField("e.g. Chase Sapphire", text: $name)
                }

                Section {
                    HStack {
                        Text("Fee")
                        Spacer()
                        TextField("0.00", text: $markupText)
                            .keyboardType(.decimalPad).multilineTextAlignment(.trailing).frame(width: 80)
                        Text("%")
                    }
                    HStack {
                        Text("Minimum fee")
                        Spacer()
                        TextField("0.00", text: $minFeeText)
                            .keyboardType(.decimalPad).multilineTextAlignment(.trailing).frame(width: 80)
                        Button(minFeeCurrency) { showMinFeeCurrencyPicker = true }.foregroundColor(.accentColor)
                    }
                } header: { Text("Foreign transaction fee") }
                  footer: { Text("The higher of the % fee or minimum fee is applied.").font(.caption) }

                Section {
                    Toggle("Use global custom rates", isOn: $useGlobalRates)
                } footer: {
                    Text("Falls back to your global custom rates if no card-specific rate exists for a currency.").font(.caption)
                }

                Section {
                    ForEach(cardCustomRates.keys.sorted(), id: \.self) { target in
                        if let entry = cardCustomRates[target] {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(currencyFlag(entry.base)) \(entry.base) → \(currencyFlag(target)) \(target)")
                                        .font(.system(size: 13, weight: .medium))
                                    Text("1 \(entry.base) = \(String(format: "%.4f", entry.rate)) \(target)")
                                        .font(.system(size: 14, weight: .semibold)).foregroundColor(.orange)
                                }
                                Spacer()
                                Button { editingRateTarget = target; showCardRateSheet = true } label: {
                                    Image(systemName: "pencil").foregroundColor(.secondary)
                                }.buttonStyle(.plain)
                                Button { cardCustomRates.removeValue(forKey: target) } label: {
                                    Image(systemName: "trash").foregroundColor(.red)
                                }.buttonStyle(.plain).padding(.leading, 8)
                            }
                        }
                    }
                    Button { editingRateTarget = ""; showCardRateSheet = true } label: {
                        Label("Add card rate", systemImage: "plus")
                    }
                } header: { Text("Card-specific exchange rates") }
                  footer: { Text("Override rates for specific currencies on this card.").font(.caption) }
            }
            .navigationTitle(existingCard == nil ? "Add Card" : "Edit Card")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(name.trimmingCharacters(in: .whitespaces), markupPercent,
                               minFeeAmount, minFeeCurrency, useGlobalRates, cardCustomRates)
                        dismiss()
                    }.disabled(!canSave)
                }
            }
        }
        .sheet(isPresented: $showMinFeeCurrencyPicker) {
            CurrencyPickerView(selected: minFeeCurrency, recents: [], all: availableCurrencies) { minFeeCurrency = $0 }
        }
        .sheet(isPresented: $showCardRateSheet) {
            CardRateSheet(editingTarget: editingRateTarget, availableCurrencies: availableCurrencies,
                          liveRates: liveRates) { base, target, rate in
                cardCustomRates[target] = CustomRate(base: base, rate: rate)
            }
        }
    }
}

private struct CardRateSheet: View {
    let editingTarget: String
    let availableCurrencies: [String]
    let liveRates: [String: Double]
    let onSave: (String, String, Double) -> Void

    @Environment(\.dismiss) var dismiss
    @State private var baseCode: String = "USD"
    @State private var targetCode: String = ""
    @State private var rateInput: String = ""
    @State private var showBasePicker = false
    @State private var showTargetPicker = false

    private var isEditing: Bool { !editingTarget.isEmpty }
    private var liveRate: Double? {
        guard let b = liveRates[baseCode], let t = liveRates[targetCode], b > 0 else { return nil }
        return t / b
    }
    private var canSave: Bool {
        (Double(rateInput) ?? 0) > 0 && !baseCode.isEmpty && !targetCode.isEmpty && baseCode != targetCode
    }

    var body: some View {
        NavigationView {
            Form {
                Section("Base currency") {
                    Button { if !isEditing { showBasePicker = true } } label: {
                        HStack {
                            Text(baseCode.isEmpty ? "Select…" : "\(currencyFlag(baseCode)) \(baseCode)")
                                .foregroundColor(baseCode.isEmpty ? .secondary : .primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary)
                        }
                    }.disabled(isEditing)
                }
                Section("Target currency") {
                    Button { showTargetPicker = true } label: {
                        HStack {
                            Text(targetCode.isEmpty ? "Select…" : "\(currencyFlag(targetCode)) \(targetCode)")
                                .foregroundColor(targetCode.isEmpty ? .secondary : .primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary)
                        }
                    }.disabled(isEditing)
                }
                if !baseCode.isEmpty && !targetCode.isEmpty && baseCode != targetCode {
                    Section("Rate") {
                        HStack {
                            Text("1 \(baseCode) =")
                            TextField("Rate", text: $rateInput).keyboardType(.decimalPad).multilineTextAlignment(.trailing)
                            Text(targetCode)
                        }
                        if let live = liveRate {
                            Text("Live: \(String(format: "%.4f", live))").font(.caption).foregroundColor(.secondary)
                                .onAppear { if rateInput.isEmpty { rateInput = String(format: "%.4f", live) } }
                        }
                    }
                }
            }
            .navigationTitle(isEditing ? "Edit Rate" : "Add Rate")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let rate = Double(rateInput), rate > 0 { onSave(baseCode, targetCode, rate); dismiss() }
                    }.disabled(!canSave)
                }
            }
        }
        .onAppear { if isEditing { targetCode = editingTarget } }
        .sheet(isPresented: $showBasePicker) {
            CurrencyPickerView(selected: baseCode, recents: [], all: availableCurrencies) { baseCode = $0 }
        }
        .sheet(isPresented: $showTargetPicker) {
            CurrencyPickerView(selected: targetCode, recents: [], all: availableCurrencies) { targetCode = $0 }
        }
    }
}
