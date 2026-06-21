import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @Environment(\.dismiss) var dismiss
    @State private var showCustomRateSheet = false
    @State private var editingCustomRateTarget = ""
    @State private var showCardSheet = false
    @State private var editingCard: CardProfile? = nil

    var body: some View {
        NavigationView {
            Form {

                // MARK: Appearance
                Section("Appearance") {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Mode").font(.subheadline).foregroundColor(colors.textSecondary)
                        Picker("", selection: Binding(get: { vm.darkModePref }, set: { vm.setDarkMode($0) })) {
                            ForEach(DarkModePref.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                        }.pickerStyle(.segmented)
                    }.padding(.vertical, 4)

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Accent colour").font(.subheadline).foregroundColor(colors.textSecondary)
                        HStack(spacing: 16) {
                            ForEach(AccentScheme.allCases, id: \.self) { scheme in
                                Button { vm.setAccentScheme(scheme) } label: {
                                    ZStack {
                                        Circle().fill(scheme.swatch).frame(width: 36, height: 36)
                                        if vm.accentScheme == scheme {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 14, weight: .bold)).foregroundColor(.white)
                                        }
                                    }
                                    .overlay(
                                        Circle()
                                            .stroke(vm.accentScheme == scheme ? Color.primary : Color.clear, lineWidth: 2)
                                            .padding(-2)
                                    )
                                }
                            }
                        }
                    }.padding(.vertical, 4)
                }

                // MARK: General
                Section("General") {
                    Toggle("Haptic feedback", isOn: Binding(get: { vm.hapticEnabled }, set: { vm.setHaptic($0) }))
                    Toggle("Swap 0 and . keys", isOn: Binding(get: { vm.swapZeroDot }, set: { vm.setSwapZeroDot($0) }))
                }

                // MARK: Tab visibility
                Section {
                    ForEach(ConversionMode.allCases, id: \.self) { mode in
                        HStack {
                            Text(mode.settingsLabel)
                            Spacer()
                            if mode == .currency {
                                Text("Always on").foregroundColor(.secondary).font(.caption)
                            } else {
                                Toggle("", isOn: Binding(
                                    get: { vm.enabledModes.contains(mode) },
                                    set: { enabled in
                                        var modes = vm.enabledModes
                                        if enabled { modes.insert(mode) } else { modes.remove(mode) }
                                        vm.setEnabledModes(modes)
                                    }
                                )).labelsHidden()
                            }
                        }
                    }
                } header: { Text("Tab visibility") }
                  footer: { Text("Hide tabs you don't use to keep the tab bar uncluttered.").font(.caption) }

                // MARK: Card profiles
                Section {
                    ForEach(vm.cardProfiles) { card in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(card.name).font(.system(size: 15, weight: .medium))
                                Text(cardSubtitle(card)).font(.caption).foregroundColor(.secondary)
                            }
                            Spacer()
                            Button { editingCard = card; showCardSheet = true } label: {
                                Image(systemName: "pencil").foregroundColor(.secondary)
                            }.buttonStyle(.plain)
                        }
                    }
                    .onDelete { idx in idx.forEach { vm.deleteCardProfile(id: vm.cardProfiles[$0].id) } }
                    Button { editingCard = nil; showCardSheet = true } label: {
                        Label("Add card", systemImage: "plus")
                    }
                } header: { Text("Card profiles") }
                  footer: { Text("Set foreign transaction fees for each card. Activate a card from the FX tab.").font(.caption) }

                // MARK: Global custom rates
                Section {
                    ForEach(vm.customRates.keys.sorted(), id: \.self) { target in
                        if let entry = vm.customRates[target] {
                            CustomRateRow(target: target, entry: entry, liveRates: vm.liveRates,
                                onEdit: { editingCustomRateTarget = target; showCustomRateSheet = true },
                                onDelete: { vm.removeCustomRate(target: target) })
                        }
                    }
                    Button { editingCustomRateTarget = ""; showCustomRateSheet = true } label: {
                        Label("Add custom rate", systemImage: "plus")
                    }
                } header: { Text("Global custom exchange rates") }
                  footer: { Text("Override any live rate. Marked with ★ in the FX display.").font(.caption) }

                // MARK: Distance
                Section {
                    ForEach(DistancePair.allCases, id: \.self) { pair in
                        HStack {
                            Text(pair.settingsLabel)
                            Spacer()
                            if pair == .milesKm {
                                Text("Always on").foregroundColor(.secondary).font(.caption)
                            } else {
                                Toggle("", isOn: Binding(
                                    get: { vm.enabledDistancePairs.contains(pair) },
                                    set: { enabled in
                                        var pairs = vm.enabledDistancePairs
                                        if enabled { pairs.insert(pair) } else { pairs.remove(pair) }
                                        vm.setEnabledDistancePairs(pairs)
                                    }
                                )).labelsHidden()
                            }
                        }
                    }
                } header: { Text("Distance") }
                  footer: { Text("Enable or disable unit pairs shown in the Distance tab.").font(.caption) }

                // MARK: Fuel
                Section {
                    Picker("Gallons", selection: Binding(
                        get: { vm.fuelUseUkGallons },
                        set: { vm.setFuelUseUkGallons($0) }
                    )) {
                        Text("UK (imperial)").tag(true)
                        Text("US").tag(false)
                    }
                } header: { Text("Fuel economy") }
                  footer: { Text("Gallon size used in mpg ↔ L/100km conversions.").font(.caption) }

                // MARK: Tip
                Section {
                    HStack {
                        Text("Default tip")
                        Spacer()
                        Picker("", selection: Binding(
                            get: { vm.defaultTipPercent },
                            set: { vm.setDefaultTipPercent($0) }
                        )) {
                            ForEach([5.0, 10.0, 12.5, 15.0, 18.0, 20.0, 25.0], id: \.self) { pct in
                                Text("\(formatSettingsPct(pct))%").tag(pct)
                            }
                        }.pickerStyle(.menu)
                    }
                } header: { Text("Tip") }

                // MARK: Exchange rates
                Section {
                    HStack {
                        Text(vm.lastUpdated.isEmpty ? "Not yet fetched" : vm.lastUpdated)
                            .foregroundColor(.secondary).font(.system(size: 14))
                        Spacer()
                        if vm.isOffline {
                            Label("Offline", systemImage: "wifi.slash").font(.caption).foregroundColor(colors.warningColor)
                        }
                        if vm.isRefreshing { ProgressView().scaleEffect(0.7) }
                    }
                    Button("Refresh rates now") { Task { await vm.fetchRates(forceRefresh: true) } }
                        .disabled(vm.isRefreshing)
                } header: { Text("Exchange rates") }

                // MARK: About
                Section("About") {
                    HStack {
                        Text("TripCalc")
                        Spacer()
                        Text("v1.2.8").foregroundColor(.secondary).font(.caption)
                    }
                    Link("Privacy Policy",
                         destination: URL(string: "https://richw100.github.io/android-currency-calculator/privacy-policy.html")!)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
            }
        }
        .sheet(isPresented: $showCustomRateSheet) {
            CustomRateSheet(editingTarget: editingCustomRateTarget,
                            availableCurrencies: vm.availableCurrencies,
                            liveRates: vm.liveRates,
                            defaultBase: vm.fromCurrency,
                            defaultTarget: vm.toCurrency) { base, target, rate in
                vm.setCustomRate(target: target, base: base, rate: rate)
            }
        }
        .sheet(isPresented: $showCardSheet) {
            CardProfileView(existingCard: editingCard,
                            availableCurrencies: vm.availableCurrencies,
                            liveRates: vm.liveRates) { name, markup, minFee, minCur, useGlobal, rates in
                if let card = editingCard {
                    vm.updateCardProfile(id: card.id, name: name, markupPercent: markup,
                                         minFeeAmount: minFee, minFeeCurrency: minCur,
                                         useGlobalRates: useGlobal, customRates: rates)
                } else {
                    vm.addCardProfile(name: name, markupPercent: markup,
                                      minFeeAmount: minFee, minFeeCurrency: minCur,
                                      useGlobalRates: useGlobal, customRates: rates)
                }
            }
        }
    }

    private func cardSubtitle(_ card: CardProfile) -> String {
        var parts: [String] = []
        if card.markupPercent > 0 { parts.append("\(formatSettingsPct(card.markupPercent))%") }
        if card.minFeeAmount > 0 { parts.append("min \(String(format: "%.2f", card.minFeeAmount)) \(card.minFeeCurrency)") }
        return parts.isEmpty ? "No fee" : parts.joined(separator: " · ")
    }

    private func formatSettingsPct(_ v: Double) -> String {
        v == v.rounded() ? String(Int(v)) : String(format: "%.1f", v)
    }
}

// MARK: - Custom rate row

private struct CustomRateRow: View {
    let target: String; let entry: CustomRate; let liveRates: [String: Double]
    let onEdit: () -> Void; let onDelete: () -> Void

    private var liveRate: Double? {
        guard let b = liveRates[entry.base], let t = liveRates[target], b > 0 else { return nil }
        return t / b
    }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(currencyFlag(entry.base)) \(entry.base) → \(currencyFlag(target)) \(target)")
                    .font(.system(size: 13, weight: .medium))
                Text("1 \(entry.base) = \(String(format: "%.4f", entry.rate)) \(target)")
                    .font(.system(size: 14, weight: .semibold)).foregroundColor(.orange)
                if let live = liveRate {
                    Text("Live: \(String(format: "%.4f", live))").font(.caption).foregroundColor(.secondary)
                }
            }
            Spacer()
            Button(action: onEdit) { Image(systemName: "pencil").foregroundColor(.secondary) }.buttonStyle(.plain).padding(.trailing, 8)
            Button(action: onDelete) { Image(systemName: "trash").foregroundColor(.red) }.buttonStyle(.plain)
        }
    }
}

// MARK: - Custom rate sheet

private struct CustomRateSheet: View {
    let editingTarget: String; let availableCurrencies: [String]; let liveRates: [String: Double]
    let defaultBase: String; let defaultTarget: String
    let onSave: (String, String, Double) -> Void

    @Environment(\.dismiss) var dismiss
    @State private var baseCode: String
    @State private var targetCode: String
    @State private var rateInput = ""
    @State private var showBasePicker = false
    @State private var showTargetPicker = false

    init(editingTarget: String, availableCurrencies: [String], liveRates: [String: Double],
         defaultBase: String, defaultTarget: String, onSave: @escaping (String, String, Double) -> Void) {
        self.editingTarget = editingTarget; self.availableCurrencies = availableCurrencies
        self.liveRates = liveRates; self.defaultBase = defaultBase; self.defaultTarget = defaultTarget
        self.onSave = onSave
        _baseCode   = State(initialValue: defaultBase)
        _targetCode = State(initialValue: editingTarget.isEmpty ? defaultTarget : editingTarget)
    }

    private var isEditing: Bool { !editingTarget.isEmpty }
    private var liveRate: Double? {
        guard let b = liveRates[baseCode], let t = liveRates[targetCode], b > 0 else { return nil }
        return t / b
    }
    private var canSave: Bool { (Double(rateInput) ?? 0) > 0 && baseCode != targetCode && !baseCode.isEmpty && !targetCode.isEmpty }

    var body: some View {
        NavigationView {
            Form {
                Section("From (base)") {
                    Button { if !isEditing { showBasePicker = true } } label: {
                        HStack {
                            Text(baseCode.isEmpty ? "Select…" : "\(currencyFlag(baseCode)) \(baseCode)")
                                .foregroundColor(baseCode.isEmpty ? .secondary : .primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary)
                        }
                    }.disabled(isEditing)
                }
                Section("To (converted)") {
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
                            Text("Live rate: \(String(format: "%.4f", live))").font(.caption).foregroundColor(.secondary)
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
        .sheet(isPresented: $showBasePicker) {
            CurrencyPickerView(selected: baseCode, recents: [], all: availableCurrencies) { baseCode = $0 }
        }
        .sheet(isPresented: $showTargetPicker) {
            CurrencyPickerView(selected: targetCode, recents: [], all: availableCurrencies) { targetCode = $0 }
        }
    }
}
