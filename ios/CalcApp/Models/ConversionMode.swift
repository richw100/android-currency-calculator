import Foundation

enum ConversionMode: String, CaseIterable, Codable {
    case currency    = "currency"
    case distance    = "distance"
    case temperature = "temperature"
    case tip         = "tip"
    case fuel        = "fuel"

    var tabLabel: String {
        switch self {
        case .currency:    return "💱 FX"
        case .distance:    return "📏 Dist"
        case .temperature: return "🌡 Temp"
        case .tip:         return "🍽 Tip"
        case .fuel:        return "⛽ mpg"
        }
    }

    var settingsLabel: String {
        switch self {
        case .currency:    return "Currency (always on)"
        case .distance:    return "Distance converter"
        case .temperature: return "Temperature converter"
        case .tip:         return "Tip & bill splitter"
        case .fuel:        return "Fuel economy"
        }
    }
}

enum FuelMode: String, Codable {
    case mpg = "mpg"
    case kwh = "kwh"
}

enum DistancePair: String, CaseIterable, Codable {
    case milesKm    = "milesKm"
    case inchesCm   = "inchesCm"
    case feetM      = "feetM"
    case sqftSqm    = "sqftSqm"

    var fromLabel: String {
        switch self {
        case .milesKm:  return "Miles"
        case .inchesCm: return "Inches"
        case .feetM:    return "Feet"
        case .sqftSqm:  return "Sq feet"
        }
    }

    var fromAbbr: String {
        switch self {
        case .milesKm:  return "mi"
        case .inchesCm: return "in"
        case .feetM:    return "ft"
        case .sqftSqm:  return "ft²"
        }
    }

    var toLabel: String {
        switch self {
        case .milesKm:  return "Kilometres"
        case .inchesCm: return "Centimetres"
        case .feetM:    return "Metres"
        case .sqftSqm:  return "Sq metres"
        }
    }

    var toAbbr: String {
        switch self {
        case .milesKm:  return "km"
        case .inchesCm: return "cm"
        case .feetM:    return "m"
        case .sqftSqm:  return "m²"
        }
    }

    var factor: Double {
        switch self {
        case .milesKm:  return 1.60934
        case .inchesCm: return 2.54
        case .feetM:    return 0.3048
        case .sqftSqm:  return 0.092903
        }
    }

    var settingsLabel: String {
        switch self {
        case .milesKm:  return "Miles ↔ Kilometres"
        case .inchesCm: return "Inches ↔ Centimetres"
        case .feetM:    return "Feet ↔ Metres"
        case .sqftSqm:  return "Sq feet ↔ Sq metres"
        }
    }
}

enum TempUnit: String, Codable {
    case celsius    = "celsius"
    case fahrenheit = "fahrenheit"

    var abbr: String { self == .celsius ? "°C" : "°F" }
}

enum SizeCategory: String, CaseIterable, Codable {
    case shoe   = "shoe"
    case womens = "womens"
    case mens   = "mens"

    var label: String {
        switch self {
        case .shoe:   return "Shoes"
        case .womens: return "Women's"
        case .mens:   return "Men's"
        }
    }
}
