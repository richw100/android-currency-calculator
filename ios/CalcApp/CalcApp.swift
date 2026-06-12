import SwiftUI

@main
struct CalcAppApp: App {
    @StateObject private var vm = CalculatorViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(vm)
        }
    }
}

struct ContentView: View {
    @EnvironmentObject var vm: CalculatorViewModel
    @Environment(\.colorScheme) var systemScheme

    private var effectiveIsDark: Bool {
        switch vm.darkModePref {
        case .dark:   return true
        case .light:  return false
        case .system: return systemScheme == .dark
        }
    }

    private var colors: AppColors {
        buildAppColors(isDark: effectiveIsDark, scheme: vm.accentScheme)
    }

    var body: some View {
        CalculatorView()
            .environment(\.appColors, colors)
            .preferredColorScheme(vm.darkModePref == .system ? nil : effectiveIsDark ? .dark : .light)
    }
}
