import SwiftUI

struct CalculatorView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @State private var showFromPicker    = false
    @State private var showToPicker      = false
    @State private var showHistory       = false
    @State private var showSettings      = false
    @State private var showSizeConverter = false

    private var sortedModes: [ConversionMode] {
        ConversionMode.allCases.filter { vm.enabledModes.contains($0) }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            displayArea
            conversionRow
            modeControlRow
            ButtonGrid()
                .padding(.horizontal, 12)
                .frame(height: UIScreen.main.bounds.height * 0.43)
                .padding(.bottom, 8)
            if sortedModes.count > 1 { tabBar }
        }
        .background(colors.background.ignoresSafeArea())
        .sheet(isPresented: $showFromPicker) {
            CurrencyPickerView(selected: vm.fromCurrency, recents: vm.recentCurrencies,
                               all: vm.availableCurrencies) { vm.selectFromCurrency($0) }
        }
        .sheet(isPresented: $showToPicker) {
            CurrencyPickerView(selected: vm.toCurrency, recents: vm.recentCurrencies,
                               all: vm.availableCurrencies) { vm.selectToCurrency($0) }
        }
        .sheet(isPresented: $showHistory)       { HistoryView() }
        .sheet(isPresented: $showSettings)      { SettingsView() }
        .sheet(isPresented: $showSizeConverter) { SizeConverterView() }
    }

    // MARK: - Top bar

    private var topBar: some View {
        HStack {
            Menu {
                Button { showSizeConverter = true } label: { Label("Clothing & Shoe Sizes", systemImage: "tshirt") }
                Button { showHistory       = true } label: { Label("History",  systemImage: "clock") }
                Button { showSettings      = true } label: { Label("Settings", systemImage: "gearshape") }
            } label: {
                Image(systemName: "ellipsis.circle").font(.title3).foregroundColor(colors.textSecondary)
            }
            Spacer()
            if vm.isOffline {
                HStack(spacing: 4) {
                    Image(systemName: "wifi.slash")
                    Text("Offline").font(.caption)
                }.foregroundColor(colors.warningColor)
            }
            if vm.isRefreshing { ProgressView().scaleEffect(0.7) }
            Spacer()
            Button { showSettings = true } label: {
                Image(systemName: "gearshape").font(.title3).foregroundColor(colors.textSecondary)
            }
        }
        .padding(.horizontal, 20).padding(.top, 12).padding(.bottom, 4)
    }

    // MARK: - Display

    private var displayArea: some View {
        VStack(alignment: .trailing, spacing: 4) {
            if !vm.expression.isEmpty {
                Text(vm.expression)
                    .font(.system(size: 18, design: .monospaced))
                    .foregroundColor(colors.textSecondary)
                    .lineLimit(1).minimumScaleFactor(0.6)
            }
            Text(vm.display)
                .font(.system(size: 52, weight: .light, design: .rounded))
                .foregroundColor(colors.textPrimary)
                .lineLimit(1).minimumScaleFactor(0.4)
                .contextMenu {
                    Button { UIPasteboard.general.string = vm.display } label: {
                        Label("Copy", systemImage: "doc.on.doc")
                    }
                    Button {
                        if let s = UIPasteboard.general.string { vm.pasteValue(s) }
                    } label: { Label("Paste", systemImage: "doc.on.clipboard") }
                }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .padding(.horizontal, 20).padding(.vertical, 4)
    }

    // MARK: - Conversion row

    private var conversionRow: some View {
        VStack(spacing: 0) {
            Divider()
            Group {
                switch vm.conversionMode {
                case .currency:    currencyConversionRow
                case .tip:         tipConversionRow
                default:           genericConversionRow
                }
            }
            Divider()
            if !vm.exchangeRateLabel.isEmpty {
                Text(vm.exchangeRateLabel)
                    .font(.system(size: 11))
                    .foregroundColor(vm.customRatePctDiff != nil ? colors.warningColor : colors.textSecondary)
                    .lineLimit(1).minimumScaleFactor(0.7)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16).padding(.vertical, 3)
            }
        }
    }

    private var currencyConversionRow: some View {
        HStack(spacing: 0) {
            CurrencySide(amount: vm.fromAmount, currency: vm.fromCurrency,
                         amountColor: colors.fromAmountColor) { showFromPicker = true }
            Image(systemName: "arrow.right").font(.caption).foregroundColor(colors.textMuted).frame(width: 24)
            CurrencySide(amount: vm.toAmount, currency: vm.toCurrency,
                         amountColor: colors.toAmountColor) { showToPicker = true }
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
    }

    private var genericConversionRow: some View {
        HStack(spacing: 0) {
            AmountSide(text: vm.fromAmount, color: colors.fromAmountColor)
            Image(systemName: "arrow.right").font(.caption).foregroundColor(colors.textMuted).frame(width: 24)
            AmountSide(text: vm.toAmount, color: colors.toAmountColor)
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
    }

    private var tipConversionRow: some View {
        HStack {
            Text(vm.fromAmount)
                .font(.system(size: 17, weight: .medium, design: .rounded))
                .foregroundColor(colors.fromAmountColor)
            Spacer()
            Text(vm.toAmount)
                .font(.system(size: 17, weight: .medium, design: .rounded))
                .foregroundColor(colors.toAmountColor)
                .multilineTextAlignment(.trailing)
        }
        .padding(.horizontal, 16).padding(.vertical, 10)
    }

    // MARK: - Mode control rows

    @ViewBuilder
    private var modeControlRow: some View {
        switch vm.conversionMode {
        case .currency:    currencyModeControls
        case .distance:    distanceModeControls
        case .temperature: tempModeControls
        case .tip:         tipModeControls
        case .fuel:        fuelModeControls
        }
    }

    private var currencyModeControls: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                CardChip(label: "No card", isSelected: vm.activeCardId == nil) {
                    vm.setActiveCard(id: nil)
                }
                ForEach(vm.cardProfiles) { card in
                    CardChip(label: card.name, isSelected: vm.activeCardId == card.id) {
                        vm.setActiveCard(id: vm.activeCardId == card.id ? nil : card.id)
                    }
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 6)
        }
    }

    private var distanceModeControls: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(vm.enabledDistancePairs).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { pair in
                    ModeChip(label: "\(pair.fromAbbr)↔\(pair.toAbbr)",
                             isSelected: vm.distancePair == pair,
                             colors: colors) { vm.setDistancePair(pair) }
                }
                Button { vm.swapDistanceUnits() } label: {
                    Image(systemName: "arrow.left.arrow.right")
                        .font(.system(size: 14, weight: .medium))
                        .padding(.horizontal, 14).padding(.vertical, 7)
                        .background(colors.surface)
                        .foregroundColor(colors.textSecondary)
                        .cornerRadius(20)
                }
            }
            .padding(.horizontal, 16).padding(.vertical, 6)
        }
    }

    private var tempModeControls: some View {
        HStack {
            Spacer()
            Button { vm.swapTempUnits() } label: {
                HStack(spacing: 6) {
                    Text(vm.tempUnit == .celsius ? "°C → °F" : "°F → °C")
                        .font(.system(size: 14, weight: .medium))
                    Image(systemName: "arrow.left.arrow.right")
                }
                .padding(.horizontal, 16).padding(.vertical, 7)
                .background(colors.surface)
                .foregroundColor(colors.textSecondary)
                .cornerRadius(20)
            }
            Spacer()
        }
        .padding(.horizontal, 16).padding(.vertical, 6)
    }

    private var tipModeControls: some View {
        VStack(spacing: 6) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach([10.0, 12.5, 15.0, 18.0, 20.0], id: \.self) { pct in
                        let label = pct == pct.rounded() ? "\(Int(pct))%" : "\(String(format: "%.1f", pct))%"
                        ModeChip(label: label, isSelected: vm.tipPercent == pct, colors: colors) {
                            vm.setTipPercent(pct)
                        }
                    }
                    ModeChip(label: "\(formatTipPct(vm.customTipPercent))% ✎",
                             isSelected: vm.tipPercent == vm.customTipPercent,
                             colors: colors) { vm.setTipPercent(vm.customTipPercent) }
                }
                .padding(.horizontal, 16)
            }
            HStack(spacing: 12) {
                HStack(spacing: 8) {
                    Button { vm.setTipPeople(vm.tipPeopleCount - 1) } label: {
                        Image(systemName: "minus").frame(width: 28, height: 28)
                            .background(colors.surface).cornerRadius(14)
                            .foregroundColor(colors.textSecondary)
                    }
                    Text("\(vm.tipPeopleCount) \(vm.tipPeopleCount == 1 ? "person" : "people")")
                        .font(.system(size: 13)).foregroundColor(colors.textSecondary)
                    Button { vm.setTipPeople(vm.tipPeopleCount + 1) } label: {
                        Image(systemName: "plus").frame(width: 28, height: 28)
                            .background(colors.surface).cornerRadius(14)
                            .foregroundColor(colors.textSecondary)
                    }
                }
                Spacer()
                Button { vm.sendTipToFX() } label: {
                    Text("→ FX")
                        .font(.system(size: 13, weight: .semibold))
                        .padding(.horizontal, 14).padding(.vertical, 6)
                        .background(colors.operatorColor)
                        .foregroundColor(colors.operatorContent)
                        .cornerRadius(14)
                }
            }
            .padding(.horizontal, 16).padding(.bottom, 4)
        }
    }

    private var fuelModeControls: some View {
        HStack(spacing: 8) {
            ModeChip(label: "mpg / L", isSelected: vm.fuelMode == .mpg, colors: colors) { vm.setFuelMode(.mpg) }
            ModeChip(label: "mi/kWh",  isSelected: vm.fuelMode == .kwh, colors: colors) { vm.setFuelMode(.kwh) }
            Spacer()
            Button { vm.swapFuelDirection() } label: {
                Image(systemName: "arrow.left.arrow.right")
                    .font(.system(size: 14, weight: .medium))
                    .padding(.horizontal, 14).padding(.vertical, 7)
                    .background(colors.surface)
                    .foregroundColor(colors.textSecondary)
                    .cornerRadius(20)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 6)
    }

    // MARK: - Tab bar

    private var tabBar: some View {
        HStack(spacing: 0) {
            ForEach(sortedModes, id: \.self) { mode in
                Button { vm.setConversionMode(mode) } label: {
                    Text(mode.tabLabel)
                        .font(.system(size: 12, weight: vm.conversionMode == mode ? .semibold : .regular))
                        .foregroundColor(vm.conversionMode == mode ? colors.operatorColor : colors.textMuted)
                        .lineLimit(1).minimumScaleFactor(0.7)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(vm.conversionMode == mode ? colors.surface : Color.clear)
                }
            }
        }
        .background(colors.background)
        .overlay(Rectangle().frame(height: 1).foregroundColor(colors.divider), alignment: .top)
    }
}

// MARK: - Subviews

private struct CurrencySide: View {
    let amount: String
    let currency: String
    let amountColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 2) {
                Text(amount.isEmpty ? "—" : amount)
                    .font(.system(size: 20, weight: .medium, design: .rounded))
                    .foregroundColor(amountColor).lineLimit(1).minimumScaleFactor(0.7)
                HStack(spacing: 4) {
                    Text(currencyFlag(currency))
                    Text(currency).font(.system(size: 13, weight: .semibold)).foregroundColor(.primary)
                    Image(systemName: "chevron.down").font(.system(size: 10)).foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button { UIPasteboard.general.string = amount } label: {
                Label("Copy amount", systemImage: "doc.on.doc")
            }
        }
    }
}

private struct AmountSide: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text.isEmpty ? "—" : text)
            .font(.system(size: 18, weight: .medium, design: .rounded))
            .foregroundColor(color)
            .lineLimit(1).minimumScaleFactor(0.6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contextMenu {
                Button { UIPasteboard.general.string = text } label: {
                    Label("Copy", systemImage: "doc.on.doc")
                }
            }
    }
}

private struct ModeChip: View {
    let label: String
    let isSelected: Bool
    let colors: AppColors
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 12, weight: .medium))
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(isSelected ? colors.operatorColor : colors.surface)
                .foregroundColor(isSelected ? colors.operatorContent : colors.textSecondary)
                .cornerRadius(14)
        }
    }
}

private struct CardChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void
    @Environment(\.appColors) var colors

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                if isSelected { Image(systemName: "checkmark").font(.system(size: 10, weight: .bold)) }
                Text(label).font(.system(size: 12, weight: .medium))
            }
            .padding(.horizontal, 12).padding(.vertical, 6)
            .background(isSelected ? colors.operatorColor : colors.surface)
            .foregroundColor(isSelected ? colors.operatorContent : colors.textSecondary)
            .cornerRadius(14)
        }
    }
}

private func formatTipPct(_ v: Double) -> String {
    v == v.rounded() ? String(Int(v)) : String(format: "%.1f", v)
}
