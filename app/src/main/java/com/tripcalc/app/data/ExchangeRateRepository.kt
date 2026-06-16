package com.tripcalc.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tripcalc.app.ui.AccentScheme
import com.tripcalc.app.ui.ConversionMode
import com.tripcalc.app.ui.SizeCategory
import com.tripcalc.app.ui.DistancePair
import com.tripcalc.app.ui.CustomRateEntry
import com.tripcalc.app.ui.DarkModePref
import com.tripcalc.app.ui.HistoryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exchange_rates")

private val RATES_KEY = stringPreferencesKey("rates_json")
private val TIMESTAMP_KEY = longPreferencesKey("rates_timestamp")
private val BASE_KEY = stringPreferencesKey("base_currency")
private val HOME_CURRENCY_KEY = stringPreferencesKey("home_currency")
private val LOCAL_CURRENCY_KEY = stringPreferencesKey("local_currency")
private val RECENT_CURRENCIES_KEY = stringPreferencesKey("recent_currencies")

private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L

class ExchangeRateRepository(private val context: Context) {
    private val api = ExchangeRateApi.create()
    private val gson = Gson()
    private val rateMapType = object : TypeToken<Map<String, Double>>() {}.type
    private val customRateMapType = object : TypeToken<Map<String, CustomRateEntry>>() {}.type
    private val historyListType = object : TypeToken<List<HistoryEntry>>() {}.type

    suspend fun getRates(baseCurrency: String = "USD", forceRefresh: Boolean = false): Map<String, Double> {
        val prefs = context.dataStore.data.first()
        val cachedJson = prefs[RATES_KEY]
        val timestamp = prefs[TIMESTAMP_KEY] ?: 0L
        val cachedBase = prefs[BASE_KEY]
        val cacheValid = !forceRefresh
            && cachedJson != null
            && cachedBase == baseCurrency
            && System.currentTimeMillis() - timestamp < CACHE_TTL_MS

        if (cacheValid) return gson.fromJson(cachedJson, rateMapType)

        return try {
            val response = api.getRates(baseCurrency)
            context.dataStore.edit {
                it[RATES_KEY] = gson.toJson(response.rates)
                it[TIMESTAMP_KEY] = System.currentTimeMillis()
                it[BASE_KEY] = baseCurrency
            }
            response.rates
        } catch (e: Exception) {
            if (cachedJson != null) gson.fromJson(cachedJson, rateMapType)
            else throw e
        }
    }

    suspend fun getLastUpdated(): String {
        val timestamp = context.dataStore.data.first()[TIMESTAMP_KEY] ?: return "Never"
        return SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    suspend fun saveCurrencyPrefs(home: String, local: String) {
        context.dataStore.edit {
            it[HOME_CURRENCY_KEY] = home
            it[LOCAL_CURRENCY_KEY] = local
        }
    }

    suspend fun loadCurrencyPrefs(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        return Pair(
            prefs[HOME_CURRENCY_KEY] ?: "EUR",
            prefs[LOCAL_CURRENCY_KEY] ?: "GBP"
        )
    }

    suspend fun saveRecentCurrencies(currencies: List<String>) {
        context.dataStore.edit { it[RECENT_CURRENCIES_KEY] = currencies.joinToString(",") }
    }

    suspend fun loadRecentCurrencies(): List<String> {
        val raw = context.dataStore.data.first()[RECENT_CURRENCIES_KEY] ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    private val customRatesKey = stringPreferencesKey("custom_rates")

    suspend fun saveCustomRates(customRates: Map<String, CustomRateEntry>) {
        context.dataStore.edit { it[customRatesKey] = gson.toJson(customRates) }
    }

    suspend fun loadCustomRates(): Map<String, CustomRateEntry> {
        val json = context.dataStore.data.first()[customRatesKey] ?: return emptyMap()
        return try { gson.fromJson(json, customRateMapType) } catch (e: Exception) { emptyMap() }
    }

    private val hapticEnabledKey = booleanPreferencesKey("haptic_enabled")

    suspend fun saveHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[hapticEnabledKey] = enabled }
    }

    suspend fun loadHapticEnabled(): Boolean =
        context.dataStore.data.first()[hapticEnabledKey] ?: true

    private val historyKey = stringPreferencesKey("history")

    suspend fun saveHistory(history: List<HistoryEntry>) {
        context.dataStore.edit { it[historyKey] = gson.toJson(history) }
    }

    suspend fun loadHistory(): List<HistoryEntry> {
        val json = context.dataStore.data.first()[historyKey] ?: return emptyList()
        return try { gson.fromJson(json, historyListType) } catch (e: Exception) { emptyList() }
    }

    private val darkModePrefKey = stringPreferencesKey("dark_mode_pref")
    private val accentSchemeKey = stringPreferencesKey("accent_scheme")

    suspend fun saveDarkModePref(pref: DarkModePref) {
        context.dataStore.edit { it[darkModePrefKey] = pref.name }
    }

    suspend fun loadDarkModePref(): DarkModePref {
        val name = context.dataStore.data.first()[darkModePrefKey] ?: return DarkModePref.SYSTEM
        return try { DarkModePref.valueOf(name) } catch (e: Exception) { DarkModePref.SYSTEM }
    }

    suspend fun saveAccentScheme(scheme: AccentScheme) {
        context.dataStore.edit { it[accentSchemeKey] = scheme.name }
    }

    suspend fun loadAccentScheme(): AccentScheme {
        val name = context.dataStore.data.first()[accentSchemeKey] ?: return AccentScheme.TEAL_GREEN
        return try { AccentScheme.valueOf(name) } catch (e: Exception) { AccentScheme.TEAL_GREEN }
    }

    private val defaultTipPercentKey = stringPreferencesKey("default_tip_percent")

    suspend fun saveDefaultTipPercent(percent: Double) {
        context.dataStore.edit { it[defaultTipPercentKey] = percent.toString() }
    }

    suspend fun loadDefaultTipPercent(): Double =
        context.dataStore.data.first()[defaultTipPercentKey]?.toDoubleOrNull() ?: 10.0

    private val customTipPercentKey = stringPreferencesKey("custom_tip_percent")

    suspend fun saveCustomTipPercent(percent: Double) {
        context.dataStore.edit { it[customTipPercentKey] = percent.toString() }
    }

    suspend fun loadCustomTipPercent(): Double =
        context.dataStore.data.first()[customTipPercentKey]?.toDoubleOrNull() ?: 12.5

    private val fuelUseUkGallonsKey = booleanPreferencesKey("fuel_use_uk_gallons")

    suspend fun saveFuelUseUkGallons(useUk: Boolean) {
        context.dataStore.edit { it[fuelUseUkGallonsKey] = useUk }
    }

    suspend fun loadFuelUseUkGallons(): Boolean =
        context.dataStore.data.first()[fuelUseUkGallonsKey] ?: true

    private val swapZeroDotKey = booleanPreferencesKey("swap_zero_dot")

    suspend fun saveSwapZeroDot(enabled: Boolean) {
        context.dataStore.edit { it[swapZeroDotKey] = enabled }
    }

    suspend fun loadSwapZeroDot(): Boolean =
        context.dataStore.data.first()[swapZeroDotKey] ?: true

    private val enabledModesKey = stringSetPreferencesKey("enabled_modes")

    suspend fun saveEnabledModes(modes: Set<ConversionMode>) {
        context.dataStore.edit { it[enabledModesKey] = modes.map { m -> m.name }.toSet() }
    }

    suspend fun loadEnabledModes(): Set<ConversionMode> {
        val names = context.dataStore.data.first()[enabledModesKey]
            ?: return ConversionMode.entries.toSet()
        return names.mapNotNull { name -> ConversionMode.entries.find { it.name == name } }.toSet()
            .ifEmpty { ConversionMode.entries.toSet() }
    }

    private val enabledDistancePairsKey = stringSetPreferencesKey("enabled_distance_pairs")

    suspend fun saveEnabledDistancePairs(pairs: Set<DistancePair>) {
        context.dataStore.edit { it[enabledDistancePairsKey] = pairs.map { p -> p.name }.toSet() }
    }

    suspend fun loadEnabledDistancePairs(): Set<DistancePair> {
        val names = context.dataStore.data.first()[enabledDistancePairsKey]
            ?: return setOf(DistancePair.MILES_KM)
        return names.mapNotNull { name -> DistancePair.entries.find { it.name == name } }.toSet()
            .ifEmpty { setOf(DistancePair.MILES_KM) }
    }

    private val cardMarkupPercentKey = stringPreferencesKey("card_markup_percent")

    suspend fun saveCardMarkupPercent(percent: Double) {
        context.dataStore.edit { it[cardMarkupPercentKey] = percent.toString() }
    }

    suspend fun loadCardMarkupPercent(): Double =
        context.dataStore.data.first()[cardMarkupPercentKey]?.toDoubleOrNull() ?: 0.0

    private val sizeCategoryKey = stringPreferencesKey("size_category")
    private val shoeIsMensKey = booleanPreferencesKey("shoe_is_mens")
    private val shoeFromKey = stringPreferencesKey("shoe_from_country")
    private val shoeToKey = stringPreferencesKey("shoe_to_country")
    private val womensFromKey = stringPreferencesKey("womens_from_country")
    private val womensTokKey = stringPreferencesKey("womens_to_country")
    private val mensFromKey = stringPreferencesKey("mens_from_country")
    private val mensToKey = stringPreferencesKey("mens_to_country")

    suspend fun saveSizeCategory(category: SizeCategory) {
        context.dataStore.edit { it[sizeCategoryKey] = category.name }
    }

    suspend fun loadSizeCategory(): SizeCategory {
        val name = context.dataStore.data.first()[sizeCategoryKey] ?: return SizeCategory.SHOE
        return try { SizeCategory.valueOf(name) } catch (e: Exception) { SizeCategory.SHOE }
    }

    suspend fun saveShoeIsMens(isMens: Boolean) {
        context.dataStore.edit { it[shoeIsMensKey] = isMens }
    }

    suspend fun loadShoeIsMens(): Boolean =
        context.dataStore.data.first()[shoeIsMensKey] ?: true

    suspend fun saveShoeCountries(from: String, to: String) {
        context.dataStore.edit { it[shoeFromKey] = from; it[shoeToKey] = to }
    }

    suspend fun loadShoeCountries(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        return Pair(prefs[shoeFromKey] ?: "EU", prefs[shoeToKey] ?: "UK")
    }

    suspend fun saveWomensCountries(from: String, to: String) {
        context.dataStore.edit { it[womensFromKey] = from; it[womensTokKey] = to }
    }

    suspend fun loadWomensCountries(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        return Pair(prefs[womensFromKey] ?: "US", prefs[womensTokKey] ?: "UK")
    }

    suspend fun saveMensCountries(from: String, to: String) {
        context.dataStore.edit { it[mensFromKey] = from; it[mensToKey] = to }
    }

    suspend fun loadMensCountries(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        return Pair(prefs[mensFromKey] ?: "US/UK", prefs[mensToKey] ?: "EU")
    }
}
