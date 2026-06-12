package com.example.calcapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.calcapp.ui.CustomRateEntry
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
}
