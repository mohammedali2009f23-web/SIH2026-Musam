package com.example.musam.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WeatherRepository {

    private val initialCities = listOf(
        CityLocation("delhi", "New Delhi", "Delhi NCR", "India", 28.6139, 77.2090, isFavorite = true, isCurrent = true, 31.4, "Hazy Sunshine", 188),
        CityLocation("mumbai", "Mumbai", "Maharashtra", "India", 19.0760, 72.8777, isFavorite = true, isCurrent = false, 29.8, "Scattered Showers", 74),
        CityLocation("bengaluru", "Bengaluru", "Karnataka", "India", 12.9716, 77.5946, isFavorite = true, isCurrent = false, 24.2, "Partly Cloudy", 42),
        CityLocation("kolkata", "Kolkata", "West Bengal", "India", 22.5726, 88.3639, isFavorite = false, isCurrent = false, 32.0, "Humid Thunderstorm", 112),
        CityLocation("srinagar", "Srinagar", "Jammu & Kashmir", "India", 34.0837, 74.7973, isFavorite = true, isCurrent = false, 19.5, "Clear Sky", 32),
        CityLocation("jaipur", "Jaipur", "Rajasthan", "India", 26.9124, 75.7873, isFavorite = false, isCurrent = false, 34.8, "Sunny & Warm", 145),
        CityLocation("chennai", "Chennai", "Tamil Nadu", "India", 13.0827, 80.2707, isFavorite = false, isCurrent = false, 30.5, "Breezy & Humid", 68),
        CityLocation("shimla", "Shimla", "Himachal Pradesh", "India", 31.1048, 77.1734, isFavorite = false, isCurrent = false, 17.2, "Misty Mountain Breeze", 28),
        CityLocation("hyderabad", "Hyderabad", "Telangana", "India", 17.3850, 78.4867, isFavorite = false, isCurrent = false, 28.4, "Passing Clouds", 86),
        CityLocation("pune", "Pune", "Maharashtra", "India", 18.5204, 73.8567, isFavorite = false, isCurrent = false, 26.0, "Pleasant Wind", 52)
    )

    private val _cities = MutableStateFlow<List<CityLocation>>(initialCities)
    val cities: StateFlow<List<CityLocation>> = _cities.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityLocation>(initialCities.first())
    val selectedCity: StateFlow<CityLocation> = _selectedCity.asStateFlow()

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

    fun addCity(name: String, state: String = "India"): CityLocation {
        val id = name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis() % 1000
        val baseTemp = (220 + (name.hashCode() % 140).coerceAtLeast(0)) / 10.0
        val baseAqi = (40 + (name.hashCode() % 220).coerceAtLeast(0))
        val newCity = CityLocation(
            id = id,
            name = name,
            state = state,
            country = "India",
            lat = 20.0 + (name.hashCode() % 10),
            lon = 78.0 + (name.hashCode() % 10),
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

    fun getAqiData(city: CityLocation): AqiData {
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

        val mask = when (category) {
            AqiCategory.GOOD, AqiCategory.MODERATE -> "Not required for general public."
            AqiCategory.SENSITIVE -> "Recommended for children, elderly, and asthmatic patients."
            AqiCategory.UNHEALTHY -> "N95 / FFP2 mask strongly recommended outdoors."
            AqiCategory.VERY_UNHEALTHY, AqiCategory.SEVERE -> "N95 particulate respirator mandatory when stepping outside."
        }

        val outdoor = when (category) {
            AqiCategory.GOOD -> "Ideal for all outdoor running, jogging, and sports."
            AqiCategory.MODERATE -> "Comfortable for outdoor workouts, keep hydrated."
            AqiCategory.SENSITIVE -> "Reduce heavy prolonged outdoor sports in early mornings."
            AqiCategory.UNHEALTHY -> "Shift physical workouts indoors; avoid peak traffic hours."
            AqiCategory.VERY_UNHEALTHY -> "Strictly avoid outdoor jogging, running, and heavy physical labor."
            AqiCategory.SEVERE -> "Do not step outside. Hazardous particulate levels present."
        }

        val purifier = when (category) {
            AqiCategory.GOOD, AqiCategory.MODERATE -> "Air purifier optional; natural cross-ventilation encouraged."
            AqiCategory.SENSITIVE -> "Run HEPA air purifier on low in bedrooms."
            AqiCategory.UNHEALTHY -> "Operate True HEPA filtration in all active rooms."
            AqiCategory.VERY_UNHEALTHY, AqiCategory.SEVERE -> "Keep True HEPA air purifier on continuous high performance."
        }

        val ventilation = when (category) {
            AqiCategory.GOOD -> "Keep windows open for fresh mountain/breeze air intake."
            AqiCategory.MODERATE -> "Open windows during afternoon hours with favorable wind."
            AqiCategory.SENSITIVE -> "Brief ventilation only during midday hours."
            AqiCategory.UNHEALTHY, AqiCategory.VERY_UNHEALTHY, AqiCategory.SEVERE -> "Seal windows and doors to prevent outdoor particulate ingress."
        }

        return AqiData(
            aqi = aqiValue,
            category = category,
            primaryPollutant = if (pm25Val > 60) "PM2.5" else "PM10",
            healthSummary = category.healthAdvice,
            maskAdvisory = mask,
            outdoorAdvisory = outdoor,
            airPurifierAdvisory = purifier,
            ventilationAdvisory = ventilation,
            pollutants = pollutants
        )
    }

    fun getHourlyForecast(city: CityLocation): List<HourlyForecast> {
        val baseTemp = city.tempC
        return listOf(
            HourlyForecast("Now", baseTemp, city.condition, 20, 14.0, isNow = true),
            HourlyForecast("09:00", (baseTemp + 0.8), "Sunny / Bright", 15, 12.0),
            HourlyForecast("11:00", (baseTemp + 2.1), "Scattered Clouds", 25, 16.0),
            HourlyForecast("13:00", (baseTemp + 3.4), "Hazy Sunshine", 30, 18.0),
            HourlyForecast("15:00", (baseTemp + 2.8), "Partly Cloudy", 40, 22.0),
            HourlyForecast("17:00", (baseTemp + 1.2), "Passing Shower", 65, 20.0),
            HourlyForecast("19:00", (baseTemp - 1.1), "Cloudy Evening", 35, 14.0),
            HourlyForecast("21:00", (baseTemp - 2.6), "Clear Night", 10, 11.0),
            HourlyForecast("23:00", (baseTemp - 3.8), "Cool Breeze", 5, 9.0),
            HourlyForecast("01:00", (baseTemp - 4.5), "Misty Sky", 10, 8.0),
            HourlyForecast("03:00", (baseTemp - 5.0), "Misty Night", 15, 7.0),
            HourlyForecast("05:00", (baseTemp - 4.8), "Dawn Sunrise", 20, 10.0),
            HourlyForecast("07:00", (baseTemp - 2.0), "Morning Warmth", 15, 12.0)
        )
    }

    fun getDailyForecast(city: CityLocation): List<DailyForecast> {
        val baseTemp = city.tempC
        return listOf(
            DailyForecast("Today", "12 Sep", baseTemp - 5.0, baseTemp + 3.5, city.condition, 35, "Warm afternoon with mild evening clouds"),
            DailyForecast("Sat", "13 Sep", baseTemp - 4.5, baseTemp + 4.0, "Partly Cloudy", 20, "Gentle easterly wind with bright sunny intervals"),
            DailyForecast("Sun", "14 Sep", baseTemp - 4.0, baseTemp + 2.8, "Thunderstorm", 75, "Convective monsoon shower expected in afternoon"),
            DailyForecast("Mon", "15 Sep", baseTemp - 5.5, baseTemp + 2.0, "Scattered Showers", 60, "Cloudy skies with intermittent light rain"),
            DailyForecast("Tue", "16 Sep", baseTemp - 4.8, baseTemp + 3.2, "Mostly Sunny", 25, "Clear sky with rising afternoon temperatures"),
            DailyForecast("Wed", "17 Sep", baseTemp - 4.2, baseTemp + 4.1, "Hazy Warmth", 30, "Mild smog layer during early morning commute"),
            DailyForecast("Thu", "18 Sep", baseTemp - 5.0, baseTemp + 3.8, "Clear Sky", 15, "Dry north-westerly breeze and comfortable evening")
        )
    }

    fun getAtmosphereDetails(city: CityLocation): AtmosphereDetails {
        val isRainy = city.condition.contains("Rain") || city.condition.contains("Shower")
        val humidity = if (isRainy) 84 else 62
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
            sunrise = "05:54 AM",
            sunset = "06:38 PM",
            cloudCoverPercent = if (isRainy) 78 else 38
        )
    }

    fun getAlerts(city: CityLocation): List<WeatherAlert> {
        val list = mutableListOf<WeatherAlert>()
        if (city.aqi >= 150) {
            list.add(
                WeatherAlert(
                    id = "aqi_alert_${city.id}",
                    severity = AlertSeverity.ORANGE,
                    headline = "High Air Pollution Advisory for ${city.name}",
                    description = "Concentration of PM2.5 has breached 90 µg/m³. Atmospheric stagnation and temperature inversion are trapping pollutants near ground level.",
                    issuedBy = "Central Pollution Control Board (CPCB)",
                    validUntil = "Today, 11:59 PM IST",
                    instructions = listOf(
                        "Wear certified N95 respirators if venturing outdoors.",
                        "Sensitive individuals must avoid strenuous outdoor exercises.",
                        "Operate indoor air purifiers with HEPA filtration.",
                        "Report any garbage burning via the CPCB Sameer mobile portal."
                    )
                )
            )
        }
        if (city.tempC > 33.0 || city.condition.contains("Warm")) {
            list.add(
                WeatherAlert(
                    id = "heat_alert_${city.id}",
                    severity = AlertSeverity.YELLOW,
                    headline = "High Temperature Watch for ${city.name}",
                    description = "Maximum daytime temperature is 3°C above normal climatological average. Peak radiant solar heat from 12:00 PM to 3:30 PM.",
                    issuedBy = "India Meteorological Department (IMD)",
                    validUntil = "Tomorrow, 06:00 PM IST",
                    instructions = listOf(
                        "Drink adequate water and electrolytes throughout the day.",
                        "Wear light, loose-fitting cotton clothing.",
                        "Use umbrella or wide-brim hat during peak afternoon hours."
                    )
                )
            )
        }
        if (city.condition.contains("Showers") || city.condition.contains("Thunderstorm") || city.condition.contains("Rain")) {
            list.add(
                WeatherAlert(
                    id = "rain_alert_${city.id}",
                    severity = AlertSeverity.YELLOW,
                    headline = "Thunderstorm & Lightning Warning",
                    description = "Scattered thunderstorms accompanied by lightning and gusty winds (30-40 kmph) likely over parts of ${city.name} district.",
                    issuedBy = "IMD Regional Meteorological Centre",
                    validUntil = "Tonight, 09:00 PM IST",
                    instructions = listOf(
                        "Do not take shelter under isolated tall trees during thunderstorm.",
                        "Unplug sensitive electronic devices during severe lightning.",
                        "Exercise caution while driving on waterlogged roads."
                    )
                )
            )
        }
        return list
    }
}
