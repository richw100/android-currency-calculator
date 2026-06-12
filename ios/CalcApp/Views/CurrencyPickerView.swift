import SwiftUI

struct CurrencyPickerView: View {
    let selected: String
    let recents: [String]
    let all: [String]
    let onSelect: (String) -> Void

    @Environment(\.appColors) var colors
    @Environment(\.dismiss) var dismiss
    @State private var search = ""

    private var filteredRecents: [String] {
        guard !search.isEmpty else { return recents }
        return recents.filter { matches($0) }
    }

    private var filteredOthers: [String] {
        let recentSet = Set(recents)
        let others = all.filter { !recentSet.contains($0) }
        guard !search.isEmpty else { return others }
        return others.filter { matches($0) }
    }

    private func matches(_ code: String) -> Bool {
        code.localizedCaseInsensitiveContains(search) ||
        currencyName(code).localizedCaseInsensitiveContains(search)
    }

    var body: some View {
        NavigationView {
            List {
                if !filteredRecents.isEmpty {
                    Section("Recent") {
                        ForEach(filteredRecents, id: \.self) { code in
                            row(code)
                        }
                    }
                }
                Section("All currencies") {
                    ForEach(filteredOthers, id: \.self) { code in
                        row(code)
                    }
                }
            }
            .navigationTitle("Select currency")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $search, prompt: "Search currency")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private func row(_ code: String) -> some View {
        Button {
            onSelect(code)
            dismiss()
        } label: {
            HStack(spacing: 12) {
                Text(currencyFlag(code))
                    .font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text(code)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)
                    Text(currencyName(code))
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }
                Spacer()
                if code == selected {
                    Image(systemName: "checkmark")
                        .foregroundColor(.accentColor)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Helpers (mirrors Android currencyName / currencyFlag)

private var nameCache = [String: String]()

func currencyName(_ code: String) -> String {
    if let cached = nameCache[code] { return cached }
    let name = Locale.current.localizedString(forCurrencyCode: code) ?? code
    nameCache[code] = name
    return name
}

func currencyFlag(_ code: String) -> String {
    guard code.count == 3 else { return "🏳️" }
    let country = String(code.prefix(2))
    let base: UInt32 = 127397
    var flag = ""
    for scalar in country.unicodeScalars {
        guard let s = Unicode.Scalar(base + scalar.value) else { return "🏳️" }
        flag.append(Character(s))
    }
    return flag.isEmpty ? "🏳️" : flag
}
