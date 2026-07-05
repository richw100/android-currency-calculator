package com.tripcalc.app.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tripcalc.app.data.CANADA_PROVINCE_TAX_RATES
import com.tripcalc.app.data.CountryLocalisationInfo
import com.tripcalc.app.data.EU_VAT_RATES
import com.tripcalc.app.data.ExchangeRateRepository
import com.tripcalc.app.data.LocalisationRepository
import com.tripcalc.app.data.US_STATE_TAX_RATES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Currency as JavaCurrency
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

enum class ConversionMode(val tabLabel: String, val settingsLabel: String) {
    CURRENCY("💱 FX",      "Currency (always on)"),
    TIP("🧾 Bill",         "Tip & bill splitter"),
    TEMPERATURE("🌡 Temp", "Temperature converter"),
    DISTANCE("📐 Units",   "Unit converter"),
    FUEL("🚗 Car",         "Car: petrol vs electric cost comparison")
}

enum class CarField(val label: String) {
    FUEL_PRICE("Petrol price"),
    ELECTRICITY_PRICE("Electricity price"),
    MPG("Fuel economy"),
    MI_PER_KWH("EV economy")
}

enum class UnitCategory(val label: String) {
    DISTANCE("Distance"), VOLUME("Volume"), WEIGHT("Weight"), SPEED("Speed"),
    PRESSURE("Pressure"), ENERGY("Energy"), FUEL("Fuel economy")
}

enum class DistancePair(
    val fromLabel: String, val fromAbbr: String,
    val toLabel: String,   val toAbbr: String,
    val factor: Double,
    val settingsLabel: String,
    val category: UnitCategory,
    val isReciprocal: Boolean = false
) {
    MILES_KM    ("Miles",      "mi",     "Kilometres",  "km",     1.60934,  "Miles ↔ Kilometres",     UnitCategory.DISTANCE),
    INCHES_CM   ("Inches",     "in",     "Centimetres", "cm",     2.54,     "Inches ↔ Centimetres",   UnitCategory.DISTANCE),
    FEET_M      ("Feet",       "ft",     "Metres",      "m",      0.3048,   "Feet ↔ Metres",          UnitCategory.DISTANCE),
    YARDS_M     ("Yards",      "yd",     "Metres",      "m",      0.9144,   "Yards ↔ Metres",         UnitCategory.DISTANCE),
    SQFT_SQM    ("Sq feet",    "ft²",    "Sq metres",   "m²",     0.092903, "Sq feet ↔ Sq metres",    UnitCategory.DISTANCE),
    ACRES_HA    ("Acres",      "ac",     "Hectares",    "ha",     0.404686, "Acres ↔ Hectares",       UnitCategory.DISTANCE),
    UK_PINT_ML  ("UK pint",    "pt",     "Millilitres", "ml",     568.261,  "UK Pint ↔ Millilitres",  UnitCategory.VOLUME),
    US_PINT_ML  ("US pint",    "pt US",  "Millilitres", "ml",     473.176,  "US Pint ↔ Millilitres",  UnitCategory.VOLUME),
    UK_GAL_L    ("UK gallon",  "gal UK", "Litres",      "L",      4.54609,  "UK Gallon ↔ Litres",     UnitCategory.VOLUME),
    US_GAL_L    ("US gallon",  "gal US", "Litres",      "L",      3.78541,  "US Gallon ↔ Litres",     UnitCategory.VOLUME),
    UK_US_GAL   ("UK gallon",  "gal UK", "US gallon",   "gal US", 1.20095,  "UK Gallon ↔ US Gallon",  UnitCategory.VOLUME),
    FLUID_OZ_ML ("Fluid oz",   "fl oz",  "Millilitres", "ml",     29.5735,  "Fluid oz ↔ Millilitres", UnitCategory.VOLUME),
    US_CUP_ML   ("US cup",     "cup",    "Millilitres", "ml",     236.588,  "US Cup ↔ Millilitres",   UnitCategory.VOLUME),
    LB_KG       ("Pounds",     "lb",     "Kilograms",   "kg",     0.453592, "Pounds ↔ Kilograms",     UnitCategory.WEIGHT),
    STONE_LB_KG ("Stone",      "st",     "Kilograms",   "kg",     6.35029,  "Stone+lb ↔ Kilograms",   UnitCategory.WEIGHT),
    OZ_G        ("Ounces",     "oz",     "Grams",       "g",      28.3495,  "Ounces ↔ Grams",         UnitCategory.WEIGHT),
    MPH_KPH     ("mph",        "mph",    "km/h",        "km/h",   1.60934,  "mph ↔ km/h",             UnitCategory.SPEED),
    PSI_BAR     ("PSI",        "psi",    "Bar",         "bar",    0.0689476,"PSI ↔ Bar",               UnitCategory.PRESSURE),
    KCAL_KJ     ("kcal",       "kcal",   "Kilojoules",  "kJ",     4.18400,  "kcal ↔ kJ",              UnitCategory.ENERGY),
    MPG_L100KM     ("mpg",    "mpg",    "L/100km",   "L/100km",  282.481,  "mpg ↔ L/100km",       UnitCategory.FUEL, isReciprocal = true),
    MIKWH_KWH100KM ("mi/kWh", "mi/kWh", "kWh/100km", "kWh/100km", 62.1371, "mi/kWh ↔ kWh/100km",  UnitCategory.FUEL, isReciprocal = true)
}

enum class TempUnit(val label: String, val abbr: String) {
    CELSIUS("Celsius", "°C"),
    FAHRENHEIT("Fahrenheit", "°F")
}

enum class SizeCategory(val label: String) {
    SHOE("Shoes"), WOMENS("Women's"), MENS("Men's")
}

data class SizeCountry(val key: String, val displayName: String, val unit: String = "")

internal val SHOE_COUNTRIES = listOf(
    SizeCountry("EU", "EU"), SizeCountry("UK", "UK"), SizeCountry("US", "US"),
    SizeCountry("JP", "Japan", "cm"), SizeCountry("AU", "Australia"), SizeCountry("KR", "Korea", "mm")
)
internal val WOMENS_COUNTRIES = listOf(
    SizeCountry("US", "US"), SizeCountry("UK", "UK"), SizeCountry("EU", "EU"),
    SizeCountry("FR", "France"), SizeCountry("IT", "Italy"), SizeCountry("AU", "Australia"), SizeCountry("JP", "Japan")
)
internal val MENS_COUNTRIES = listOf(
    SizeCountry("US/UK", "US / UK"), SizeCountry("EU", "EU"),
    SizeCountry("JP", "Japan"), SizeCountry("AU", "Australia")
)

internal val MEN_SHOE_TABLE: List<Map<String, String>> = listOf(
    mapOf("EU" to "38",   "UK" to "5",    "US" to "6",    "JP" to "24.0", "AU" to "5",    "KR" to "240"),
    mapOf("EU" to "39",   "UK" to "6",    "US" to "7",    "JP" to "24.5", "AU" to "6",    "KR" to "245"),
    mapOf("EU" to "40",   "UK" to "6.5",  "US" to "7.5",  "JP" to "25.0", "AU" to "6.5",  "KR" to "250"),
    mapOf("EU" to "41",   "UK" to "7",    "US" to "8",    "JP" to "25.5", "AU" to "7",    "KR" to "255"),
    mapOf("EU" to "42",   "UK" to "8",    "US" to "9",    "JP" to "26.0", "AU" to "8",    "KR" to "260"),
    mapOf("EU" to "43",   "UK" to "9",    "US" to "10",   "JP" to "27.0", "AU" to "9",    "KR" to "270"),
    mapOf("EU" to "44",   "UK" to "9.5",  "US" to "10.5", "JP" to "27.5", "AU" to "9.5",  "KR" to "275"),
    mapOf("EU" to "45",   "UK" to "10.5", "US" to "11.5", "JP" to "28.0", "AU" to "10.5", "KR" to "280"),
    mapOf("EU" to "46",   "UK" to "11",   "US" to "12",   "JP" to "29.0", "AU" to "11",   "KR" to "290"),
    mapOf("EU" to "47",   "UK" to "12",   "US" to "13",   "JP" to "30.0", "AU" to "12",   "KR" to "300"),
    mapOf("EU" to "48",   "UK" to "13",   "US" to "14",   "JP" to "31.0", "AU" to "13",   "KR" to "310"),
)
internal val WOMEN_SHOE_TABLE: List<Map<String, String>> = listOf(
    mapOf("EU" to "35",   "UK" to "2",   "US" to "4",   "JP" to "21.5", "AU" to "2",   "KR" to "215"),
    mapOf("EU" to "35.5", "UK" to "2.5", "US" to "4.5", "JP" to "22.0", "AU" to "2.5", "KR" to "220"),
    mapOf("EU" to "36",   "UK" to "3",   "US" to "5",   "JP" to "22.5", "AU" to "3",   "KR" to "225"),
    mapOf("EU" to "36.5", "UK" to "3.5", "US" to "5.5", "JP" to "23.0", "AU" to "3.5", "KR" to "230"),
    mapOf("EU" to "37",   "UK" to "4",   "US" to "6",   "JP" to "23.0", "AU" to "4",   "KR" to "235"),
    mapOf("EU" to "37.5", "UK" to "4.5", "US" to "6.5", "JP" to "23.5", "AU" to "4.5", "KR" to "235"),
    mapOf("EU" to "38",   "UK" to "5",   "US" to "7",   "JP" to "24.0", "AU" to "5",   "KR" to "240"),
    mapOf("EU" to "38.5", "UK" to "5.5", "US" to "7.5", "JP" to "24.0", "AU" to "5.5", "KR" to "245"),
    mapOf("EU" to "39",   "UK" to "6",   "US" to "8",   "JP" to "24.5", "AU" to "6",   "KR" to "245"),
    mapOf("EU" to "40",   "UK" to "6.5", "US" to "8.5", "JP" to "25.0", "AU" to "6.5", "KR" to "250"),
    mapOf("EU" to "40.5", "UK" to "7",   "US" to "9",   "JP" to "25.5", "AU" to "7",   "KR" to "255"),
    mapOf("EU" to "41",   "UK" to "7.5", "US" to "9.5", "JP" to "25.5", "AU" to "7.5", "KR" to "260"),
    mapOf("EU" to "42",   "UK" to "8",   "US" to "10",  "JP" to "26.0", "AU" to "8",   "KR" to "265"),
)
internal val WOMENS_CLOTHING_TABLE: List<Map<String, String>> = listOf(
    mapOf("US" to "0",  "UK" to "4",  "EU" to "32", "FR" to "34", "IT" to "36", "AU" to "4",  "JP" to "5"),
    mapOf("US" to "2",  "UK" to "6",  "EU" to "34", "FR" to "36", "IT" to "38", "AU" to "6",  "JP" to "7"),
    mapOf("US" to "4",  "UK" to "8",  "EU" to "36", "FR" to "38", "IT" to "40", "AU" to "8",  "JP" to "9"),
    mapOf("US" to "6",  "UK" to "10", "EU" to "38", "FR" to "40", "IT" to "42", "AU" to "10", "JP" to "11"),
    mapOf("US" to "8",  "UK" to "12", "EU" to "40", "FR" to "42", "IT" to "44", "AU" to "12", "JP" to "13"),
    mapOf("US" to "10", "UK" to "14", "EU" to "42", "FR" to "44", "IT" to "46", "AU" to "14", "JP" to "15"),
    mapOf("US" to "12", "UK" to "16", "EU" to "44", "FR" to "46", "IT" to "48", "AU" to "16", "JP" to "17"),
    mapOf("US" to "14", "UK" to "18", "EU" to "46", "FR" to "48", "IT" to "50", "AU" to "18", "JP" to "19"),
    mapOf("US" to "16", "UK" to "20", "EU" to "48", "FR" to "50", "IT" to "52", "AU" to "20", "JP" to "21"),
    mapOf("US" to "18", "UK" to "22", "EU" to "50", "FR" to "52", "IT" to "54", "AU" to "22", "JP" to "23"),
    mapOf("US" to "20", "UK" to "24", "EU" to "52", "FR" to "54", "IT" to "56", "AU" to "24", "JP" to "25"),
)
internal val MENS_CLOTHING_TABLE: List<Map<String, String>> = listOf(
    mapOf("US/UK" to "XS",  "EU" to "44", "JP" to "S",   "AU" to "XS"),
    mapOf("US/UK" to "S",   "EU" to "46", "JP" to "M",   "AU" to "S"),
    mapOf("US/UK" to "M",   "EU" to "48", "JP" to "L",   "AU" to "M"),
    mapOf("US/UK" to "L",   "EU" to "50", "JP" to "XL",  "AU" to "L"),
    mapOf("US/UK" to "XL",  "EU" to "52", "JP" to "XXL", "AU" to "XL"),
    mapOf("US/UK" to "XXL", "EU" to "54", "JP" to "3XL", "AU" to "XXL"),
    mapOf("US/UK" to "3XL", "EU" to "56", "JP" to "4XL", "AU" to "3XL"),
)

fun lookupSize(table: List<Map<String, String>>, fromKey: String, toKey: String, fromValue: String): String? =
    table.find { it[fromKey] == fromValue }?.get(toKey)

data class TaxRateInfo(
    val countryCode: String,
    val countryName: String,
    val standardRate: Double,
    val reducedRates: List<Double>,
    val includedInPrice: Boolean,
    val stateCode: String? = null,
    val stateName: String? = null
)

data class TipBreakdown(
    val bill: Double,
    val taxAmount: Double,
    val tipBase: Double,
    val tipAmount: Double,
    val total: Double,
    val perPerson: Double?,
    val taxIncludedStyle: Boolean
)

fun computeTipBreakdown(
    bill: Double, tipPercent: Double, people: Int,
    taxInfo: TaxRateInfo?, taxApplied: Boolean, tipAfterTax: Boolean, noTip: Boolean
): TipBreakdown {
    val effectiveTipPct = if (noTip) 0.0 else tipPercent
    if (taxInfo == null || !taxApplied || bill == 0.0) {
        val tip = bill * effectiveTipPct / 100.0
        return TipBreakdown(bill, 0.0, bill, tip, bill + tip,
            if (people > 1) (bill + tip) / people else null, false)
    }
    val rate = taxInfo.standardRate
    return if (!taxInfo.includedInPrice) {
        val tax = bill * rate / 100.0
        val tipBase = if (tipAfterTax) bill + tax else bill
        val tip = tipBase * effectiveTipPct / 100.0
        val total = bill + tax + tip
        TipBreakdown(bill, tax, tipBase, tip, total, if (people > 1) total / people else null, false)
    } else {
        val vatContent = bill * rate / (100.0 + rate)
        val tip = bill * effectiveTipPct / 100.0
        val total = bill + tip
        TipBreakdown(bill, vatContent, bill, tip, total, if (people > 1) total / people else null, true)
    }
}

data class CardProfile(
    val id: String,
    val name: String,
    val markupPercent: Double,
    val minFeeAmount: Double = 0.0,
    val minFeeCurrency: String = "",
    val customRates: Map<String, CustomRateEntry> = emptyMap(),
    val useGlobalRates: Boolean = false
)

data class CustomRateEntry(val base: String, val rate: Double)

data class HistoryEntry(
    val display: String,
    val expression: String,
    val fromAmount: String,
    val toAmount: String,
    val fromCurrency: String,
    val toCurrency: String,
    val timestamp: Long,
    val conversionMode: String = "CURRENCY",
    val note: String? = null,
    val cardName: String? = null,
    val cardMarkupPercent: Double? = null,
    val cardMinFeeAmount: Double? = null,
    val cardMinFeeCurrency: String? = null
)

data class CalculatorUiState(
    val display: String = "0",
    val expression: String = "",
    val toCurrency: String = "EUR",
    val fromCurrency: String = "GBP",
    val toAmount: String = "EUR 0.00",
    val fromAmount: String = "GBP 0.00",
    val exchangeRateLabel: String = "",
    val lastUpdated: String = "",
    val isRefreshing: Boolean = false,
    val availableCurrencies: List<String> = emptyList(),
    val recentCurrencies: List<String> = emptyList(),
    val customRates: Map<String, CustomRateEntry> = emptyMap(),
    val liveRates: Map<String, Double> = emptyMap(),
    val customRatePctDiff: Double? = null,
    val hapticEnabled: Boolean = true,
    val isError: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val isOffline: Boolean = false,
    val helpHintSeen: Boolean = true,
    val darkModePref: DarkModePref = DarkModePref.SYSTEM,
    val accentScheme: AccentScheme = AccentScheme.TEAL_GREEN,
    val conversionMode: ConversionMode = ConversionMode.CURRENCY,
    // Stack of modes visited before the current one, for back-button navigation between chips
    val modeHistory: List<ConversionMode> = emptyList(),
    val distancePair: DistancePair = DistancePair.MILES_KM,
    val distanceReversed: Boolean = false,
    val enabledDistancePairs: Set<DistancePair> = setOf(DistancePair.MILES_KM, DistancePair.MPG_L100KM, DistancePair.MIKWH_KWH100KM),
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val defaultTipPercent: Double = 10.0,
    val tipPercent: Double = 10.0,
    val customTipPercent: Double = 12.5,
    val tipPeopleCount: Int = 1,
    val fuelUseUkGallons: Boolean = true,
    val carTabUnlocked: Boolean = false,
    val carUnlockPromptPending: Boolean = false,
    val carActiveField: CarField = CarField.MPG,
    val carTargetField: CarField? = null,
    val carUseMetric: Boolean = false,
    val carFuelPricePerLitre: String = "",
    val carElectricityPricePerKwh: String = "",
    val carMpg: String = "",
    val carMiPerKwh: String = "",
    val swapZeroDot: Boolean = true,
    val enabledModes: Set<ConversionMode> = ConversionMode.entries.toSet() - ConversionMode.FUEL,
    val cardProfiles: List<CardProfile> = emptyList(),
    val activeCardId: String? = null,
    val sizeCategory: SizeCategory = SizeCategory.SHOE,
    val shoeIsMens: Boolean = true,
    val shoeFromCountry: String = "EU",
    val shoeToCountry: String = "UK",
    val womensFromCountry: String = "US",
    val womensToCountry: String = "UK",
    val mensFromCountry: String = "US/UK",
    val mensToCountry: String = "EU",
    val exchangeRate: Double? = null,
    val ocrConvertEnabled: Boolean = true,
    val taxEnabled: Boolean = false,
    val taxCountryCode: String? = null,
    val taxStateCode: String? = null,
    val taxRateOverride: Double? = null,
    val currentTaxInfo: TaxRateInfo? = null,
    val taxApplied: Boolean = true,
    val tipAfterTax: Boolean = false,
    val noTip: Boolean = false,
    val tipPresets: List<Double> = listOf(10.0, 15.0, 20.0),
    val stoneLbPart: Int = 0,
    val distanceUseLitres: Boolean = false,
    val localisationCountryCode: String = "GB",
    val localisationInfo: CountryLocalisationInfo? = null,
    val localisationLoading: Boolean = false,
    val localisationError: Boolean = false,
    val localisationRecentCountries: List<String> = emptyList()
)

sealed class CalculatorAction {
    data class Digit(val value: Int) : CalculatorAction()
    object Decimal : CalculatorAction()
    object Clear : CalculatorAction()
    object Delete : CalculatorAction()
    object Calculate : CalculatorAction()
    object Percent : CalculatorAction()
    object SmartBracket : CalculatorAction()
    object OpenBracket : CalculatorAction()
    object CloseBracket : CalculatorAction()
    object RefreshRates : CalculatorAction()
    object SwapCurrencies : CalculatorAction()
    data class Operation(val symbol: Char) : CalculatorAction()
    data class SetToCurrency(val code: String) : CalculatorAction()
    data class SetFromCurrency(val code: String) : CalculatorAction()
    data class SetCustomRate(val target: String, val base: String, val rate: Double) : CalculatorAction()
    data class ClearCustomRate(val code: String) : CalculatorAction()
    data class SetHaptic(val enabled: Boolean) : CalculatorAction()
    data class PasteValue(val text: String) : CalculatorAction()
    data class RestoreHistory(val entry: HistoryEntry) : CalculatorAction()
    object ClearHistory : CalculatorAction()
    data class SetDarkMode(val pref: DarkModePref) : CalculatorAction()
    data class SetAccentScheme(val scheme: AccentScheme) : CalculatorAction()
    data class SetConversionMode(val mode: ConversionMode) : CalculatorAction()
    object PopConversionMode : CalculatorAction()
    object SwapDistanceUnits : CalculatorAction()
    data class SetDistancePair(val pair: DistancePair) : CalculatorAction()
    data class SetEnabledDistancePairs(val pairs: Set<DistancePair>) : CalculatorAction()
    data class DeleteHistoryEntry(val timestamp: Long) : CalculatorAction()
    data class SetHistoryNote(val timestamp: Long, val note: String?) : CalculatorAction()
    object SwapTempUnits : CalculatorAction()
    data class SetTipPercent(val percent: Double) : CalculatorAction()
    data class SetTipPeople(val count: Int) : CalculatorAction()
    data class SetCustomTipPercent(val percent: Double) : CalculatorAction()
    data class SetFuelUseUkGallons(val useUk: Boolean) : CalculatorAction()
    data class SetCarActiveField(val field: CarField) : CalculatorAction()
    object ResetCarFields : CalculatorAction()
    object ConfirmCarUnlockToggle : CalculatorAction()
    object DismissCarUnlockPrompt : CalculatorAction()
    object ToggleCarUseMetric : CalculatorAction()
    data class SetSwapZeroDot(val enabled: Boolean) : CalculatorAction()
    data class SetEnabledModes(val modes: Set<ConversionMode>) : CalculatorAction()
    data class SetDefaultTipPercent(val percent: Double) : CalculatorAction()
    data class AddCardProfile(val name: String, val markupPercent: Double, val minFeeAmount: Double, val minFeeCurrency: String, val useGlobalRates: Boolean, val customRates: Map<String, CustomRateEntry> = emptyMap()) : CalculatorAction()
    data class UpdateCardProfile(val id: String, val name: String, val markupPercent: Double, val minFeeAmount: Double, val minFeeCurrency: String, val useGlobalRates: Boolean, val customRates: Map<String, CustomRateEntry>) : CalculatorAction()
    data class DeleteCardProfile(val id: String) : CalculatorAction()
    data class SetActiveCard(val id: String?) : CalculatorAction()
    data class SetCardCustomRate(val cardId: String, val target: String, val base: String, val rate: Double) : CalculatorAction()
    data class DeleteCardCustomRate(val cardId: String, val target: String) : CalculatorAction()
    object SendTipShareToFX : CalculatorAction()
    data class SetSizeCategory(val category: SizeCategory) : CalculatorAction()
    data class SetShoeIsMens(val isMens: Boolean) : CalculatorAction()
    data class SetShoeCountries(val from: String, val to: String) : CalculatorAction()
    data class SetWomensCountries(val from: String, val to: String) : CalculatorAction()
    data class SetMensCountries(val from: String, val to: String) : CalculatorAction()
    object DismissHelpHint : CalculatorAction()
    data class SetOcrConvertEnabled(val enabled: Boolean) : CalculatorAction()
    data class SetTaxEnabled(val enabled: Boolean) : CalculatorAction()
    data class SetTaxCountry(val countryCode: String) : CalculatorAction()
    data class SetTaxState(val stateCode: String) : CalculatorAction()
    data class SetTaxRateOverride(val rate: Double?) : CalculatorAction()
    data class SetTaxApplied(val applied: Boolean) : CalculatorAction()
    data class SetTipAfterTax(val afterTax: Boolean) : CalculatorAction()
    data class SetNoTip(val noTip: Boolean) : CalculatorAction()
    data class SetTipPreset(val index: Int, val percent: Double) : CalculatorAction()
    object IncrLbPart : CalculatorAction()
    object DecrLbPart : CalculatorAction()
    object ToggleDistanceLitres : CalculatorAction()
    data class SetLocalisationCountry(val code: String) : CalculatorAction()
    object RefreshLocalisation : CalculatorAction()
}

private data class BracketState(val firstOperand: Double?, val pendingOp: Char?)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExchangeRateRepository(application)
    private val localisationRepository = LocalisationRepository(application)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var currentInput = "0"
    private var firstOperand: Double? = null
    private var pendingOp: Char? = null
    private var justCalculated = false
    private var shouldResetInput = false
    private var tipSavedBill: String? = null
    private var lastCalcExpression = ""
    private val secretCarCode = "98741235"

    private fun restoreTipBillIfNeeded(mode: ConversionMode) {
        if (mode == ConversionMode.TIP && tipSavedBill != null) {
            currentInput = tipSavedBill!!
            firstOperand = null; pendingOp = null
            bracketStack.clear(); expressionDisplay.clear()
            justCalculated = true; shouldResetInput = false
        }
    }

    // Receipt image for the OCR overlay — held here rather than in saved instance
    // state (too large for a Bundle) so it survives rotation
    var ocrBitmap by mutableStateOf<Bitmap?>(null)

    // Each ( pushes the current (firstOperand, pendingOp) so ) can restore it
    private val bracketStack = mutableListOf<BracketState>()

    // Drives the small expression line above the main display
    private val expressionDisplay = StringBuilder()

    private var rates: Map<String, Double> = emptyMap()

    init {
        viewModelScope.launch {
            val (to, from) = repository.loadCurrencyPrefs()
            val recents = repository.loadRecentCurrencies()
            val customRates = repository.loadCustomRates()
            val hapticEnabled = repository.loadHapticEnabled()
            val history = repository.loadHistory()
            val darkModePref = repository.loadDarkModePref()
            val accentScheme = repository.loadAccentScheme()
            val defaultTipPercent = repository.loadDefaultTipPercent()
            val customTipPercent = repository.loadCustomTipPercent()
            val fuelUseUkGallons = repository.loadFuelUseUkGallons()
            val carTabUnlocked = repository.loadCarTabUnlocked()
            val carUseMetric = repository.loadCarUseMetric()
            val swapZeroDot = repository.loadSwapZeroDot()
            val enabledModes = repository.loadEnabledModes()
            val enabledDistancePairs = repository.loadEnabledDistancePairs()
            val cardProfiles = repository.loadCardProfiles()
            val activeCardId = repository.loadActiveCardId()
            val sizeCategory = repository.loadSizeCategory()
            val shoeIsMens = repository.loadShoeIsMens()
            val (shoeFrom, shoeTo) = repository.loadShoeCountries()
            val (womensFrom, womensTo) = repository.loadWomensCountries()
            val (mensFrom, mensTo) = repository.loadMensCountries()
            val helpHintSeen = repository.loadHelpHintSeen()
            val ocrConvertEnabled = repository.loadOcrConvertEnabled()
            val taxEnabled = repository.loadTaxEnabled()
            val taxCountryCode = repository.loadTaxCountryCode()
            val taxStateCode = repository.loadTaxStateCode()
            val taxRateOverride = repository.loadTaxRateOverride()
            val taxApplied = repository.loadTaxApplied()
            val tipAfterTax = repository.loadTipAfterTax()
            val noTip = repository.loadNoTip()
            val tipPresets = repository.loadTipPresets()
            val locCountryCode = localisationRepository.loadCountryCode()
            val locRecentCountries = localisationRepository.loadRecentCountries()
            _uiState.update { it.copy(toCurrency = to, fromCurrency = from, recentCurrencies = recents, customRates = customRates, hapticEnabled = hapticEnabled, history = history, darkModePref = darkModePref, accentScheme = accentScheme, defaultTipPercent = defaultTipPercent, tipPercent = defaultTipPercent, customTipPercent = customTipPercent, fuelUseUkGallons = fuelUseUkGallons, carTabUnlocked = carTabUnlocked, carUseMetric = carUseMetric, swapZeroDot = swapZeroDot, enabledModes = enabledModes, enabledDistancePairs = enabledDistancePairs, cardProfiles = cardProfiles, activeCardId = activeCardId, sizeCategory = sizeCategory, shoeIsMens = shoeIsMens, shoeFromCountry = shoeFrom, shoeToCountry = shoeTo, womensFromCountry = womensFrom, womensToCountry = womensTo, mensFromCountry = mensFrom, mensToCountry = mensTo, helpHintSeen = helpHintSeen, ocrConvertEnabled = ocrConvertEnabled, taxEnabled = taxEnabled, taxCountryCode = taxCountryCode, taxStateCode = taxStateCode, taxRateOverride = taxRateOverride, taxApplied = taxApplied, tipAfterTax = tipAfterTax, noTip = noTip, tipPresets = tipPresets, localisationCountryCode = locCountryCode, localisationRecentCountries = locRecentCountries) }
            refreshCurrentTaxInfo()
            fetchRates(forceRefresh = false)
        }
    }

    private fun refreshCurrentTaxInfo() {
        val s = _uiState.value
        val info = resolveTaxInfo(s.taxCountryCode, s.taxStateCode, s.taxRateOverride)
        _uiState.update { it.copy(currentTaxInfo = info) }
    }

    private fun resolveTaxInfo(countryCode: String?, stateCode: String?, override: Double?): TaxRateInfo? {
        if (countryCode == null) return null
        return when (countryCode) {
            "US" -> {
                if (stateCode == null) return null
                val (stateName, bundledRate) = US_STATE_TAX_RATES[stateCode] ?: return null
                val rate = override ?: bundledRate
                TaxRateInfo("US", "United States", rate, emptyList(), false, stateCode, stateName)
            }
            "CA" -> {
                if (stateCode == null) return null
                val (provinceName, bundledRate) = CANADA_PROVINCE_TAX_RATES[stateCode] ?: return null
                val rate = override ?: bundledRate
                TaxRateInfo("CA", "Canada", rate, emptyList(), false, stateCode, provinceName)
            }
            else -> {
                val (countryName, bundledRate, reduced) = EU_VAT_RATES[countryCode] ?: return null
                val rate = override ?: bundledRate
                // includedInPrice = false is deliberate for Japan: pricing there is transitioning
                // and sales tax is sometimes not included, so treat it as added-at-checkout and
                // let the user toggle the tax chip based on what they see in the moment.
                TaxRateInfo(countryCode, countryName, rate, reduced, false)
            }
        }
    }

    private fun updateRecents(code: String) {
        val updated = (_uiState.value.recentCurrencies.toMutableList()
            .also { it.remove(code) }
            .also { it.add(0, code) })
            .take(5)
        _uiState.update { it.copy(recentCurrencies = updated) }
        viewModelScope.launch { repository.saveRecentCurrencies(updated) }
    }

    private fun fetchRates(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val result = repository.getRates("USD", forceRefresh)
                rates = result.rates
                val updated = repository.getLastUpdated()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        lastUpdated = "Updated $updated",
                        availableCurrencies = rates.keys.sorted(),
                        liveRates = rates,
                        isOffline = result.isStale
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, lastUpdated = "Offline", isOffline = true) }
            }
            updateCurrencyDisplay()
        }
    }

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Digit -> handleDigit(action.value)
            is CalculatorAction.Decimal -> handleDecimal()
            is CalculatorAction.Clear -> handleClear()
            is CalculatorAction.Delete -> handleDelete()
            is CalculatorAction.Calculate -> {
                handleCalculate()
                updateDisplay()
                if (!_uiState.value.isError && _uiState.value.display != "0" &&
                    _uiState.value.conversionMode != ConversionMode.FUEL) recordHistory()
                return
            }
            is CalculatorAction.Percent -> handlePercent()
            is CalculatorAction.SmartBracket -> {
                val atStart = firstOperand == null && pendingOp == null
                    && currentInput == "0" && bracketStack.isEmpty() && !justCalculated
                if (shouldResetInput || atStart) handleOpenBracket()
                else if (bracketStack.isNotEmpty()) handleCloseBracket()
            }
            is CalculatorAction.OpenBracket -> handleOpenBracket()
            is CalculatorAction.CloseBracket -> handleCloseBracket()
            is CalculatorAction.Operation -> handleOperation(action.symbol)
            is CalculatorAction.RefreshRates -> { fetchRates(forceRefresh = true); return }
            is CalculatorAction.SwapCurrencies -> {
                val s = _uiState.value
                _uiState.update { it.copy(toCurrency = s.fromCurrency, fromCurrency = s.toCurrency) }
                viewModelScope.launch { repository.saveCurrencyPrefs(s.fromCurrency, s.toCurrency) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetToCurrency -> {
                _uiState.update { it.copy(toCurrency = action.code) }
                updateRecents(action.code)
                viewModelScope.launch { repository.saveCurrencyPrefs(action.code, _uiState.value.fromCurrency) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetFromCurrency -> {
                _uiState.update { it.copy(fromCurrency = action.code) }
                updateRecents(action.code)
                viewModelScope.launch { repository.saveCurrencyPrefs(_uiState.value.toCurrency, action.code) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetCustomRate -> {
                val updated = _uiState.value.customRates.toMutableMap()
                    .also { it[action.target] = CustomRateEntry(action.base, action.rate) }
                _uiState.update { it.copy(customRates = updated) }
                viewModelScope.launch { repository.saveCustomRates(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.ClearCustomRate -> {
                val updated = _uiState.value.customRates.toMutableMap().also { it.remove(action.code) }
                _uiState.update { it.copy(customRates = updated) }
                viewModelScope.launch { repository.saveCustomRates(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetHaptic -> {
                _uiState.update { it.copy(hapticEnabled = action.enabled) }
                viewModelScope.launch { repository.saveHapticEnabled(action.enabled) }
                return
            }
            is CalculatorAction.PasteValue -> {
                val cleaned = action.text.replace(",", "").trim()
                if (cleaned.toDoubleOrNull() != null) {
                    currentInput = cleaned
                    firstOperand = null; pendingOp = null
                    bracketStack.clear(); expressionDisplay.clear()
                    justCalculated = false; shouldResetInput = false
                    _uiState.update { it.copy(isError = false) }
                    updateDisplay()
                }
                return
            }
            is CalculatorAction.RestoreHistory -> {
                val entry = action.entry
                currentInput = entry.display
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
                justCalculated = true; shouldResetInput = false
                val entryMode = ConversionMode.entries.find { it.name == entry.conversionMode }
                if (entryMode == ConversionMode.CURRENCY) {
                    _uiState.update { it.copy(
                        isError = false,
                        fromCurrency = entry.fromCurrency,
                        toCurrency = entry.toCurrency,
                        conversionMode = ConversionMode.CURRENCY,
                        modeHistory = emptyList()
                    ) }
                    viewModelScope.launch { repository.saveCurrencyPrefs(entry.toCurrency, entry.fromCurrency) }
                } else {
                    // Non-currency entries store unit labels (e.g. "10%", "mi") in the currency
                    // fields — never write those into the currency state or prefs
                    val targetMode = entryMode?.takeIf { it in _uiState.value.enabledModes }
                    _uiState.update { it.copy(
                        isError = false,
                        conversionMode = targetMode ?: it.conversionMode,
                        modeHistory = emptyList()
                    ) }
                }
                updateDisplay(); return
            }
            is CalculatorAction.ClearHistory -> {
                _uiState.update { it.copy(history = emptyList()) }
                viewModelScope.launch { repository.saveHistory(emptyList()) }
                return
            }
            is CalculatorAction.SetDarkMode -> {
                _uiState.update { it.copy(darkModePref = action.pref) }
                viewModelScope.launch { repository.saveDarkModePref(action.pref) }
                return
            }
            is CalculatorAction.SetAccentScheme -> {
                _uiState.update { it.copy(accentScheme = action.scheme) }
                viewModelScope.launch { repository.saveAccentScheme(action.scheme) }
                return
            }
            is CalculatorAction.SetConversionMode -> {
                restoreTipBillIfNeeded(action.mode)
                _uiState.update {
                    val newHistory = if (action.mode != it.conversionMode) it.modeHistory + it.conversionMode else it.modeHistory
                    it.copy(conversionMode = action.mode, modeHistory = newHistory)
                }
                updateDisplay(); return
            }
            CalculatorAction.PopConversionMode -> {
                val history = _uiState.value.modeHistory
                val prev = history.lastOrNull() ?: return
                restoreTipBillIfNeeded(prev)
                _uiState.update { it.copy(conversionMode = prev, modeHistory = history.dropLast(1)) }
                updateDisplay(); return
            }
            is CalculatorAction.SwapDistanceUnits -> {
                _uiState.update { it.copy(distanceReversed = !it.distanceReversed) }
                updateDisplay(); return
            }
            is CalculatorAction.SetDistancePair -> {
                _uiState.update { it.copy(distancePair = action.pair, distanceReversed = false, stoneLbPart = 0) }
                updateDisplay(); return
            }
            CalculatorAction.IncrLbPart -> {
                _uiState.update { it.copy(stoneLbPart = (it.stoneLbPart + 1).coerceAtMost(13)) }
                updateDisplay(); return
            }
            CalculatorAction.DecrLbPart -> {
                _uiState.update { it.copy(stoneLbPart = (it.stoneLbPart - 1).coerceAtLeast(0)) }
                updateDisplay(); return
            }
            CalculatorAction.ToggleDistanceLitres -> {
                _uiState.update { it.copy(distanceUseLitres = !it.distanceUseLitres) }
                updateDisplay(); return
            }
            is CalculatorAction.SetEnabledDistancePairs -> {
                val pairs = (action.pairs + DistancePair.MILES_KM + DistancePair.MPG_L100KM + DistancePair.MIKWH_KWH100KM)
                val current = _uiState.value.distancePair
                val newPair = if (current in pairs) current else DistancePair.MILES_KM
                _uiState.update { it.copy(enabledDistancePairs = pairs, distancePair = newPair) }
                viewModelScope.launch { repository.saveEnabledDistancePairs(pairs) }
                updateDisplay(); return
            }
            is CalculatorAction.SwapTempUnits -> {
                val newUnit = if (_uiState.value.tempUnit == TempUnit.CELSIUS) TempUnit.FAHRENHEIT else TempUnit.CELSIUS
                _uiState.update { it.copy(tempUnit = newUnit) }
                updateDisplay(); return
            }
            is CalculatorAction.SetTipPercent -> {
                _uiState.update { it.copy(tipPercent = action.percent) }
                updateDisplay(); return
            }
            is CalculatorAction.SetTipPeople -> {
                _uiState.update { it.copy(tipPeopleCount = action.count.coerceIn(1, 20)) }
                updateDisplay(); return
            }
            is CalculatorAction.SetCustomTipPercent -> {
                _uiState.update { it.copy(customTipPercent = action.percent, tipPercent = action.percent) }
                viewModelScope.launch { repository.saveCustomTipPercent(action.percent) }
                updateDisplay(); return
            }
            is CalculatorAction.SetFuelUseUkGallons -> {
                _uiState.update { it.copy(fuelUseUkGallons = action.useUk) }
                viewModelScope.launch { repository.saveFuelUseUkGallons(action.useUk) }
                updateDisplay(); return
            }
            is CalculatorAction.SetCarActiveField -> {
                val s = _uiState.value
                if (action.field == s.carActiveField) return
                val stored = when (action.field) {
                    CarField.FUEL_PRICE -> s.carFuelPricePerLitre
                    CarField.ELECTRICITY_PRICE -> s.carElectricityPricePerKwh
                    CarField.MPG -> s.carMpg
                    CarField.MI_PER_KWH -> s.carMiPerKwh
                }
                currentInput = stored.ifEmpty { "0" }
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
                justCalculated = true; shouldResetInput = false
                _uiState.update { it.copy(carActiveField = action.field) }
                updateDisplay(); return
            }
            CalculatorAction.ResetCarFields -> {
                currentInput = "0"
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
                justCalculated = false; shouldResetInput = false
                _uiState.update { it.copy(
                    carFuelPricePerLitre = "", carElectricityPricePerKwh = "",
                    carMpg = "", carMiPerKwh = "", carTargetField = null
                ) }
                updateDisplay(); return
            }
            CalculatorAction.ConfirmCarUnlockToggle -> {
                val unlocked = !_uiState.value.carTabUnlocked
                _uiState.update { it.copy(carTabUnlocked = unlocked, carUnlockPromptPending = false) }
                viewModelScope.launch { repository.saveCarTabUnlocked(unlocked) }
                return
            }
            CalculatorAction.DismissCarUnlockPrompt -> {
                _uiState.update { it.copy(carUnlockPromptPending = false) }
                return
            }
            CalculatorAction.ToggleCarUseMetric -> {
                val s = _uiState.value
                val newMetric = !s.carUseMetric
                val gallonLitres = if (s.fuelUseUkGallons) 4.54609 else 3.78541
                val mpgFactor = if (s.fuelUseUkGallons) 282.481 else 235.214
                fun reciprocal(v: String, factor: Double) =
                    v.toDoubleOrNull()?.takeIf { it > 0 }?.let { formatResult(factor / it) } ?: v
                val newMpg = reciprocal(s.carMpg, mpgFactor)
                val newMiPerKwh = reciprocal(s.carMiPerKwh, 62.1371)
                val newFuelPrice = s.carFuelPricePerLitre.toDoubleOrNull()
                    ?.let { formatResult(if (newMetric) it / gallonLitres else it * gallonLitres) }
                    ?: s.carFuelPricePerLitre
                currentInput = when (s.carActiveField) {
                    CarField.MPG -> newMpg
                    CarField.MI_PER_KWH -> newMiPerKwh
                    CarField.FUEL_PRICE -> newFuelPrice
                    CarField.ELECTRICITY_PRICE -> currentInput
                }.ifEmpty { "0" }
                _uiState.update { it.copy(
                    carUseMetric = newMetric, carMpg = newMpg,
                    carMiPerKwh = newMiPerKwh, carFuelPricePerLitre = newFuelPrice
                ) }
                viewModelScope.launch { repository.saveCarUseMetric(newMetric) }
                updateDisplay(); return
            }
            is CalculatorAction.SetSwapZeroDot -> {
                _uiState.update { it.copy(swapZeroDot = action.enabled) }
                viewModelScope.launch { repository.saveSwapZeroDot(action.enabled) }
                updateDisplay(); return
            }
            is CalculatorAction.SetEnabledModes -> {
                val modes = (action.modes + ConversionMode.CURRENCY)
                val currentMode = _uiState.value.conversionMode
                val newMode = if (currentMode in modes) currentMode else ConversionMode.CURRENCY
                _uiState.update { it.copy(enabledModes = modes, conversionMode = newMode, modeHistory = it.modeHistory.filter { m -> m in modes }) }
                viewModelScope.launch { repository.saveEnabledModes(modes) }
                updateDisplay(); return
            }
            is CalculatorAction.SetDefaultTipPercent -> {
                _uiState.update { it.copy(defaultTipPercent = action.percent, tipPercent = action.percent) }
                viewModelScope.launch { repository.saveDefaultTipPercent(action.percent) }
                updateDisplay(); return
            }
            is CalculatorAction.AddCardProfile -> {
                val profile = CardProfile(UUID.randomUUID().toString(), action.name, action.markupPercent, action.minFeeAmount, action.minFeeCurrency, customRates = action.customRates, useGlobalRates = action.useGlobalRates)
                val updated = _uiState.value.cardProfiles + profile
                _uiState.update { it.copy(cardProfiles = updated) }
                viewModelScope.launch { repository.saveCardProfiles(updated) }
                return
            }
            is CalculatorAction.UpdateCardProfile -> {
                val updated = _uiState.value.cardProfiles.map {
                    if (it.id == action.id) it.copy(name = action.name, markupPercent = action.markupPercent, minFeeAmount = action.minFeeAmount, minFeeCurrency = action.minFeeCurrency, useGlobalRates = action.useGlobalRates, customRates = action.customRates) else it
                }
                _uiState.update { it.copy(cardProfiles = updated) }
                viewModelScope.launch { repository.saveCardProfiles(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.DeleteCardProfile -> {
                val updated = _uiState.value.cardProfiles.filter { it.id != action.id }
                val newActiveId = if (_uiState.value.activeCardId == action.id) null else _uiState.value.activeCardId
                _uiState.update { it.copy(cardProfiles = updated, activeCardId = newActiveId) }
                viewModelScope.launch { repository.saveCardProfiles(updated); repository.saveActiveCardId(newActiveId) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetActiveCard -> {
                _uiState.update { it.copy(activeCardId = action.id) }
                viewModelScope.launch { repository.saveActiveCardId(action.id) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetCardCustomRate -> {
                val updated = _uiState.value.cardProfiles.map { card ->
                    if (card.id == action.cardId) card.copy(customRates = card.customRates + (action.target to CustomRateEntry(action.base, action.rate)))
                    else card
                }
                _uiState.update { it.copy(cardProfiles = updated) }
                viewModelScope.launch { repository.saveCardProfiles(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.DeleteCardCustomRate -> {
                val updated = _uiState.value.cardProfiles.map { card ->
                    if (card.id == action.cardId) card.copy(customRates = card.customRates - action.target) else card
                }
                _uiState.update { it.copy(cardProfiles = updated) }
                viewModelScope.launch { repository.saveCardProfiles(updated) }
                updateCurrencyDisplay(); return
            }
            is CalculatorAction.SetSizeCategory -> {
                _uiState.update { it.copy(sizeCategory = action.category) }
                viewModelScope.launch { repository.saveSizeCategory(action.category) }
                return
            }
            is CalculatorAction.SetShoeIsMens -> {
                _uiState.update { it.copy(shoeIsMens = action.isMens) }
                viewModelScope.launch { repository.saveShoeIsMens(action.isMens) }
                return
            }
            is CalculatorAction.SetShoeCountries -> {
                _uiState.update { it.copy(shoeFromCountry = action.from, shoeToCountry = action.to) }
                viewModelScope.launch { repository.saveShoeCountries(action.from, action.to) }
                return
            }
            is CalculatorAction.SetWomensCountries -> {
                _uiState.update { it.copy(womensFromCountry = action.from, womensToCountry = action.to) }
                viewModelScope.launch { repository.saveWomensCountries(action.from, action.to) }
                return
            }
            is CalculatorAction.SetMensCountries -> {
                _uiState.update { it.copy(mensFromCountry = action.from, mensToCountry = action.to) }
                viewModelScope.launch { repository.saveMensCountries(action.from, action.to) }
                return
            }
            is CalculatorAction.DismissHelpHint -> {
                _uiState.update { it.copy(helpHintSeen = true) }
                viewModelScope.launch { repository.saveHelpHintSeen() }
                return
            }
            is CalculatorAction.SetOcrConvertEnabled -> {
                _uiState.update { it.copy(ocrConvertEnabled = action.enabled) }
                viewModelScope.launch { repository.saveOcrConvertEnabled(action.enabled) }
                return
            }
            is CalculatorAction.SetTaxEnabled -> {
                _uiState.update { it.copy(taxEnabled = action.enabled) }
                viewModelScope.launch { repository.saveTaxEnabled(action.enabled) }
                refreshCurrentTaxInfo()
                return
            }
            is CalculatorAction.SetTaxCountry -> {
                _uiState.update { it.copy(taxCountryCode = action.countryCode, taxStateCode = null, taxRateOverride = null, currentTaxInfo = null) }
                viewModelScope.launch {
                    repository.saveTaxCountryCode(action.countryCode)
                    repository.saveTaxStateCode(null)
                    repository.saveTaxRateOverride(null)
                }
                refreshCurrentTaxInfo()
                return
            }
            is CalculatorAction.SetTaxState -> {
                _uiState.update { it.copy(taxStateCode = action.stateCode, taxRateOverride = null) }
                viewModelScope.launch {
                    repository.saveTaxStateCode(action.stateCode)
                    repository.saveTaxRateOverride(null)
                }
                refreshCurrentTaxInfo()
                return
            }
            is CalculatorAction.SetTaxRateOverride -> {
                _uiState.update { it.copy(taxRateOverride = action.rate) }
                viewModelScope.launch { repository.saveTaxRateOverride(action.rate) }
                refreshCurrentTaxInfo()
                return
            }
            is CalculatorAction.SetTaxApplied -> {
                _uiState.update { it.copy(taxApplied = action.applied) }
                viewModelScope.launch { repository.saveTaxApplied(action.applied) }
                updateDisplay(); return
            }
            is CalculatorAction.SetTipAfterTax -> {
                _uiState.update { it.copy(tipAfterTax = action.afterTax) }
                viewModelScope.launch { repository.saveTipAfterTax(action.afterTax) }
                updateDisplay(); return
            }
            is CalculatorAction.SetNoTip -> {
                _uiState.update { it.copy(noTip = action.noTip) }
                viewModelScope.launch { repository.saveNoTip(action.noTip) }
                updateDisplay(); return
            }
            is CalculatorAction.SetTipPreset -> {
                val current = _uiState.value
                val updated = current.tipPresets.toMutableList().also { it[action.index] = action.percent }
                val newTipPercent = if (current.tipPercent == current.tipPresets[action.index]) action.percent else current.tipPercent
                _uiState.update { it.copy(tipPresets = updated, tipPercent = newTipPercent) }
                viewModelScope.launch { repository.saveTipPreset(action.index, action.percent) }
                updateDisplay(); return
            }
            is CalculatorAction.DeleteHistoryEntry -> {
                val updated = _uiState.value.history.filter { it.timestamp != action.timestamp }
                _uiState.update { it.copy(history = updated) }
                viewModelScope.launch { repository.saveHistory(updated) }
                return
            }
            is CalculatorAction.SetHistoryNote -> {
                val updated = _uiState.value.history.map {
                    if (it.timestamp == action.timestamp) it.copy(note = action.note) else it
                }
                _uiState.update { it.copy(history = updated) }
                viewModelScope.launch { repository.saveHistory(updated) }
                return
            }
            is CalculatorAction.SendTipShareToFX -> {
                val s = _uiState.value
                tipSavedBill = currentInput
                val bill = currentInput.toDoubleOrNull() ?: 0.0
                val bd = computeTipBreakdown(
                    bill, s.tipPercent, s.tipPeopleCount,
                    s.currentTaxInfo, s.taxApplied, s.tipAfterTax, s.noTip
                )
                val amount = bd.perPerson ?: bd.total
                currentInput = formatResult(amount)
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
                justCalculated = true; shouldResetInput = false
                _uiState.update {
                    val newHistory = if (it.conversionMode != ConversionMode.CURRENCY) it.modeHistory + it.conversionMode else it.modeHistory
                    it.copy(isError = false, conversionMode = ConversionMode.CURRENCY, modeHistory = newHistory)
                }
                updateDisplay(); return
            }
            is CalculatorAction.SetLocalisationCountry -> {
                val updatedRecents = (_uiState.value.localisationRecentCountries.toMutableList()
                    .also { it.remove(action.code) }
                    .also { it.add(0, action.code) })
                    .take(5)
                _uiState.update { it.copy(localisationCountryCode = action.code, localisationLoading = true, localisationError = false, localisationInfo = null, localisationRecentCountries = updatedRecents) }
                viewModelScope.launch {
                    localisationRepository.saveCountryCode(action.code)
                    localisationRepository.saveRecentCountries(updatedRecents)
                    runCatching { localisationRepository.getCountryInfo(action.code) }
                        .onSuccess { info -> _uiState.update { it.copy(localisationInfo = info, localisationLoading = false) } }
                        .onFailure { _uiState.update { it.copy(localisationLoading = false, localisationError = true) } }
                }
                return
            }
            is CalculatorAction.RefreshLocalisation -> {
                val code = _uiState.value.localisationCountryCode
                _uiState.update { it.copy(localisationLoading = true, localisationError = false) }
                viewModelScope.launch {
                    runCatching { localisationRepository.getCountryInfo(code, forceRefresh = true) }
                        .onSuccess { info -> _uiState.update { it.copy(localisationInfo = info, localisationLoading = false) } }
                        .onFailure { _uiState.update { it.copy(localisationLoading = false, localisationError = true) } }
                }
                return
            }
        }
        updateDisplay()
    }

    private fun handleDigit(digit: Int) {
        if (_uiState.value.conversionMode == ConversionMode.TIP) tipSavedBill = null
        if (justCalculated || shouldResetInput) {
            if (justCalculated) {
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
            }
            currentInput = digit.toString()
            justCalculated = false; shouldResetInput = false
            return
        }
        currentInput = if (currentInput == "0") digit.toString()
        else if (currentInput.length < 12) currentInput + digit.toString()
        else currentInput
        if (currentInput == secretCarCode) {
            currentInput = "0"
            _uiState.update { it.copy(carUnlockPromptPending = true) }
        }
    }

    private fun handleDecimal() {
        if (_uiState.value.conversionMode == ConversionMode.TIP) tipSavedBill = null
        if (justCalculated || shouldResetInput) {
            if (justCalculated) {
                firstOperand = null; pendingOp = null
                bracketStack.clear(); expressionDisplay.clear()
            }
            currentInput = "0."
            justCalculated = false; shouldResetInput = false
            return
        }
        if (!currentInput.contains('.')) currentInput += "."
    }

    private fun handleClear() {
        currentInput = "0"
        firstOperand = null; pendingOp = null
        justCalculated = false; shouldResetInput = false
        bracketStack.clear(); expressionDisplay.clear()
        tipSavedBill = null
        _uiState.update { it.copy(isError = false) }
    }

    private fun handleDelete() {
        if (justCalculated || _uiState.value.isError) { handleClear(); return }
        if (shouldResetInput) {
            // Undo the last operator: fold the chain back to a single editable value
            // (a partial chain like "5 + 3 +" collapses to its running total "8" —
            // keeping the expression text would drift out of sync with the operands)
            if (pendingOp != null) {
                currentInput = formatResult(firstOperand ?: 0.0)
                firstOperand = null
                pendingOp = null
                expressionDisplay.clear()
            }
            shouldResetInput = false
            return
        }
        currentInput = if (currentInput.length <= 1) "0" else currentInput.dropLast(1)
    }

    private fun handlePercent() {
        if (shouldResetInput) return
        val value = currentInput.toDoubleOrNull() ?: return
        currentInput = formatResult(value / 100.0)
        justCalculated = false
    }

    private fun handleOperation(op: Char) {
        if (_uiState.value.isError) return
        justCalculated = false
        val sym = opSymbol(op)

        if (shouldResetInput) {
            // Replace pending operator or set new one after bracket close
            if (pendingOp != null) {
                val expr = expressionDisplay.toString().trimEnd()
                expressionDisplay.clear()
                expressionDisplay.append(expr.substring(0, expr.lastIndexOf(' ')).trimEnd())
                expressionDisplay.append(" $sym ")
            } else {
                // After bracket close: firstOperand holds the result, just attach new op
                expressionDisplay.append(" $sym ")
            }
            pendingOp = op
            return
        }

        val current = currentInput.toDoubleOrNull() ?: return
        if (firstOperand != null && pendingOp != null) {
            val result = applyOp(firstOperand!!, current, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true, display = "Error") }; return
            }
            firstOperand = result
            currentInput = formatResult(result)
        } else {
            firstOperand = current
        }
        expressionDisplay.append("$currentInput $sym ")
        pendingOp = op
        shouldResetInput = true
    }

    private fun handleOpenBracket() {
        // Only open when we're waiting for a new operand (after operator or at fresh start)
        val atStart = firstOperand == null && pendingOp == null
            && currentInput == "0" && bracketStack.isEmpty() && !justCalculated
        if (!shouldResetInput && !atStart) return

        justCalculated = false
        bracketStack.add(BracketState(firstOperand, pendingOp))
        firstOperand = null; pendingOp = null
        shouldResetInput = false
        currentInput = "0"
        expressionDisplay.append("(")
    }

    private fun handleCloseBracket() {
        if (bracketStack.isEmpty()) return

        val current = currentInput.toDoubleOrNull() ?: return
        val bracketResult = if (firstOperand != null && pendingOp != null) {
            applyOp(firstOperand!!, current, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true) }; return
            }
        } else current

        expressionDisplay.append("$currentInput)")

        val (outerFirst, outerOp) = bracketStack.removeAt(bracketStack.lastIndex)
        firstOperand = outerFirst
        pendingOp = outerOp

        // Apply the outer pending operation to the bracket result
        val result = if (firstOperand != null && pendingOp != null) {
            applyOp(firstOperand!!, bracketResult, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true) }; return
            }
        } else bracketResult

        firstOperand = result
        pendingOp = null
        currentInput = formatResult(result)
        shouldResetInput = true
    }

    private fun handleCalculate() {
        if (_uiState.value.isError) { handleClear(); return }

        // Auto-close any unclosed brackets
        var value = currentInput.toDoubleOrNull() ?: return

        // Snapshot the expression for history before it is cleared below
        lastCalcExpression = if (expressionDisplay.isEmpty()) ""
            else expressionDisplay.toString() + (if (shouldResetInput) "" else currentInput)

        if (firstOperand != null && pendingOp != null) {
            value = applyOp(firstOperand!!, value, pendingOp!!) ?: run {
                _uiState.update { it.copy(isError = true, display = "Error") }; return
            }
        } else if (firstOperand != null && shouldResetInput) {
            value = firstOperand!!
        }

        for ((stackFirst, stackOp) in bracketStack.asReversed()) {
            if (stackFirst != null && stackOp != null) {
                value = applyOp(stackFirst, value, stackOp) ?: continue
            }
        }

        currentInput = formatResult(value)
        firstOperand = null; pendingOp = null
        bracketStack.clear(); expressionDisplay.clear()
        justCalculated = true
    }

    private fun recordHistory() {
        val state = _uiState.value
        val distFrom = if (state.distanceReversed) state.distancePair.toAbbr else state.distancePair.fromAbbr
        val distTo   = if (state.distanceReversed) state.distancePair.fromAbbr else state.distancePair.toAbbr
        val toTempUnit = if (state.tempUnit == TempUnit.CELSIUS) TempUnit.FAHRENHEIT else TempUnit.CELSIUS
        val pctLabel = if (state.tipPercent == state.tipPercent.toLong().toDouble())
            state.tipPercent.toLong().toString() else "%.1f".format(Locale.US, state.tipPercent)
        val activeCard = if (state.conversionMode == ConversionMode.CURRENCY)
            state.cardProfiles.find { it.id == state.activeCardId } else null
        val entry = HistoryEntry(
            display = state.display,
            expression = lastCalcExpression,
            fromAmount = state.fromAmount,
            toAmount = state.toAmount,
            fromCurrency = when (state.conversionMode) {
                ConversionMode.DISTANCE -> distFrom
                ConversionMode.TEMPERATURE -> state.tempUnit.abbr
                ConversionMode.TIP -> "$pctLabel%"
                ConversionMode.FUEL -> "car"   // unreachable: Car-tab calculations don't call recordHistory()
                ConversionMode.CURRENCY -> state.fromCurrency
            },
            toCurrency = when (state.conversionMode) {
                ConversionMode.DISTANCE -> distTo
                ConversionMode.TEMPERATURE -> toTempUnit.abbr
                ConversionMode.TIP -> "${state.tipPeopleCount}p"
                ConversionMode.FUEL -> "car"   // unreachable: Car-tab calculations don't call recordHistory()
                ConversionMode.CURRENCY -> state.toCurrency
            },
            timestamp = System.currentTimeMillis(),
            conversionMode = state.conversionMode.name,
            cardName = activeCard?.name,
            cardMarkupPercent = activeCard?.markupPercent,
            cardMinFeeAmount = activeCard?.minFeeAmount?.takeIf { it > 0 },
            cardMinFeeCurrency = activeCard?.minFeeCurrency?.takeIf { it.isNotEmpty() }
        )
        val updated = (listOf(entry) + state.history).take(50)
        _uiState.update { it.copy(history = updated) }
        viewModelScope.launch { repository.saveHistory(updated) }
    }

    private fun applyOp(a: Double, b: Double, op: Char): Double? = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0.0) null else a / b
        else -> null
    }

    private fun opSymbol(op: Char) = when (op) {
        '*' -> "×"; '/' -> "÷"; '-' -> "−"; else -> op.toString()
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        return if (value == floor(value) && abs(value) < 1e12) value.toLong().toString()
        else "%.10g".format(Locale.US, value).trimEnd('0').trimEnd('.')
    }

    private fun updateDisplay() {
        val expression = when {
            expressionDisplay.isNotEmpty() -> expressionDisplay.toString()
            firstOperand != null && pendingOp != null ->
                "${formatResult(firstOperand!!)} ${opSymbol(pendingOp!!)}"
            else -> ""
        }
        _uiState.update { it.copy(display = currentInput, expression = expression, isError = false) }
        when (_uiState.value.conversionMode) {
            ConversionMode.DISTANCE -> updateDistanceDisplay()
            ConversionMode.TEMPERATURE -> updateTempDisplay()
            ConversionMode.TIP -> updateTipDisplay()
            ConversionMode.FUEL -> updateCarDisplay()
            ConversionMode.CURRENCY -> updateCurrencyDisplay()
        }
    }

    // Returns the effective "1 USD = X [code]" rate, preferring custom pair entries over live rates.
    // Card rates take priority. Global custom rates are only used when no card is active,
    // or when the card has useGlobalRates = true.
    private fun effectiveUsdRate(code: String, state: CalculatorUiState, cardRates: Map<String, CustomRateEntry>? = null, useGlobalRates: Boolean = true): Double? {
        cardRates?.get(code)?.let { entry ->
            val baseRate = rates[entry.base] ?: return null
            return baseRate * entry.rate
        }
        if (useGlobalRates) {
            val entry = state.customRates[code]
            if (entry != null) {
                val baseRate = rates[entry.base] ?: return null
                return baseRate * entry.rate
            }
        }
        return rates[code]
    }

    private fun updateCurrencyDisplay() {
        val value = currentInput.toDoubleOrNull() ?: 0.0
        val state = _uiState.value
        val activeCard = state.cardProfiles.find { it.id == state.activeCardId }
        val cardRates = activeCard?.customRates?.takeIf { it.isNotEmpty() }
        val useGlobalRates = activeCard == null || activeCard.useGlobalRates
        val fromRate = effectiveUsdRate(state.fromCurrency, state, cardRates, useGlobalRates)
        val toRate = effectiveUsdRate(state.toCurrency, state, cardRates, useGlobalRates)

        if (fromRate != null && toRate != null && fromRate != 0.0) {
            val baseToValue = value * toRate / fromRate
            val fromName = try { JavaCurrency.getInstance(state.fromCurrency).displayName } catch (e: Exception) { state.fromCurrency }
            val toName = try { JavaCurrency.getInstance(state.toCurrency).displayName } catch (e: Exception) { state.toCurrency }
            val usingCustom = state.customRates.containsKey(state.fromCurrency) || state.customRates.containsKey(state.toCurrency) ||
                (cardRates != null && (cardRates.containsKey(state.fromCurrency) || cardRates.containsKey(state.toCurrency)))

            val (toValue, cardLabel) = if (activeCard != null) {
                val percentFee = baseToValue * activeCard.markupPercent / 100.0
                val pctStr = if (activeCard.markupPercent == 0.0) "0%"
                    else "+${"%.2f".format(Locale.US, activeCard.markupPercent).trimEnd('0').trimEnd('.')}%"
                if (value > 0 && activeCard.minFeeAmount > 0 && activeCard.minFeeCurrency.isNotEmpty()) {
                    val minFeeRate = rates[activeCard.minFeeCurrency]
                    val minFeeInTo = if (minFeeRate != null && minFeeRate > 0)
                        activeCard.minFeeAmount * toRate / minFeeRate
                    else activeCard.minFeeAmount
                    if (minFeeInTo > percentFee) {
                        val minStr = "${"%.2f".format(Locale.US, activeCard.minFeeAmount)} ${activeCard.minFeeCurrency}"
                        Pair(baseToValue + minFeeInTo, "  •  💳 ${activeCard.name} min. $minStr")
                    } else {
                        Pair(baseToValue + percentFee, "  •  💳 ${activeCard.name} $pctStr")
                    }
                } else {
                    Pair(baseToValue + percentFee, "  •  💳 ${activeCard.name} $pctStr")
                }
            } else {
                Pair(baseToValue, "")
            }

            val rateLabel = "1 ${state.fromCurrency} ($fromName) = ${"%.4f".format(Locale.US, toRate / fromRate)} ${state.toCurrency} ($toName)" +
                (if (usingCustom) " ★" else "") + cardLabel
            val pctDiff = if (usingCustom) {
                val liveFrom = rates[state.fromCurrency]
                val liveTo = rates[state.toCurrency]
                if (liveFrom != null && liveTo != null && liveFrom != 0.0) {
                    val liveRate = liveTo / liveFrom
                    val customRate = toRate / fromRate
                    (customRate - liveRate) / liveRate * 100.0
                } else null
            } else null
            _uiState.update {
                it.copy(
                    fromAmount = "${state.fromCurrency} ${"%.2f".format(Locale.US, value)}",
                    toAmount = "${state.toCurrency} ${"%.2f".format(Locale.US, toValue)}",
                    exchangeRateLabel = rateLabel,
                    customRatePctDiff = pctDiff,
                    exchangeRate = toRate / fromRate
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    fromAmount = "${state.fromCurrency} —",
                    toAmount = "${state.toCurrency} —",
                    exchangeRateLabel = "",
                    customRatePctDiff = null,
                    exchangeRate = null
                )
            }
        }
    }

    private fun updateDistanceDisplay() {
        val value = currentInput.toDoubleOrNull() ?: 0.0
        val s = _uiState.value
        val pair = s.distancePair
        if (pair == DistancePair.STONE_LB_KG) {
            val lbs = s.stoneLbPart
            if (!s.distanceReversed) {
                val kg = (value * 14 + lbs) * 0.453592
                _uiState.update { it.copy(
                    fromAmount = "${currentInput.ifEmpty { "0" }} st $lbs lb",
                    toAmount   = "kg ${"%.3f".format(Locale.US, kg)}",
                    exchangeRateLabel = "1 st = 6.350 kg  ·  1 lb = 0.454 kg",
                    customRatePctDiff = null
                )}
            } else {
                // Round total pounds first so e.g. 13.9 lb carries to the next stone
                // instead of clamping at "13 lb"
                val wholeLbs  = (value / 0.453592).roundToInt()
                val stonePart = wholeLbs / 14
                val lbPart    = wholeLbs % 14
                _uiState.update { it.copy(
                    fromAmount = "kg ${currentInput.ifEmpty { "0" }}",
                    toAmount   = "$stonePart st $lbPart lb",
                    exchangeRateLabel = "1 kg = 2.205 lb  ·  14 lb = 1 st",
                    customRatePctDiff = null
                )}
            }
            return
        }
        if (pair.isReciprocal) {
            val effectiveFactor = if (pair == DistancePair.MPG_L100KM) (if (s.fuelUseUkGallons) 282.481 else 235.214) else pair.factor
            val fromAbbr = if (s.distanceReversed) pair.toAbbr else pair.fromAbbr
            val toAbbr   = if (s.distanceReversed) pair.fromAbbr else pair.toAbbr
            val converted = if (value > 0) effectiveFactor / value else 0.0
            _uiState.update {
                it.copy(
                    fromAmount = "$fromAbbr ${"%.2f".format(Locale.US, value)}",
                    toAmount = "$toAbbr ${"%.2f".format(Locale.US, converted)}",
                    exchangeRateLabel = "${"%.2f".format(Locale.US, effectiveFactor)} ÷ $fromAbbr = $toAbbr",
                    customRatePctDiff = null
                )
            }
            return
        }
        val useLitres = s.distanceUseLitres && pair.toAbbr == "ml"
        val baseFactor = if (useLitres) pair.factor / 1000.0 else pair.factor
        val fromAbbr = if (s.distanceReversed) (if (useLitres) "L" else pair.toAbbr) else pair.fromAbbr
        val toAbbr   = if (s.distanceReversed) pair.fromAbbr else (if (useLitres) "L" else pair.toAbbr)
        val factor   = if (s.distanceReversed) 1.0 / baseFactor else baseFactor
        val converted = value * factor
        _uiState.update {
            it.copy(
                fromAmount = "$fromAbbr ${"%.4f".format(Locale.US, value)}",
                toAmount = "$toAbbr ${"%.4f".format(Locale.US, converted)}",
                exchangeRateLabel = "1 $fromAbbr = ${"%.5f".format(Locale.US, factor)} $toAbbr",
                customRatePctDiff = null
            )
        }
    }

    private fun updateTempDisplay() {
        val value = currentInput.toDoubleOrNull() ?: 0.0
        val unit = _uiState.value.tempUnit
        val (fromAbbr, toAbbr, converted) = when (unit) {
            TempUnit.CELSIUS    -> Triple("°C", "°F", value * 9.0 / 5.0 + 32.0)
            TempUnit.FAHRENHEIT -> Triple("°F", "°C", (value - 32.0) * 5.0 / 9.0)
        }
        _uiState.update {
            it.copy(
                fromAmount = "$fromAbbr ${"%.2f".format(Locale.US, value)}",
                toAmount = "$toAbbr ${"%.2f".format(Locale.US, converted)}",
                exchangeRateLabel = "$fromAbbr  ⇄  $toAbbr",
                customRatePctDiff = null
            )
        }
    }

    private fun updateTipDisplay() {
        val bill = currentInput.toDoubleOrNull() ?: 0.0
        val state = _uiState.value
        val bd = computeTipBreakdown(
            bill, state.tipPercent, state.tipPeopleCount,
            state.currentTaxInfo, state.taxApplied, state.tipAfterTax, state.noTip
        )
        val pctLabel = if (state.tipPercent == state.tipPercent.toLong().toDouble())
            state.tipPercent.toLong().toString() else "%.1f".format(Locale.US, state.tipPercent)
        _uiState.update {
            it.copy(
                // These only surface in history cards ("Bill" tab shows TipBreakdownDisplay)
                fromAmount = "Bill ${"%.2f".format(Locale.US, bd.bill)}",
                toAmount = "Total ${"%.2f".format(Locale.US, bd.total)}",
                exchangeRateLabel = "$pctLabel% tip · ${state.tipPeopleCount} ${if (state.tipPeopleCount == 1) "person" else "people"}",
                customRatePctDiff = null
            )
        }
    }

    private fun solveMissingCarValue(target: CarField, fpGal: Double?, ep: Double?, mpg: Double?, mikwh: Double?): Double? {
        fun ok(v: Double?) = v != null && v > 0.0
        return when (target) {
            CarField.MPG               -> if (ok(fpGal) && ok(mikwh) && ok(ep)) fpGal!! * mikwh!! / ep!! else null
            CarField.MI_PER_KWH        -> if (ok(ep) && ok(mpg) && ok(fpGal))   ep!! * mpg!! / fpGal!!   else null
            CarField.FUEL_PRICE        -> if (ok(mpg) && ok(ep) && ok(mikwh))   mpg!! * ep!! / mikwh!!    else null
            CarField.ELECTRICITY_PRICE -> if (ok(mikwh) && ok(fpGal) && ok(mpg)) mikwh!! * fpGal!! / mpg!! else null
        }
    }

    private fun updateCarDisplay() {
        val s = _uiState.value
        // Displayed values: mpg/mi-kWh are shown in metric (L/100km, kWh/100km) when
        // carUseMetric is on, imperial otherwise; fuel price is per-litre when metric,
        // per-gallon (UK/US) otherwise. The break-even math always runs in a fixed
        // canonical basis (mpg, mi/kWh, price-per-gallon) — toMpg/toMiKwh/toGalPrice
        // convert display values into that basis, and results are converted back.
        var fpDisplay = s.carFuelPricePerLitre
        var epDisplay = s.carElectricityPricePerKwh
        var mpgDisplay = s.carMpg
        var mikwhDisplay = s.carMiPerKwh
        when (s.carActiveField) {
            CarField.FUEL_PRICE -> fpDisplay = currentInput
            CarField.ELECTRICITY_PRICE -> epDisplay = currentInput
            CarField.MPG -> mpgDisplay = currentInput
            CarField.MI_PER_KWH -> mikwhDisplay = currentInput
        }

        val gallonLitres = if (s.fuelUseUkGallons) 4.54609 else 3.78541
        val mpgFactor = if (s.fuelUseUkGallons) 282.481 else 235.214
        val kwhFactor = 62.1371
        // mpg <-> L/100km and mi/kWh <-> kWh/100km are both reciprocal (constant / value),
        // so the same formula converts either direction between imperial and metric.
        fun toMpg(v: String) = if (s.carUseMetric) v.toDoubleOrNull()?.takeIf { it > 0 }?.let { mpgFactor / it } else v.toDoubleOrNull()
        fun toMiKwh(v: String) = if (s.carUseMetric) v.toDoubleOrNull()?.takeIf { it > 0 }?.let { kwhFactor / it } else v.toDoubleOrNull()
        fun toGalPrice(v: String) = v.toDoubleOrNull()?.let { if (s.carUseMetric) it * gallonLitres else it }

        fun blank(v: String) = v.toDoubleOrNull().let { it == null || it <= 0.0 }

        var target = s.carTargetField
        if (target == s.carActiveField) target = null
        if (target == null) {
            val values = mapOf(
                CarField.FUEL_PRICE to fpDisplay, CarField.ELECTRICITY_PRICE to epDisplay,
                CarField.MPG to mpgDisplay, CarField.MI_PER_KWH to mikwhDisplay
            )
            target = CarField.entries.firstOrNull { it != s.carActiveField && blank(values.getValue(it)) }
        }

        if (target != null) {
            val fpGal = toGalPrice(fpDisplay)
            val mpgCanon = toMpg(mpgDisplay)
            val mikwhCanon = toMiKwh(mikwhDisplay)
            val solved = solveMissingCarValue(target, fpGal, epDisplay.toDoubleOrNull(), mpgCanon, mikwhCanon)
            if (solved != null) {
                when (target) {
                    CarField.FUEL_PRICE -> fpDisplay = formatResult(if (s.carUseMetric) solved / gallonLitres else solved)
                    CarField.ELECTRICITY_PRICE -> epDisplay = formatResult(solved)
                    CarField.MPG -> mpgDisplay = formatResult(if (s.carUseMetric) mpgFactor / solved else solved)
                    CarField.MI_PER_KWH -> mikwhDisplay = formatResult(if (s.carUseMetric) kwhFactor / solved else solved)
                }
            }
        }

        val fpGalV = toGalPrice(fpDisplay)
        val mpgCanonV = toMpg(mpgDisplay)
        val mikwhCanonV = toMiKwh(mikwhDisplay)
        val epv = epDisplay.toDoubleOrNull()
        val costFuel = if (fpGalV != null && mpgCanonV != null && mpgCanonV > 0) fpGalV / mpgCanonV else null
        val costElec = if (epv != null && mikwhCanonV != null && mikwhCanonV > 0) epv / mikwhCanonV else null

        _uiState.update {
            it.copy(
                carFuelPricePerLitre = fpDisplay, carElectricityPricePerKwh = epDisplay,
                carMpg = mpgDisplay, carMiPerKwh = mikwhDisplay, carTargetField = target,
                fromAmount = costFuel?.let { c -> "Petrol ${"%.3f".format(Locale.US, c)}/mi" } ?: "Petrol —",
                toAmount = costElec?.let { c -> "Electric ${"%.3f".format(Locale.US, c)}/mi" } ?: "Electric —",
                exchangeRateLabel = "",
                customRatePctDiff = null
            )
        }
    }
}
