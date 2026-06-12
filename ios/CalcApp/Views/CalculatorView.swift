import SwiftUI

struct CalculatorView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @State private var showFromPicker = false
    @State private var showToPicker = false
    @State private var showHistory = false
    @State private var showSettings = false

    var body: some View {
        VStack(spacing: 0) {
            // Top bar
            HStack {
                Button { showHistory = true } label: {
                    Image(systemName: "clock")
                        .font(.title3)
                        .foregroundColor(Color(colors.textSecondary))
                }
                Spacer()
                if vm.isOffline {
                    HStack(spacing: 4) {
                        Image(systemName: "wifi.slash")
                        Text("Offline")
                            .font(.caption)
                    }
                    .foregroundColor(Color(colors.warningColor))
                }
                if vm.isLoadingRates {
                    ProgressView().scaleEffect(0.7)
                }
                Spacer()
                Button { showSettings = true } label: {
                    Image(systemName: "gearshape")
                        .font(.title3)
                        .foregroundColor(Color(colors.textSecondary))
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 8)

            Spacer()

            // Display
            VStack(alignment: .trailing, spacing: 4) {
                if !vm.expression.isEmpty {
                    Text(vm.expression)
                        .font(.system(size: 18, design: .monospaced))
                        .foregroundColor(Color(colors.textSecondary))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                Text(vm.display)
                    .font(.system(size: 56, weight: .light, design: .rounded))
                    .foregroundColor(Color(colors.textPrimary))
                    .lineLimit(1)
                    .minimumScaleFactor(0.4)
                    .contextMenu {
                        Button {
                            UIPasteboard.general.string = vm.display
                        } label: { Label("Copy", systemImage: "doc.on.doc") }
                        Button {
                            if let str = UIPasteboard.general.string, let _ = Double(str) {
                                vm.display = str
                            }
                        } label: { Label("Paste", systemImage: "doc.on.clipboard") }
                    }
            }
            .frame(maxWidth: .infinity, alignment: .trailing)
            .padding(.horizontal, 20)

            Spacer().frame(height: 12)

            // Currency conversion row
            VStack(spacing: 0) {
                Divider().background(Color(colors.divider))
                HStack(spacing: 0) {
                    CurrencySide(
                        amount: vm.fromLabel,
                        currency: vm.fromCurrency,
                        amountColor: Color(colors.fromAmountColor)
                    ) { showFromPicker = true }

                    Image(systemName: "arrow.right")
                        .font(.caption)
                        .foregroundColor(Color(colors.textMuted))
                        .frame(width: 24)

                    CurrencySide(
                        amount: vm.convertedAmount,
                        currency: vm.toCurrency,
                        amountColor: Color(colors.toAmountColor)
                    ) { showToPicker = true }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                Divider().background(Color(colors.divider))
            }

            Spacer().frame(height: 12)

            // Buttons
            ButtonGrid()
                .padding(.horizontal, 12)
                .frame(height: UIScreen.main.bounds.height * 0.45)

            Spacer().frame(height: 16)
        }
        .background(Color(colors.background).ignoresSafeArea())
        .sheet(isPresented: $showFromPicker) {
            CurrencyPickerView(
                selected: vm.fromCurrency,
                recents: vm.recentCurrencies,
                all: vm.availableCurrencies
            ) { vm.selectFromCurrency($0) }
        }
        .sheet(isPresented: $showToPicker) {
            CurrencyPickerView(
                selected: vm.toCurrency,
                recents: vm.recentCurrencies,
                all: vm.availableCurrencies
            ) { vm.selectToCurrency($0) }
        }
        .sheet(isPresented: $showHistory) { HistoryView() }
        .sheet(isPresented: $showSettings) { SettingsView() }
    }
}

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
                    .foregroundColor(amountColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                HStack(spacing: 4) {
                    Text(currencyFlag(currency))
                    Text(currency)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(.primary)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10))
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button {
                UIPasteboard.general.string = amount
            } label: { Label("Copy amount", systemImage: "doc.on.doc") }
        }
    }
}
