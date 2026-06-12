import SwiftUI

struct ButtonGrid: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors

    private let rows: [[CalcButton]] = [
        [.clear, .bracket, .percent, .operator_("+")],
        [.digit("7"), .digit("8"), .digit("9"), .operator_("-")],
        [.digit("4"), .digit("5"), .digit("6"), .operator_("×")],
        [.digit("1"), .digit("2"), .digit("3"), .operator_("÷")],
        [.negate, .digit("0"), .decimal, .equals],
    ]

    var body: some View {
        GeometryReader { geo in
            let spacing: CGFloat = 10
            let cols = 4
            let size = (geo.size.width - spacing * CGFloat(cols - 1)) / CGFloat(cols)
            VStack(spacing: spacing) {
                ForEach(rows, id: \.self) { row in
                    HStack(spacing: spacing) {
                        ForEach(row, id: \.self) { btn in
                            CalcButtonView(button: btn, size: size)
                        }
                    }
                }
            }
        }
    }
}

struct CalcButtonView: View {
    let button: CalcButton
    let size: CGFloat
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.appColors) var colors

    var body: some View {
        Button {
            vm.tap(button)
        } label: {
            Text(label)
                .font(.system(size: fontSize, weight: .medium, design: .rounded))
                .foregroundColor(contentColor)
                .frame(width: size, height: size)
                .background(backgroundColor)
                .cornerRadius(size / 2)
        }
    }

    private var label: String {
        switch button {
        case .digit(let d):     return d
        case .decimal:          return "."
        case .operator_(let o): return o
        case .equals:           return "="
        case .clear:            return vm.display == "0" && vm.expression.isEmpty ? "AC" : "C"
        case .negate:           return "+/-"
        case .percent:          return "%"
        case .bracket:          return "( )"
        case .delete:           return "⌫"
        }
    }

    private var fontSize: CGFloat {
        switch button {
        case .clear, .negate, .percent, .bracket, .delete: return size * 0.30
        default: return size * 0.36
        }
    }

    private var backgroundColor: Color {
        switch button {
        case .equals:            return colors.equals
        case .operator_:         return colors.operatorColor
        case .clear, .negate, .percent, .bracket, .delete:
                                 return colors.buttonFunction
        default:                 return colors.buttonDigit
        }
    }

    private var contentColor: Color {
        switch button {
        case .equals:            return colors.equalsContent
        case .operator_:         return colors.operatorContent
        case .clear, .negate, .percent, .bracket, .delete:
                                 return colors.buttonFunctionContent
        default:                 return colors.buttonDigitContent
        }
    }
}
