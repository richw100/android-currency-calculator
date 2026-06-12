import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @Environment(\.dismiss) var dismiss
    @State private var showCustomRateSheet = false
    @State private var editingTarget = ""

    var body: some View {
        NavigationView {
            Form {
                // MARK: Appearance
                Section("Appearance") {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Mode")
                            .font(.subheadline)
                            .foregroundColor(Color(colors.textSecondary))
                        Picker("", selection: Binding(
                            get: { vm.darkModePref },
                            set: { vm.setDarkMode($0) }
                        )) {
                            ForEach(DarkModePref.allCases, id: \.self) { pref in
                                Text(pref.rawValue).tag(pref)
                            }
                        }
                        .pickerStyle(.segmented)
                    }
                    .padding(.vertical, 4)

                    VStack(alignment: .leading, spacing: 10) {
                        Text("Accent colour")
                            .font(.subheadline)
                            .foregroundColor(Color(colors.textSecondary))
                        HStack(spacing: 16) {
                            ForEach(AccentScheme.allCases, id: \.self) { scheme in
                                let isSelected = vm.accentScheme == scheme
                                Button {
                                    vm.setAccentScheme(scheme)
                                } label: {
                                    ZStack {
                                        Circle()
                                            .fill(scheme.swatch)
                                            .frame(width: 36, height: 36)
                                        if isSelected {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 14, weight: .bold))
                                                .foregroundColor(.white)
                                        }
                                    }
                                    .overlay(
                                        Circle()
                                            .stroke(isSelected ? Color.primary : Color.clear, lineWidth: 2)
                                            .padding(-2)
                                    )
                                }
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }

                // MARK: General
                Section("General") {
                    Toggle("Haptic feedback", isOn: Binding(
                        get: { vm.hapticEnabled },
                        set: { vm.setHaptic($0) }
                    ))
                }

                // MARK: Custom rates
                Section {
                    ForEach(vm.customRates.keys.sorted(), id: \.self) { target in
                        if let entry = vm.customRates[target] {
                            CustomRateRow(
                                target: target,
                                entry: entry,
                                liveRates: vm.liveRates,
                                onEdit: { editingTarget = target; showCustomRateSheet = true },
                                onDelete: { vm.removeCustomRate(target: target) }
                            )
                        }
                    }
                    Button {
                        editingTarget = ""
                        showCustomRateSheet = true
                    } label: {
                        Label("Add custom rate", systemImage: "plus")
                    }
                } header: {
                    Text("Custom exchange rates")
                } footer: {
                    Text("Override any live rate with your own value.")
                        .font(.caption)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .sheet(isPresented: $showCustomRateSheet) {
            CustomRateSheet(
                editingTarget: editingTarget,
                availableCurrencies: vm.availableCurrencies,
                liveRates: vm.liveRates,
                defaultBase: vm.fromCurrency,
                defaultTarget: vm.toCurrency,
                onSave: { base, target, rate in
                    vm.setCustomRate(target: target, base: base, rate: rate)
                }
            )
        }
    }
}

// MARK: - Custom rate row

private struct CustomRateRow: View {
    let target: String
    let entry: CustomRate
    let liveRates: [String: Double]
    let onEdit: () -> Void
    let onDelete: () -> Void

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
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.orange)
                if let live = liveRate {
                    Text("Live: \(String(format: "%.4f", live))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            Button(action: onEdit) {
                Image(systemName: "pencil").foregroundColor(.secondary)
            }
            .buttonStyle(.plain)
            .padding(.trailing, 8)
            Button(action: onDelete) {
                Image(systemName: "trash").foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
    }
}

// MARK: - Custom rate sheet

private struct CustomRateSheet: View {
    let editingTarget: String
    let availableCurrencies: [String]
    let liveRates: [String: Double]
    let defaultBase: String
    let defaultTarget: String
    let onSave: (String, String, Double) -> Void

    @Environment(\.dismiss) var dismiss
    @State private var baseCode: String
    @State private var targetCode: String
    @State private var rateInput = ""
    @State private var showBasePicker = false
    @State private var showTargetPicker = false

    init(editingTarget: String, availableCurrencies: [String], liveRates: [String: Double],
         defaultBase: String, defaultTarget: String, onSave: @escaping (String, String, Double) -> Void) {
        self.editingTarget = editingTarget
        self.availableCurrencies = availableCurrencies
        self.liveRates = liveRates
        self.defaultBase = defaultBase
        self.defaultTarget = defaultTarget
        self.onSave = onSave
        _baseCode = State(initialValue: defaultBase)
        _targetCode = State(initialValue: editingTarget.isEmpty ? defaultTarget : editingTarget)
    }

    private var isEditing: Bool { !editingTarget.isEmpty }

    private var liveRate: Double? {
        guard let b = liveRates[baseCode], let t = liveRates[targetCode], b > 0 else { return nil }
        return t / b
    }

    private var saveEnabled: Bool {
        (rateInput.toDouble() ?? 0) > 0 && baseCode != targetCode && !baseCode.isEmpty && !targetCode.isEmpty
    }

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
                    }
                    .disabled(isEditing)
                }
                Section("To (converted)") {
                    Button { showTargetPicker = true } label: {
                        HStack {
                            Text(targetCode.isEmpty ? "Select…" : "\(currencyFlag(targetCode)) \(targetCode)")
                                .foregroundColor(targetCode.isEmpty ? .secondary : .primary)
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(.secondary)
                        }
                    }
                    .disabled(isEditing)
                }
                if !baseCode.isEmpty && !targetCode.isEmpty && baseCode != targetCode {
                    Section {
                        HStack {
                            Text("1 \(baseCode) =")
                            TextField("Rate", text: $rateInput)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                            Text(targetCode)
                        }
                        if let live = liveRate {
                            Text("Live rate: \(String(format: "%.4f", live))")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .onAppear {
                                    if rateInput.isEmpty { rateInput = String(format: "%.4f", live) }
                                }
                        }
                    } header: { Text("Rate") }
                }
            }
            .navigationTitle(isEditing ? "Edit Rate" : "Add Rate")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if let rate = rateInput.toDouble(), rate > 0 {
                            onSave(baseCode, targetCode, rate)
                            dismiss()
                        }
                    }
                    .disabled(!saveEnabled)
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

private extension String {
    func toDouble() -> Double? { Double(self) }
}
