package com.wallwar.data.ad

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoLocationDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs = context.getSharedPreferences("geo_prefs", Context.MODE_PRIVATE)

    private val _isIranUser = MutableStateFlow(determineInitialIranStatus())
    val isIranUser: StateFlow<Boolean> = _isIranUser.asStateFlow()

    private val _detectedCountry = MutableStateFlow(prefs.getString("detected_country", null) ?: "IR")
    val detectedCountry: StateFlow<String> = _detectedCountry.asStateFlow()

    init {
        detectCountryFromIp()
    }

    private fun determineInitialIranStatus(): Boolean {
        // Check saved preference first
        if (prefs.contains("is_iran")) {
            return prefs.getBoolean("is_iran", true)
        }

        // Fast system heuristic: Locale, TimeZone, Language
        val systemCountry = Locale.getDefault().country
        val systemLanguage = Locale.getDefault().language
        val timeZoneId = TimeZone.getDefault().id

        val isIranBySystem = systemCountry.equals("IR", ignoreCase = true) ||
                systemLanguage.equals("fa", ignoreCase = true) ||
                systemLanguage.equals("fas", ignoreCase = true) ||
                timeZoneId.contains("Tehran", ignoreCase = true) ||
                timeZoneId.contains("Iran", ignoreCase = true)

        Log.d("GeoLocationDetector", "Initial system heuristic: isIran=$isIranBySystem (country=$systemCountry, lang=$systemLanguage, tz=$timeZoneId)")
        return isIranBySystem
    }

    fun setIranUserMode(isIran: Boolean) {
        _isIranUser.value = isIran
        prefs.edit()
            .putBoolean("is_iran", isIran)
            .putString("detected_country", if (isIran) "IR" else "GLOBAL")
            .apply()
        Log.d("GeoLocationDetector", "Manual Iran user mode set to: $isIran")
    }

    fun detectCountryFromIp() {
        scope.launch {
            val country = fetchCountryFromIp()
            if (country != null) {
                val isIran = country.equals("IR", ignoreCase = true)
                _detectedCountry.value = country
                _isIranUser.value = isIran
                prefs.edit()
                    .putBoolean("is_iran", isIran)
                    .putString("detected_country", country)
                    .apply()
                Log.d("GeoLocationDetector", "IP Detection complete: country=$country, isIran=$isIran")
            } else {
                Log.d("GeoLocationDetector", "IP Detection unavailable, retaining state: ${_isIranUser.value}")
            }
        }
    }

    private suspend fun fetchCountryFromIp(): String? = withContext(Dispatchers.IO) {
        // Try country.is first
        var country = queryGeoApi("https://api.country.is", "country")
        if (country != null) return@withContext country

        // Fallback to ip-api.com
        country = queryGeoApi("http://ip-api.com/json", "countryCode")
        if (country != null) return@withContext country

        // Fallback to ipapi.co
        country = queryGeoApi("https://ipapi.co/json", "country_code")
        return@withContext country
    }

    private fun queryGeoApi(endpoint: String, jsonKey: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpoint)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "WallWar-Android")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                val json = JSONObject(response.toString())
                if (json.has(jsonKey)) {
                    json.getString(jsonKey)
                } else null
            } else null
        } catch (e: Exception) {
            Log.w("GeoLocationDetector", "Error querying $endpoint: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
