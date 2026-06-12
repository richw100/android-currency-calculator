import SwiftUI

enum DarkModePref: String, CaseIterable, Codable {
    case system = "System"
    case light = "Light"
    case dark = "Dark"
}

enum AccentScheme: String, CaseIterable, Codable {
    case tealGreen = "Teal"
    case orange = "Orange"
    case blue = "Blue"
    case purple = "Purple"
    case red = "Red"

    var swatch: Color {
        switch self {
        case .tealGreen: return Color(hex: 0x00BCD4)
        case .orange:    return Color(hex: 0xFF9F0A)
        case .blue:      return Color(hex: 0x0A84FF)
        case .purple:    return Color(hex: 0xBF5AF2)
        case .red:       return Color(hex: 0xFF3B30)
        }
    }

    var operatorColor: Color { swatch }

    var equalsColor: Color {
        switch self {
        case .tealGreen: return Color(hex: 0x4CD964)
        case .orange:    return Color(hex: 0xFF9F0A)
        case .blue:      return Color(hex: 0x5AC8FA)
        case .purple:    return Color(hex: 0xFF375F)
        case .red:       return Color(hex: 0xFF9500)
        }
    }

    var equalsContentColor: Color {
        switch self {
        case .tealGreen, .blue, .red: return .black
        case .orange, .purple:        return .white
        }
    }
}

struct AppColors {
    let background: Color
    let surface: Color
    let buttonDigit: Color
    let buttonDigitContent: Color
    let buttonFunction: Color
    let buttonFunctionContent: Color
    let textPrimary: Color
    let textSecondary: Color
    let textMuted: Color
    let divider: Color
    let operatorColor: Color
    let operatorContent: Color
    let equals: Color
    let equalsContent: Color
    let fromAmountColor: Color
    let toAmountColor: Color
    let errorColor: Color
    let positiveColor: Color
    let negativeColor: Color
    let warningColor: Color
    let inputBorder: Color
    let promoColor: Color
}

func buildAppColors(isDark: Bool, scheme: AccentScheme) -> AppColors {
    if isDark {
        return AppColors(
            background:          Color(hex: 0x1C1C1E),
            surface:             Color(hex: 0x2C2C2E),
            buttonDigit:         Color(hex: 0x333333),
            buttonDigitContent:  .white,
            buttonFunction:      Color(hex: 0xA5A5A5),
            buttonFunctionContent: .black,
            textPrimary:         .white,
            textSecondary:       Color(hex: 0x8E8E93),
            textMuted:           Color(hex: 0x636366),
            divider:             Color(hex: 0x3A3A3C),
            operatorColor:       scheme.operatorColor,
            operatorContent:     .white,
            equals:              scheme.equalsColor,
            equalsContent:       scheme.equalsContentColor,
            fromAmountColor:     Color(hex: 0x0A84FF),
            toAmountColor:       Color(hex: 0x34C759),
            errorColor:          Color(hex: 0xFF453A),
            positiveColor:       Color(hex: 0x34C759),
            negativeColor:       Color(hex: 0xFF3B30),
            warningColor:        Color(hex: 0xFF9F0A),
            inputBorder:         Color(hex: 0x48484A),
            promoColor:          Color(hex: 0xFF9F0A)
        )
    } else {
        return AppColors(
            background:          Color(hex: 0xF2F2F7),
            surface:             .white,
            buttonDigit:         Color(hex: 0xE5E5EA),
            buttonDigitContent:  Color(hex: 0x1C1C1E),
            buttonFunction:      Color(hex: 0xAEAEB2),
            buttonFunctionContent: Color(hex: 0x1C1C1E),
            textPrimary:         Color(hex: 0x1C1C1E),
            textSecondary:       Color(hex: 0x636366),
            textMuted:           Color(hex: 0x8E8E93),
            divider:             Color(hex: 0xD1D1D6),
            operatorColor:       scheme.operatorColor,
            operatorContent:     .white,
            equals:              scheme.equalsColor,
            equalsContent:       scheme.equalsContentColor,
            fromAmountColor:     Color(hex: 0x007AFF),
            toAmountColor:       Color(hex: 0x248A3D),
            errorColor:          Color(hex: 0xD70015),
            positiveColor:       Color(hex: 0x248A3D),
            negativeColor:       Color(hex: 0xD70015),
            warningColor:        Color(hex: 0xB25000),
            inputBorder:         Color(hex: 0x8E8E93),
            promoColor:          Color(hex: 0x6D28D9)
        )
    }
}

private struct AppColorsKey: EnvironmentKey {
    static let defaultValue = buildAppColors(isDark: true, scheme: .tealGreen)
}

extension EnvironmentValues {
    var appColors: AppColors {
        get { self[AppColorsKey.self] }
        set { self[AppColorsKey.self] = newValue }
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red:   Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8)  & 0xFF) / 255,
            blue:  Double( hex        & 0xFF) / 255
        )
    }
}
