package com.example.musam.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherRepository(
    private val apiClient: WeatherApiClient = WeatherApiClient()
) {

    private val initialCities = listOf(
        CityLocation("delhi", "New Delhi", "Delhi NCR", "India", 28.6139, 77.2090, isFavorite = true, isCurrent = true, 29.0, "Partly Cloudy", 78, feelsLikeC = 35.0, tempMinC = 28.0, tempMaxC = 34.0, humidity = 75, sunrise = "06:04 AM", sunset = "06:30 PM", iconCode = "03d"),
        CityLocation("mumbai", "Mumbai", "Maharashtra", "India", 19.0760, 72.8777, isFavorite = true, isCurrent = false, 29.8, "Scattered Showers", 74, feelsLikeC = 34.0, tempMinC = 26.0, tempMaxC = 32.0, humidity = 82, sunrise = "06:25 AM", sunset = "06:45 PM", iconCode = "10d"),
        CityLocation("bengaluru", "Bengaluru", "Karnataka", "India", 12.9716, 77.5946, isFavorite = true, isCurrent = false, 24.2, "Passing Clouds", 42, feelsLikeC = 25.0, tempMinC = 20.0, tempMaxC = 28.0, humidity = 68, sunrise = "06:08 AM", sunset = "06:23 PM", iconCode = "02d"),
        CityLocation("kolkata", "Kolkata", "West Bengal", "India", 22.5726, 88.3639, isFavorite = false, isCurrent = false, 32.0, "Humid Thunderstorm", 112, feelsLikeC = 38.0, tempMinC = 27.0, tempMaxC = 35.0, humidity = 88, sunrise = "05:22 AM", sunset = "05:43 PM", iconCode = "11d"),
        CityLocation("srinagar", "Srinagar", "Jammu & Kashmir", "India", 34.0837, 74.7973, isFavorite = true, isCurrent = false, 19.5, "Clear Sky", 32, feelsLikeC = 19.0, tempMinC = 12.0, tempMaxC = 23.0, humidity = 50, sunrise = "06:14 AM", sunset = "06:48 PM", iconCode = "01d"),
        CityLocation("jaipur", "Jaipur", "Rajasthan", "India", 26.9124, 75.7873, isFavorite = false, isCurrent = false, 34.8, "Sunny & Warm", 145, feelsLikeC = 37.0, tempMinC = 25.0, tempMaxC = 38.0, humidity = 45, sunrise = "06:12 AM", sunset = "06:38 PM", iconCode = "01d"),
        CityLocation("chennai", "Chennai", "Tamil Nadu", "India", 13.0827, 80.2707, isFavorite = false, isCurrent = false, 30.5, "Breezy & Humid", 68, feelsLikeC = 36.0, tempMinC = 27.0, tempMaxC = 33.0, humidity = 79, sunrise = "05:58 AM", sunset = "06:12 PM", iconCode = "02d"),
        CityLocation("shimla", "Shimla", "Himachal Pradesh", "India", 31.1048, 77.1734, isFavorite = false, isCurrent = false, 17.2, "Misty Mountain Breeze", 28, feelsLikeC = 17.0, tempMinC = 12.0, tempMaxC = 21.0, humidity = 60, sunrise = "06:05 AM", sunset = "06:33 PM", iconCode = "50d"),
        CityLocation("hyderabad", "Hyderabad", "Telangana", "India", 17.3850, 78.4867, isFavorite = false, isCurrent = false, 28.4, "Passing Clouds", 86, feelsLikeC = 30.0, tempMinC = 22.0, tempMaxC = 31.0, humidity = 70, sunrise = "06:06 AM", sunset = "06:24 PM", iconCode = "03d"),
        CityLocation("pune", "Pune", "Maharashtra", "India", 18.5204, 73.8567, isFavorite = false, isCurrent = false, 26.0, "Pleasant Wind", 52, feelsLikeC = 27.0, tempMinC = 21.0, tempMaxC = 29.0, humidity = 65, sunrise = "06:23 AM", sunset = "06:42 PM", iconCode = "02d")
    )

    private val _cities = MutableStateFlow<List<CityLocation>>(initialCities)
    val cities: StateFlow<List<CityLocation>> = _cities.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityLocation>(initialCities.first())
    val selectedCity: StateFlow<CityLocation> = _selectedCity.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Cached bundle results mapped by city ID
    private val bundleCache = mutableMapOf<String, WeatherBundleResult>()

    fun clearError() {
        _errorMessage.value = null
    }

    fun selectCity(city: CityLocation) {
        _selectedCity.value = city
    }

    fun toggleFavorite(cityId: String) {
        _cities.value = _cities.value.map { city ->
            if (city.id == cityId) city.copy(isFavorite = !city.isFavorite) else city
        }
        if (_selectedCity.value.id == cityId) {
            _selectedCity.value = _selectedCity.value.copy(isFavorite = !_selectedCity.value.isFavorite)
        }
    }

    fun addCity(name: String, state: String = "India", lat: Double = 20.0, lon: Double = 78.0): CityLocation {
        val id = name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis() % 1000
        val baseTemp = (220 + (name.hashCode() % 140).coerceAtLeast(0)) / 10.0
        val baseAqi = (40 + (name.hashCode() % 220).coerceAtLeast(0))
        val newCity = CityLocation(
            id = id,
            name = name,
            state = state,
            country = "India",
            lat = lat,
            lon = lon,
            isFavorite = true,
            isCurrent = false,
            tempC = baseTemp,
            condition = "Partly Cloudy",
            aqi = baseAqi
        )
        _cities.value = listOf(newCity) + _cities.value
        _selectedCity.value = newCity
        return newCity
    }

    suspend fun loadWeatherForCity(city: CityLocation): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        val result = apiClient.fetchWeatherBundle(city.lat, city.lon)
        _isLoading.value = false

        return if (result.isSuccess) {
            val bundle = result.getOrThrow()
            bundleCache[city.id] = bundle

            // Update city with live values from API
            val updatedCity = city.copy(
                tempC = bundle.city.tempC,
                feelsLikeC = bundle.city.feelsLikeC,
                tempMinC = bundle.city.tempMinC,
                tempMaxC = bundle.city.tempMaxC,
                condition = bundle.city.condition,
                aqi = bundle.city.aqi,
                humidity = bundle.city.humidity,
                sunrise = bundle.city.sunrise,
                sunset = bundle.city.sunset,
                iconCode = bundle.city.iconCode
            )

            _selectedCity.value = updatedCity
            _cities.value = _cities.value.map { if (it.id == city.id) updatedCity else it }
            true
        } else {
            val err = result.exceptionOrNull()?.message ?: "Failed to connect to weather service"
            _errorMessage.value = err
            false
        }
    }

    suspend fun setLocationFromGps(lat: Double, lon: Double): CityLocation {
        _isLoading.value = true
        _errorMessage.value = null

        var resolvedName = "Current Location"
        var resolvedState = "GPS Location"

        val geoResult = apiClient.reverseGeocode(lat, lon)
        if (geoResult.isSuccess) {
            val pair = geoResult.getOrThrow()
            resolvedName = pair.first
            resolvedState = pair.second
        }

        val gpsCity = CityLocation(
            id = "gps_current_location",
            name = resolvedName,
            state = resolvedState,
            country = "India",
            lat = lat,
            lon = lon,
            isFavorite = true,
            isCurrent = true,
            tempC = 28.0,
            condition = "Locating...",
            aqi = 60
        )

        // Remove old GPS city if present, prepend new GPS city
        val filtered = _cities.value.filter { it.id != "gps_current_location" }
        _cities.value = listOf(gpsCity) + filtered
        _selectedCity.value = gpsCity

        loadWeatherForCity(gpsCity)
        return gpsCity
    }

    suspend fun searchCitiesOnline(query: String): List<CityLocation> {
        val result = apiClient.searchCities(query)
        return if (result.isSuccess) {
            result.getOrThrow()
        } else {
            // Local fallback filter
            _cities.value.filter {
                it.name.contains(query, ignoreCase = true) || it.state.contains(query, ignoreCase = true)
            }
        }
    }

    fun getAqiData(city: CityLocation): AqiData {
        val cached = bundleCache[city.id]
        if (cached != null) {
            return cached.aqiData
        }

        val aqiValue = city.aqi
        val category = AqiCategory.fromIndex(aqiValue)
        val pm25Val = (aqiValue * 0.45).coerceAtLeast(12.0)
        val pm10Val = (aqiValue * 0.85).coerceAtLeast(25.0)
        val no2Val = (25.0 + (aqiValue * 0.15)).coerceAtMost(180.0)
        val so2Val = (8.0 + (aqiValue * 0.08)).coerceAtMost(80.0)
        val coVal = (0.5 + (aqiValue * 0.008)).coerceAtMost(6.0)
        val o3Val = (35.0 + (aqiValue * 0.12)).coerceAtMost(160.0)

        val pollutants = listOf(
            Pollutant("PM2.5", "Fine Particulate Matter (<2.5 µm)", pm25Val, "µg/m³", 60.0, if (pm25Val <= 30) "Good" else if (pm25Val <= 60) "Moderate" else "High"),
            Pollutant("PM10", "Coarse Particulate Matter (<10 µm)", pm10Val, "µg/m³", 100.0, if (pm10Val <= 50) "Good" else if (pm10Val <= 100) "Moderate" else "High"),
            Pollutant("NO₂", "Nitrogen Dioxide", no2Val, "µg/m³", 80.0, if (no2Val <= 40) "Good" else if (no2Val <= 80) "Moderate" else "Elevated"),
            Pollutant("SO₂", "Sulfur Dioxide", so2Val, "µg/m³", 80.0, if (so2Val <= 40) "Good" else "Acceptable"),
            Pollutant("CO", "Carbon Monoxide", coVal, "mg/m³", 2.0, if (coVal <= 1.0) "Good" else if (coVal <= 2.0) "Moderate" else "Unhealthy"),
            Pollutant("O₃", "Ground-Level Ozone", o3Val, "µg/m³", 100.0, if (o3Val <= 50) "Good" else if (o3Val <= 100) "Moderate" else "High")
        )

        return AqiData(
            aqi = aqiValue,
            category = category,
            primaryPollutant = if (pm25Val > 60) "PM2.5" else "PM10",
            healthSummary = category.healthAdvice,
            maskAdvisory = if (aqiValue <= 100) "Not required" else "N95 recommended outdoors",
            outdoorAdvisory = if (aqiValue <= 100) "Safe for all outdoor activities" else "Limit heavy physical exertion outdoors",
            airPurifierAdvisory = if (aqiValue <= 100) "Optional" else "Recommended in bedrooms",
            ventilationAdvisory = if (aqiValue <= 100) "Open windows" else "Keep windows closed",
            pollutants = pollutants
        )
    }

    fun getHourlyForecast(city: CityLocation): List<HourlyForecast> {
        val cached = bundleCache[city.id]
        if (cached != null && cached.hourlyForecasts.isNotEmpty()) {
            return cached.hourlyForecasts
        }

        val baseTemp = city.tempC
        return listOf(
            HourlyForecast("Now", baseTemp, city.condition, 20, 14.0, isNow = true, icon = city.iconCode),
            HourlyForecast("09:00", (baseTemp + 0.8), "Sunny / Bright", 15, 12.0, icon = "01d"),
            HourlyForecast("11:00", (baseTemp + 2.1), "Scattered Clouds", 25, 16.0, icon = "02d"),
            HourlyForecast("13:00", (baseTemp + 3.4), "Hazy Sunshine", 30, 18.0, icon = "01d"),
            HourlyForecast("15:00", (baseTemp + 2.8), "Partly Cloudy", 40, 22.0, icon = "03d"),
            HourlyForecast("17:00", (baseTemp + 1.2), "Passing Shower", 65, 20.0, icon = "10d"),
            HourlyForecast("19:00", (baseTemp - 1.1), "Cloudy Evening", 35, 14.0, icon = "04n"),
            HourlyForecast("21:00", (baseTemp - 2.6), "Clear Night", 10, 11.0, icon = "01n")
        )
    }

    fun getDailyForecast(city: CityLocation): List<DailyForecast> {
        val cached = bundleCache[city.id]
        if (cached != null && cached.dailyForecasts.isNotEmpty()) {
            return cached.dailyForecasts
        }

        val baseTemp = city.tempC
        return listOf(
            DailyForecast("Today", "12 Sep", baseTemp - 4.0, baseTemp + 3.5, city.condition, 35, "Warm afternoon with mild evening clouds"),
            DailyForecast("Sat", "13 Sep", baseTemp - 4.5, baseTemp + 4.0, "Partly Cloudy", 20, "Gentle easterly wind with bright sunny intervals"),
            DailyForecast("Sun", "14 Sep", baseTemp - 4.0, baseTemp + 2.8, "Thunderstorm", 75, "Convective monsoon shower expected in afternoon"),
            DailyForecast("Mon", "15 Sep", baseTemp - 5.5, baseTemp + 2.0, "Scattered Showers", 60, "Cloudy skies with intermittent light rain"),
            DailyForecast("Tue", "16 Sep", baseTemp - 4.8, baseTemp + 3.2, "Mostly Sunny", 25, "Clear sky with rising afternoon temperatures"),
            DailyForecast("Wed", "17 Sep", baseTemp - 4.2, baseTemp + 4.1, "Hazy Warmth", 30, "Mild smog layer during early morning commute"),
            DailyForecast("Thu", "18 Sep", baseTemp - 5.0, baseTemp + 3.8, "Clear Sky", 15, "Dry north-westerly breeze and comfortable evening")
        )
    }

    fun getAtmosphereDetails(city: CityLocation): AtmosphereDetails {
        val cached = bundleCache[city.id]
        if (cached != null) {
            return cached.atmosphere
        }

        val isRainy = city.condition.contains("Rain") || city.condition.contains("Shower")
        val humidity = if (isRainy) 84 else city.humidity
        val uv = if (isRainy) 4.2 else 8.4
        return AtmosphereDetails(
            humidityPercent = humidity,
            windSpeedKmh = 16.5,
            windDirection = "NW 315°",
            windGustKmh = 28.0,
            uvIndex = uv,
            uvCategory = if (uv > 8.0) "Very High" else if (uv > 5.0) "Moderate" else "Low",
            pressureHpa = 1011,
            visibilityKm = if (city.aqi > 150) 3.5 else 9.0,
            dewPointC = (city.tempC - 6.2),
            sunrise = city.sunrise,
            sunset = city.sunset,
            cloudCoverPercent = if (isRainy) 78 else 38
        )
    }

    fun getAlerts(city: CityLocation): List<WeatherAlert> {
        val cached = bundleCache[city.id]
        if (cached != null && cached.alerts.isNotEmpty()) {
            return cached.alerts
        }

        val list = mutableListOf<WeatherAlert>()
        if (city.aqi >= 150) {
            list.add(
                WeatherAlert(
                    id = "aqi_alert_${city.id}",
                    severity = AlertSeverity.ORANGE,
                    headline = "High Air Pollution Advisory for ${city.name}",
                    description = "Concentration of PM2.5 has breached safe limits. Stagnant air is trapping pollutants near ground level.",
                    issuedBy = "Central Pollution Control Board (CPCB)",
                    validUntil = "Today, 11:59 PM IST",
                    instructions = listOf(
                        "Wear certified N95 respirators if venturing outdoors.",
                        "Sensitive individuals must avoid strenuous outdoor exercises.",
                        "Operate indoor air purifiers with HEPA filtration."
                    )
                )
            )
        }
        if (city.tempC > 34.0 || city.condition.contains("Warm")) {
            list.add(
                WeatherAlert(
                    id = "heat_alert_${city.id}",
                    severity = AlertSeverity.YELLOW,
                    headline = "High Temperature Watch for ${city.name}",
                    description = "Maximum daytime temperature is elevated. Peak radiant solar heat from 12:00 PM to 3:30 PM.",
                    issuedBy = "India Meteorological Department (IMD)",
                    validUntil = "Today, 06:00 PM IST",
                    instructions = listOf(
                        "Drink adequate water and electrolytes throughout the day.",
                        "Wear light, loose-fitting cotton clothing.",
                        "Use umbrella or wide-brim hat during peak afternoon hours."
                    )
                )
            )
        }
        return list
    }
}
