package com.tripcalc.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class DarkModePref(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

enum class AccentScheme(
    val label: String,
    val swatch: Color,
    val operatorColor: Color,
    val equalsColor: Color,
    val equalsContentColor: Color
) {
    TEAL_GREEN("Teal",   Color(0xFF00BCD4), Color(0xFF00BCD4), Color(0xFF4CD964), Color.Black),
    ORANGE(    "Orange", Color(0xFFFF9F0A), Color(0xFFFF9F0A), Color(0xFFFF9F0A), Color.White),
    BLUE(      "Blue",   Color(0xFF0A84FF), Color(0xFF0A84FF), Color(0xFF5AC8FA), Color.Black),
    PURPLE(    "Purple", Color(0xFFBF5AF2), Color(0xFFBF5AF2), Color(0xFFFF375F), Color.White),
    RED(       "Red",    Color(0xFFFF3B30), Color(0xFFFF3B30), Color(0xFFFF9500), Color.Black)
}

data class AppColors(
    val background: Color,
    val surface: Color,
    val buttonDigit: Color,
    val buttonDigitContent: Color,
    val buttonFunction: Color,
    val buttonFunctionContent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val operator: Color,
    val operatorContent: Color,
    val equals: Color,
    val equalsContent: Color,
    val fromAmountColor: Color,
    val toAmountColor: Color,
    val errorColor: Color,
    val positiveColor: Color,
    val negativeColor: Color,
    val warningColor: Color,
    val inputBorder: Color,
    val promoColor: Color
)

fun buildAppColors(isDark: Boolean, scheme: AccentScheme): AppColors = if (isDark) AppColors(
    background          = Color(0xFF1C1C1E),
    surface             = Color(0xFF2C2C2E),
    buttonDigit         = Color(0xFF333333),
    buttonDigitContent  = Color.White,
    buttonFunction      = Color(0xFFA5A5A5),
    buttonFunctionContent = Color.Black,
    textPrimary         = Color.White,
    textSecondary       = Color(0xFF8E8E93),
    textMuted           = Color(0xFF636366),
    divider             = Color(0xFF3A3A3C),
    operator            = scheme.operatorColor,
    operatorContent     = Color.White,
    equals              = scheme.equalsColor,
    equalsContent       = scheme.equalsContentColor,
    fromAmountColor     = Color(0xFF0A84FF),
    toAmountColor       = Color(0xFF34C759),
    errorColor          = Color(0xFFFF453A),
    positiveColor       = Color(0xFF34C759),
    negativeColor       = Color(0xFFFF3B30),
    warningColor        = Color(0xFFFF9F0A),
    inputBorder         = Color(0xFF48484A),
    promoColor          = Color(0xFFFF9F0A)
) else AppColors(
    background          = Color(0xFFF2F2F7),
    surface             = Color.White,
    buttonDigit         = Color(0xFFE5E5EA),
    buttonDigitContent  = Color(0xFF1C1C1E),
    buttonFunction      = Color(0xFFAEAEB2),
    buttonFunctionContent = Color(0xFF1C1C1E),
    textPrimary         = Color(0xFF1C1C1E),
    textSecondary       = Color(0xFF636366),
    textMuted           = Color(0xFF8E8E93),
    divider             = Color(0xFFD1D1D6),
    operator            = scheme.operatorColor,
    operatorContent     = Color.White,
    equals              = scheme.equalsColor,
    equalsContent       = scheme.equalsContentColor,
    fromAmountColor     = Color(0xFF007AFF),
    toAmountColor       = Color(0xFF248A3D),
    errorColor          = Color(0xFFD70015),
    positiveColor       = Color(0xFF248A3D),
    negativeColor       = Color(0xFFD70015),
    warningColor        = Color(0xFFB25000),
    inputBorder         = Color(0xFF8E8E93),
    promoColor          = Color(0xFF6D28D9)
)

val LocalAppColors = compositionLocalOf { buildAppColors(true, AccentScheme.TEAL_GREEN) }
