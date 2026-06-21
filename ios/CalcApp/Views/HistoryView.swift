import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors
    @Environment(\.dismiss) var dismiss
    @State private var editingNoteFor: UUID? = nil
    @State private var noteText: String = ""

    var body: some View {
        NavigationView {
            Group {
                if vm.history.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "clock").font(.system(size: 48)).foregroundColor(colors.textMuted)
                        Text("No calculations yet").foregroundColor(colors.textMuted)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(colors.background)
                } else {
                    List {
                        ForEach(vm.history) { entry in
                            HistoryEntryRow(entry: entry, onEditNote: {
                                editingNoteFor = entry.id
                                noteText = entry.note ?? ""
                            })
                            .contentShape(Rectangle())
                            .onTapGesture { vm.restoreHistory(entry); dismiss() }
                            .listRowBackground(colors.surface)
                        }
                        .onDelete { indexSet in
                            indexSet.forEach { vm.deleteHistoryEntry(id: vm.history[$0].id) }
                        }
                    }
                    .listStyle(.plain).background(colors.background)
                }
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Done") { dismiss() } }
                if !vm.history.isEmpty {
                    ToolbarItem(placement: .destructiveAction) {
                        Button("Clear", role: .destructive) { vm.clearHistory() }
                    }
                }
            }
        }
        .sheet(isPresented: Binding(get: { editingNoteFor != nil }, set: { if !$0 { editingNoteFor = nil } })) {
            NoteEditorSheet(text: $noteText) { saved in
                if let id = editingNoteFor { vm.setHistoryNote(id: id, note: saved.isEmpty ? nil : saved) }
                editingNoteFor = nil
            }
        }
    }
}

private struct HistoryEntryRow: View {
    let entry: HistoryEntry
    let onEditNote: () -> Void
    @Environment(\.appColors) var colors

    private var modeEmoji: String {
        switch entry.conversionMode {
        case "currency":    return "💱"
        case "distance":    return "📏"
        case "temperature": return "🌡"
        case "tip":         return "🍽"
        case "fuel":        return "⛽"
        default:            return "💱"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Text(modeEmoji).font(.system(size: 11))
                Text(entry.expression.isEmpty ? entry.result : entry.expression)
                    .font(.system(size: 13)).foregroundColor(colors.textSecondary).lineLimit(1)
                Spacer()
                Button(action: onEditNote) {
                    Image(systemName: entry.note == nil ? "note.text.badge.plus" : "note.text")
                        .font(.system(size: 13)).foregroundColor(colors.textMuted)
                }.buttonStyle(.plain)
            }
            Text(entry.result)
                .font(.system(size: 22, weight: .medium, design: .rounded))
                .foregroundColor(colors.textPrimary)
            HStack(spacing: 4) {
                Text("\(currencyFlag(entry.fromCurrency)) \(entry.fromAmount) \(entry.fromCurrency)")
                    .foregroundColor(colors.fromAmountColor)
                Text("→").foregroundColor(colors.textMuted)
                Text("\(currencyFlag(entry.toCurrency)) \(entry.toAmount) \(entry.toCurrency)")
                    .foregroundColor(colors.toAmountColor)
            }.font(.system(size: 13))
            if let note = entry.note, !note.isEmpty {
                Text(note).font(.system(size: 12, design: .rounded)).foregroundColor(colors.textSecondary).italic()
            }
            Text(relativeTime(entry.timestamp)).font(.system(size: 11)).foregroundColor(colors.textMuted)
        }
        .padding(.vertical, 6)
    }

    private func relativeTime(_ date: Date) -> String {
        let seconds = Int(-date.timeIntervalSinceNow)
        if seconds < 60    { return "Just now" }
        if seconds < 3600  { return "\(seconds / 60)m ago" }
        if seconds < 86400 { return "\(seconds / 3600)h ago" }
        let f = DateFormatter(); f.dateFormat = "d MMM HH:mm"; return f.string(from: date)
    }
}

private struct NoteEditorSheet: View {
    @Binding var text: String
    let onSave: (String) -> Void
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 12) {
                TextEditor(text: $text)
                    .frame(minHeight: 120).padding(8)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3)))
                    .padding()
                Spacer()
            }
            .navigationTitle("Note").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Save") { onSave(text); dismiss() } }
            }
        }
    }
}
