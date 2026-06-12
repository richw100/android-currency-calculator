import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            Group {
                if vm.history.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "clock")
                            .font(.system(size: 48))
                            .foregroundColor(Color(colors.textMuted))
                        Text("No calculations yet")
                            .foregroundColor(Color(colors.textMuted))
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color(colors.background))
                } else {
                    List {
                        ForEach(vm.history) { entry in
                            HistoryEntryRow(entry: entry)
                                .contentShape(Rectangle())
                                .onTapGesture {
                                    vm.restoreHistory(entry)
                                    dismiss()
                                }
                                .listRowBackground(Color(colors.surface))
                        }
                    }
                    .listStyle(.plain)
                    .background(Color(colors.background))
                }
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
                if !vm.history.isEmpty {
                    ToolbarItem(placement: .destructiveAction) {
                        Button("Clear", role: .destructive) { vm.clearHistory() }
                    }
                }
            }
        }
    }
}

private struct HistoryEntryRow: View {
    let entry: HistoryEntry
    @Environment(\.appColors) var colors

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(entry.expression.isEmpty ? entry.result : entry.expression)
                .font(.system(size: 13))
                .foregroundColor(Color(colors.textSecondary))
                .lineLimit(1)
            Text(entry.result)
                .font(.system(size: 22, weight: .medium, design: .rounded))
                .foregroundColor(Color(colors.textPrimary))
            HStack(spacing: 4) {
                Text("\(currencyFlag(entry.fromCurrency)) \(entry.fromAmount) \(entry.fromCurrency)")
                    .foregroundColor(Color(colors.fromAmountColor))
                Text("→")
                    .foregroundColor(Color(colors.textMuted))
                Text("\(currencyFlag(entry.toCurrency)) \(entry.toAmount) \(entry.toCurrency)")
                    .foregroundColor(Color(colors.toAmountColor))
            }
            .font(.system(size: 13))
            Text(relativeTime(entry.timestamp))
                .font(.system(size: 11))
                .foregroundColor(Color(colors.textMuted))
        }
        .padding(.vertical, 6)
    }

    private func relativeTime(_ date: Date) -> String {
        let seconds = Int(-date.timeIntervalSinceNow)
        if seconds < 60  { return "Just now" }
        if seconds < 3600 { return "\(seconds / 60)m ago" }
        if seconds < 86400 { return "\(seconds / 3600)h ago" }
        let f = DateFormatter()
        f.dateFormat = "d MMM HH:mm"
        return f.string(from: date)
    }
}
