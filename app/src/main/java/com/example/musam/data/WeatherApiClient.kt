package com.example.musam.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WeatherBundleResult(
    val city: CityLocation,
    val aqiData: AqiData,
    val hourlyForecasts: List<HourlyForecast>,
    val dailyForecasts: List<DailyForecast>,
    val atmosphere: AtmosphereDetails,
    val alerts: List<WeatherAlert>
)

class WeatherApiClient(
    private val candidateHosts: List<String> = listOf(
        "http://10.0.2.2:3000",
        "http://10.0.2.2:8080",
        "http://localhost:3000",
        "http://127.0.0.1:3000"
    )
) {
    @Volatile
    private var activeHost: String? = null

    private fun fetchHttp(endpoint: String): String {
        val hostsToTry = if (activeHost != null) {
            listOf(activeHost!!) + candidateHosts.filter { it != activeHost }
        } else {
            candidateHosts
        }

        var lastException: Exception? = null

        for (host in hostsToTry) {
            val fullUrl = "$host$endpoint"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(fullUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 4500
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    activeHost = host
                    return response
                } else {
                    val errStream = connection.errorStream
                    val errMsg = errStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    lastException = RuntimeException("Server error ($responseCode): $errMsg")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        throw (lastException ?: RuntimeException("Failed to connect to weather backend server"))
    }

    suspend fun fetchWeatherBundle(lat: Double, lon: Double): Result<WeatherBundleResult> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = fetchHttp("/api/weather-bundle?lat=$lat&lon=$lon")
            val root = JSONObject(jsonStr)

            // 1. City / Current Weather
            val cityObj = root.optJSONObject("city") ?: JSONObject()
            val weatherObj = root.optJSONObject("weather") ?: JSONObject()

            val tempC = weatherObj.optDouble("tempC", cityObj.optDouble("tempC", 28.0))
            val feelsLikeC = weatherObj.optDouble("feelsLikeC", tempC + 1.2)
            val tempMinC = weatherObj.optDouble("tempMinC", tempC - 3.5)
            val tempMaxC = weatherObj.optDouble("tempMaxC", tempC + 3.5)
            val condition = weatherObj.optString("condition", cityObj.optString("condition", "Partly Cloudy"))
            val description = weatherObj.optString("description", condition)
            val iconCode = weatherObj.optString("iconCode", "02d")
            val sunrise = weatherObj.optString("sunrise", "06:00 AM")
            val sunset = weatherObj.optString("sunset", "06:30 PM")
            val humidity = weatherObj.optInt("humidity", 65)

            // 2. AQI
            val aqiObj = root.optJSONObject("aqi") ?: JSONObject()
            val aqiValue = aqiObj.optInt("aqi", cityObj.optInt("aqi", 75))
            val aqiCategory = AqiCategory.fromIndex(aqiValue)
            val primaryPollutant = aqiObj.optString("primaryPollutant", "PM2.5")
            val healthSummary = aqiObj.optString("healthSummary", aqiCategory.healthAdvice)
            val maskAdvisory = aqiObj.optString("maskAdvisory", "Wear mask during commute")
            val outdoorAdvisory = aqiObj.optString("outdoorAdvisory", "Safe for general activities")
            val airPurifierAdvisory = aqiObj.optString("airPurifierAdvisory", "Use air purifier in closed spaces")
            val ventilationAdvisory = aqiObj.optString("ventilationAdvisory", "Keep windows closed during peak traffic")

            val pollutantsArray = aqiObj.optJSONArray("pollutants") ?: JSONArray()
            val pollutants = mutableListOf<Pollutant>()
            for (i in 0 until pollutantsArray.length()) {
                val p = pollutantsArray.getJSONObject(i)
                pollutants.add(
                    Pollutant(
                        name = p.optString("name"),
                        fullName = p.optString("fullName"),
                        concentration = p.optDouble("concentration", 0.0),
                        unit = p.optString("unit", "µg/m³"),
                        standardLimit = p.optDouble("standardLimit", 60.0),
                        status = p.optString("status", "Moderate")
                    )
                )
            }

            val aqiData = AqiData(
                aqi = aqiValue,
                category = aqiCategory,
                primaryPollutant = primaryPollutant,
                healthSummary = healthSummary,
                maskAdvisory = maskAdvisory,
                outdoorAdvisory = outdoorAdvisory,
                airPurifierAdvisory = airPurifierAdvisory,
                ventilationAdvisory = ventilationAdvisory,
                pollutants = pollutants
            )

            // 3. Hourly Forecast
            val hourlyArray = root.optJSONArray("hourly") ?: JSONArray()
            val hourlyList = mutableListOf<HourlyForecast>()
            for (i in 0 until hourlyArray.length()) {
                val h = hourlyArray.getJSONObject(i)
                hourlyList.add(
                    HourlyForecast(
                        time = h.optString("time", "--:--"),
                        tempC = h.optDouble("tempC", tempC),
                        condition = h.optString("condition", condition),
                        pop = h.optInt("pop", 0),
                        windKmh = h.optDouble("windKmh", 12.0),
                        isNow = h.optBoolean("isNow", false),
                        icon = h.optString("icon", iconCode)
                    )
                )
            }

            // 4. Daily Forecast
            val dailyArray = root.optJSONArray("daily") ?: JSONArray()
            val dailyList = mutableListOf<DailyForecast>()
            for (i in 0 until dailyArray.length()) {
                val d = dailyArray.getJSONObject(i)
                val day = d.optString("day", "Day")
                val cond = d.optString("condition", condition)
                dailyList.add(
                    DailyForecast(
                        day = day,
                        date = d.optString("date", ""),
                        minTempC = d.optDouble("minTempC", tempC - 4.0),
                        maxTempC = d.optDouble("maxTempC", tempC + 3.0),
                        condition = cond,
                        pop = d.optInt("pop", 10),
                        summary = "$cond with comfortable winds"
                    )
                )
            }

            // 5. Atmosphere Details
            val atmoObj = root.optJSONObject("atmosphere") ?: JSONObject()
            val atmosphere = AtmosphereDetails(
                humidityPercent = atmoObj.optInt("humidityPercent", humidity),
                windSpeedKmh = atmoObj.optDouble("windSpeedKmh", 14.0),
                windDirection = atmoObj.optString("windDirection", "NW"),
                windGustKmh = atmoObj.optDouble("windGustKmh", 20.0),
                uvIndex = atmoObj.optDouble("uvIndex", 6.0),
                uvCategory = atmoObj.optString("uvCategory", "Moderate"),
                pressureHpa = atmoObj.optInt("pressureHpa", 1012),
                visibilityKm = atmoObj.optDouble("visibilityKm", 10.0),
                dewPointC = atmoObj.optDouble("dewPointC", tempC - 6.0),
                sunrise = atmoObj.optString("sunrise", sunrise),
                sunset = atmoObj.optString("sunset", sunset),
                cloudCoverPercent = atmoObj.optInt("cloudCoverPercent", 40)
            )

            // 6. Alerts
            val alertsArray = root.optJSONArray("alerts") ?: JSONArray()
            val alertsList = mutableListOf<WeatherAlert>()
            for (i in 0 until alertsArray.length()) {
                val a = alertsArray.getJSONObject(i)
                val sevStr = a.optString("severity", "YELLOW")
                val severity = when (sevStr.uppercase()) {
                    "RED" -> AlertSeverity.RED
                    "ORANGE" -> AlertSeverity.ORANGE
                    "GREEN" -> AlertSeverity.GREEN
                    else -> AlertSeverity.YELLOW
                }

                val instructionsArray = a.optJSONArray("instructions") ?: JSONArray()
                val instructions = mutableListOf<String>()
                for (j in 0 until instructionsArray.length()) {
                    instructions.add(instructionsArray.getString(j))
                }

                alertsList.add(
                    WeatherAlert(
                        id = a.optString("id", "alert_$i"),
                        severity = severity,
                        headline = a.optString("headline", "Weather Advisory"),
                        description = a.optString("description", ""),
                        issuedBy = a.optString("issuedBy", "IMD Meteorological Centre"),
                        validUntil = a.optString("validUntil", "Today"),
                        instructions = instructions
                    )
                )
            }

            val cityName = cityObj.optString("name", "Current City")
            val cityState = cityObj.optString("state", "India")

            val cityLocation = CityLocation(
                id = "${cityName.lowercase().replace("[^a-z0-9]".toRegex(), "_")}_${(lat*100).toInt()}_${(lon*100).toInt()}",
                name = cityName,
                state = cityState,
                country = "India",
                lat = lat,
                lon = lon,
                tempC = tempC,
                condition = description.replaceFirstChar { it.uppercase() },
                aqi = aqiValue,
                feelsLikeC = feelsLikeC,
                tempMinC = tempMinC,
                tempMaxC = tempMaxC,
                humidity = humidity,
                sunrise = sunrise,
                sunset = sunset,
                iconCode = iconCode
            )

            Result.success(
                WeatherBundleResult(
                    city = cityLocation,
                    aqiData = aqiData,
                    hourlyForecasts = hourlyList,
                    dailyForecasts = dailyList,
                    atmosphere = atmosphere,
                    alerts = alertsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCities(query: String): Result<List<CityLocation>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val jsonStr = fetchHttp("/api/search?q=$encodedQuery")
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CityLocation>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name")
                val state = obj.optString("state", obj.optString("country", "India"))
                val country = obj.optString("country", "IN")
                val lat = obj.optDouble("lat", 0.0)
                val lon = obj.optDouble("lon", 0.0)
                val id = obj.optString("id", "${name.lowercase()}_$i")

                list.add(
                    CityLocation(
                        id = id,
                        name = name,
                        state = state,
                        country = country,
                        lat = lat,
                        lon = lon,
                        tempC = 28.0,
                        condition = "Tap to load",
                        aqi = 50
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = fetchHttp("/api/reverse-geocode?lat=$lat&lon=$lon")
            val obj = JSONObject(jsonStr)
            val name = obj.optString("name", "Current Location")
            val state = obj.optString("state", "GPS Location")
            Result.success(Pair(name, state))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
