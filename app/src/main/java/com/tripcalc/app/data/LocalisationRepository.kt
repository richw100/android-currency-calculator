package com.tripcalc.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

private const val LOCALISATION_CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000L

private val Context.locDataStore: DataStore<Preferences> by preferencesDataStore(name = "localisation_data")

data class CountryLocalisationInfo(
    val code: String,
    val name: String,
    val callingCode: String?,
    val currencies: List<String>,
    val languages: List<String>,
    val drivingSide: String?,
    val emergencyNumbers: List<String>,
    val plugInfo: PlugInfo?,
    val tapWaterSafe: Boolean?,
    val drivingInfo: DrivingInfo?,
    val fetchedAt: Long = 0L
)

class LocalisationRepository(private val context: Context) {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "TripCalc/1.4 (Android; travel app)")
                    .header("Accept", "application/json")
                    .build()
            )
        }
        .build()

    private fun cacheKey(code: String) = stringPreferencesKey("country_$code")
    private val locCountryKey = stringPreferencesKey("selected_country")
    private val recentCountriesKey = stringPreferencesKey("recent_countries")

    suspend fun saveCountryCode(code: String) {
        context.locDataStore.edit { it[locCountryKey] = code }
    }

    suspend fun loadCountryCode(): String =
        context.locDataStore.data.first()[locCountryKey] ?: "GB"

    suspend fun saveRecentCountries(codes: List<String>) {
        context.locDataStore.edit { it[recentCountriesKey] = codes.joinToString(",") }
    }

    suspend fun loadRecentCountries(): List<String> {
        val raw = context.locDataStore.data.first()[recentCountriesKey] ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun getCountryInfo(code: String, forceRefresh: Boolean = false): CountryLocalisationInfo {
        // Codes are interpolated into the SPARQL query and URL path — only ISO alpha-2 allowed
        if (!code.matches(Regex("[A-Z]{2}"))) return buildStubInfo(code)
        if (!forceRefresh) {
            val cached = loadCached(code)
            if (cached != null && System.currentTimeMillis() - cached.fetchedAt < LOCALISATION_CACHE_TTL_MS) {
                return cached
            }
        }
        return try {
            fetchAndCache(code)
        } catch (e: Exception) {
            loadCached(code) ?: buildStubInfo(code)
        }
    }

    private suspend fun fetchAndCache(code: String): CountryLocalisationInfo = coroutineScope {
        val wikidataDeferred = async { fetchWikidata(code) }
        val restCountriesDeferred = async { fetchRestCountries(code) }

        val wikiResult = wikidataDeferred.await()
        val restResult = restCountriesDeferred.await()

        val info = CountryLocalisationInfo(
            code = code,
            name = restResult.name ?: COUNTRY_LIST.find { it.first == code }?.second ?: code,
            callingCode = restResult.callingCode,
            currencies = restResult.currencies,
            languages = restResult.languages,
            drivingSide = wikiResult.drivingSide,
            emergencyNumbers = wikiResult.emergencyNumbers,
            plugInfo = PLUG_TYPES[code],
            tapWaterSafe = TAP_WATER_SAFE[code],
            drivingInfo = DRIVING_INFO[code],
            fetchedAt = System.currentTimeMillis()
        )
        saveToCache(code, info)
        info
    }

    private data class WikidataResult(val drivingSide: String?, val emergencyNumbers: List<String>)
    private data class RestCountriesResult(
        val callingCode: String?, val currencies: List<String>,
        val languages: List<String>, val name: String?
    )

    private suspend fun fetchWikidata(code: String): WikidataResult = withContext(Dispatchers.IO) {
        val query = """
            SELECT ?drivingSideLabel ?emLabel WHERE {
              ?country wdt:P297 "$code".
              OPTIONAL { ?country wdt:P1622 ?drivingSide. }
              OPTIONAL { ?country wdt:P2852 ?em. }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "en".
                ?drivingSide rdfs:label ?drivingSideLabel.
                ?em rdfs:label ?emLabel. }
            }
        """.trimIndent()

        try {
            val url = "https://query.wikidata.org/sparql?format=json&query=${URLEncoder.encode(query, "UTF-8")}"
            val request = Request.Builder().url(url).build()
            val body = httpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext WikidataResult(null, emptyList())

            val json = JSONObject(body)
            val bindings = json.getJSONObject("results").getJSONArray("bindings")

            var drivingSide: String? = null
            val emergencyNumbers = mutableSetOf<String>()

            for (i in 0 until bindings.length()) {
                val binding = bindings.getJSONObject(i)
                if (drivingSide == null && binding.has("drivingSideLabel")) {
                    val label = binding.getJSONObject("drivingSideLabel").getString("value")
                    drivingSide = when {
                        label.contains("left", ignoreCase = true) -> "left"
                        label.contains("right", ignoreCase = true) -> "right"
                        else -> null
                    }
                }
                if (binding.has("emLabel")) {
                    emergencyNumbers.add(binding.getJSONObject("emLabel").getString("value"))
                }
            }

            WikidataResult(drivingSide, emergencyNumbers.sorted())
        } catch (e: Exception) {
            WikidataResult(null, emptyList())
        }
    }

    private suspend fun fetchRestCountries(code: String): RestCountriesResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://restcountries.com/v3.1/alpha/$code?fields=name,currencies,languages,idd"
            val request = Request.Builder().url(url).build()
            val body = httpClient.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext RestCountriesResult(null, emptyList(), emptyList(), null)

            val json = JSONObject(body)

            val name = try { json.getJSONObject("name").getString("common") } catch (e: Exception) { null }

            val callingCode = try {
                val idd = json.getJSONObject("idd")
                val root = idd.optString("root", "")
                val suffixes = idd.optJSONArray("suffixes")
                if (suffixes != null && suffixes.length() == 1) "$root${suffixes.getString(0)}"
                else root.ifEmpty { null }
            } catch (e: Exception) { null }

            val currencies = try {
                val cur = json.getJSONObject("currencies")
                cur.keys().asSequence().map { key ->
                    val c = cur.getJSONObject(key)
                    val cname = c.optString("name", "")
                    val symbol = c.optString("symbol", "")
                    if (symbol.isNotEmpty()) "$key ($symbol)" else key
                }.toList()
            } catch (e: Exception) { emptyList() }

            val languages = try {
                val lang = json.getJSONObject("languages")
                lang.keys().asSequence().map { lang.getString(it) }.toList().sorted()
            } catch (e: Exception) { emptyList() }

            RestCountriesResult(callingCode, currencies, languages, name)
        } catch (e: Exception) {
            RestCountriesResult(null, emptyList(), emptyList(), null)
        }
    }

    private fun buildStubInfo(code: String) = CountryLocalisationInfo(
        code = code,
        name = COUNTRY_LIST.find { it.first == code }?.second ?: code,
        callingCode = null, currencies = emptyList(), languages = emptyList(),
        drivingSide = null, emergencyNumbers = emptyList(),
        plugInfo = PLUG_TYPES[code], tapWaterSafe = TAP_WATER_SAFE[code],
        drivingInfo = DRIVING_INFO[code], fetchedAt = 0L
    )

    private suspend fun saveToCache(code: String, info: CountryLocalisationInfo) {
        context.locDataStore.edit { it[cacheKey(code)] = gson.toJson(info) }
    }

    private suspend fun loadCached(code: String): CountryLocalisationInfo? {
        val json = context.locDataStore.data.first()[cacheKey(code)] ?: return null
        return try { gson.fromJson(json, CountryLocalisationInfo::class.java) } catch (e: Exception) { null }
    }
}
