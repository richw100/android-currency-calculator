import SwiftUI

struct SizeConverterView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @Environment(\.dismiss) var dismiss
    @State private var selectedSize: String = ""

    private var table: [[String: String]] {
        switch vm.sizeCategory {
        case .shoe:   return vm.shoeIsMens ? menShoeTable : womenShoeTable
        case .womens: return womensClothingTable
        case .mens:   return mensClothingTable
        }
    }

    private var countries: [SizeCountry] {
        switch vm.sizeCategory {
        case .shoe:   return shoeCountries
        case .womens: return womensCountries
        case .mens:   return mensCountries
        }
    }

    private var fromCountry: String {
        switch vm.sizeCategory {
        case .shoe:   return vm.shoeFromCountry
        case .womens: return vm.womensFromCountry
        case .mens:   return vm.mensFromCountry
        }
    }

    private var toCountry: String {
        switch vm.sizeCategory {
        case .shoe:   return vm.shoeToCountry
        case .womens: return vm.womensToCountry
        case .mens:   return vm.mensToCountry
        }
    }

    private func setCountries(from: String, to: String) {
        switch vm.sizeCategory {
        case .shoe:   vm.setShoeCountries(from: from, to: to)
        case .womens: vm.setWomensCountries(from: from, to: to)
        case .mens:   vm.setMensCountries(from: from, to: to)
        }
    }

    private var availableSizes: [String] { table.compactMap { $0[fromCountry] } }
    private var result: String? {
        selectedSize.isEmpty ? nil : lookupSize(table: table, fromKey: fromCountry, toKey: toCountry, fromValue: selectedSize)
    }
    private var fromDef: SizeCountry? { countries.first { $0.key == fromCountry } }
    private var toDef:   SizeCountry? { countries.first { $0.key == toCountry } }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {

                    // Category chips
                    HStack(spacing: 8) {
                        ForEach(SizeCategory.allCases, id: \.self) { cat in
                            Button { vm.setSizeCategory(cat); selectedSize = "" } label: {
                                Text(cat.label)
                                    .font(.system(size: 14, weight: .medium))
                                    .frame(maxWidth: .infinity).padding(.vertical, 8)
                                    .background(vm.sizeCategory == cat ? colors.operatorColor : colors.surface)
                                    .foregroundColor(vm.sizeCategory == cat ? colors.operatorContent : colors.textPrimary)
                                    .cornerRadius(8)
                            }
                        }
                    }

                    // Men's / Women's toggle (shoes only)
                    if vm.sizeCategory == .shoe {
                        HStack(spacing: 8) {
                            ForEach([(true, "Men's"), (false, "Women's")], id: \.0) { isMens, label in
                                Button { vm.setShoeIsMens(isMens); selectedSize = "" } label: {
                                    Text(label)
                                        .font(.system(size: 14, weight: .medium))
                                        .frame(maxWidth: .infinity).padding(.vertical, 7)
                                        .background(vm.shoeIsMens == isMens ? colors.buttonFunction : colors.surface)
                                        .foregroundColor(vm.shoeIsMens == isMens ? colors.buttonFunctionContent : colors.textPrimary)
                                        .cornerRadius(8)
                                }
                            }
                        }
                    }

                    // Country selectors
                    HStack(spacing: 8) {
                        SizeCountrySelectorView(label: "From", selected: fromCountry, countries: countries, colors: colors) {
                            setCountries(from: $0, to: toCountry); selectedSize = ""
                        }
                        Button { setCountries(from: toCountry, to: fromCountry); selectedSize = "" } label: {
                            Image(systemName: "arrow.left.arrow.right")
                                .foregroundColor(colors.textSecondary).frame(width: 36)
                        }
                        SizeCountrySelectorView(label: "To", selected: toCountry, countries: countries, colors: colors) {
                            setCountries(from: fromCountry, to: $0)
                        }
                    }

                    Divider()

                    Text("Select your \(fromDef?.displayName ?? fromCountry) size:")
                        .font(.system(size: 13)).foregroundColor(colors.textSecondary)

                    // Size chips (wrapping flow)
                    FlowLayout(spacing: 8) {
                        ForEach(availableSizes, id: \.self) { size in
                            Button { selectedSize = selectedSize == size ? "" : size } label: {
                                Text(size)
                                    .font(.system(size: 15, weight: selectedSize == size ? .bold : .regular))
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .background(selectedSize == size ? colors.equals : colors.surface)
                                    .foregroundColor(selectedSize == size ? colors.equalsContent : colors.textPrimary)
                                    .cornerRadius(20)
                            }
                        }
                    }

                    // Result card
                    if !selectedSize.isEmpty {
                        RoundedRectangle(cornerRadius: 16)
                            .fill(colors.surface)
                            .overlay(
                                Group {
                                    if let res = result {
                                        HStack(spacing: 12) {
                                            SizeResultColumn(label: "FROM", value: selectedSize,
                                                             standard: fromDef?.displayName ?? fromCountry,
                                                             unit: fromDef?.unit ?? "",
                                                             valueColor: colors.textPrimary)
                                            Text("→").font(.title2).foregroundColor(colors.textMuted)
                                            SizeResultColumn(label: "TO", value: res,
                                                             standard: toDef?.displayName ?? toCountry,
                                                             unit: toDef?.unit ?? "",
                                                             valueColor: colors.toAmountColor)
                                        }
                                        .padding(20)
                                    } else {
                                        Text("Size not available for this region")
                                            .font(.system(size: 14)).foregroundColor(colors.textMuted)
                                            .multilineTextAlignment(.center).padding(24)
                                    }
                                }
                            )
                            .frame(maxWidth: .infinity).fixedSize(horizontal: false, vertical: true)
                    }

                    Text("Sizes are approximate and may vary between brands")
                        .font(.system(size: 11)).foregroundColor(colors.textMuted)
                        .frame(maxWidth: .infinity).multilineTextAlignment(.center).padding(.top, 4)
                }
                .padding()
            }
            .background(colors.background.ignoresSafeArea())
            .navigationTitle("Clothing & Shoe Sizes")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } }
            }
        }
        .onChange(of: vm.sizeCategory) { _ in selectedSize = "" }
        .onChange(of: vm.shoeIsMens)   { _ in selectedSize = "" }
    }
}

private struct SizeResultColumn: View {
    let label: String; let value: String; let standard: String; let unit: String; let valueColor: Color
    var body: some View {
        VStack(spacing: 4) {
            Text(label).font(.system(size: 10, weight: .medium)).foregroundColor(.secondary)
            Text(value).font(.system(size: 48, weight: .light, design: .rounded)).foregroundColor(valueColor)
            Text(standard).font(.system(size: 13)).foregroundColor(.secondary)
            if !unit.isEmpty { Text(unit).font(.system(size: 11)).foregroundColor(.secondary) }
        }.frame(maxWidth: .infinity)
    }
}

private struct SizeCountrySelectorView: View {
    let label: String; let selected: String; let countries: [SizeCountry]; let colors: AppColors
    let onSelected: (String) -> Void
    @State private var showPicker = false

    private var displayName: String { countries.first { $0.key == selected }?.displayName ?? selected }

    var body: some View {
        Button { showPicker = true } label: {
            VStack(spacing: 2) {
                Text(label).font(.system(size: 10)).foregroundColor(colors.textMuted)
                Text(displayName).font(.system(size: 14, weight: .medium)).foregroundColor(colors.textPrimary).lineLimit(1)
            }
            .frame(maxWidth: .infinity).padding(.vertical, 8)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(colors.inputBorder, lineWidth: 1))
        }
        .sheet(isPresented: $showPicker) {
            CountryPickerSheet(selected: selected, countries: countries) { onSelected($0); showPicker = false }
        }
    }
}

private struct CountryPickerSheet: View {
    let selected: String; let countries: [SizeCountry]; let onSelected: (String) -> Void
    @Environment(\.dismiss) var dismiss
    var body: some View {
        NavigationView {
            List(countries, id: \.key) { country in
                Button {
                    onSelected(country.key); dismiss()
                } label: {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(country.displayName)
                            if !country.unit.isEmpty { Text(country.unit).font(.caption).foregroundColor(.secondary) }
                        }
                        Spacer()
                        if country.key == selected { Image(systemName: "checkmark").foregroundColor(.accentColor) }
                    }
                }.foregroundColor(.primary)
            }
            .navigationTitle("Select Standard").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
        }
    }
}

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 300
        var height: CGFloat = 0; var x: CGFloat = 0; var rowHeight: CGFloat = 0
        for sv in subviews {
            let size = sv.sizeThatFits(.unspecified)
            if x + size.width > width && x > 0 { height += rowHeight + spacing; x = 0; rowHeight = 0 }
            x += size.width + spacing; rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: width, height: height + rowHeight)
    }
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX; var y = bounds.minY; var rowHeight: CGFloat = 0
        for sv in subviews {
            let size = sv.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX && x > bounds.minX { y += rowHeight + spacing; x = bounds.minX; rowHeight = 0 }
            sv.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing; rowHeight = max(rowHeight, size.height)
        }
    }
}
