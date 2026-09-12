package com.example.musam.data

import androidx.compose.ui.graphics.Color
import com.example.musam.ui.theme.*

enum class AqiCategory(
    val label: String,
    val rangeText: String,
    val color: Color,
    val healthAdvice: String
) {
    GOOD(
        label = "Good",
        rangeText = "0 - 50",
        color = AqiGoodColor,
        healthAdvice = "Air quality is ideal for outdoor activities."
    ),
    MODERATE(
        label = "Moderate",
        rangeText = "51 - 100",
        color = AqiModerateColor,
        healthAdvice = "Acceptable air quality; unusually sensitive people should consider reducing prolonged outdoor exertion."
    ),
    SENSITIVE(
        label = "Sensitive",
        rangeText = "101 - 150",
        color = AqiSensitiveColor,
        healthAdvice = "Children, elderly, and individuals with respiratory issues should limit extended outdoor exposure."
    ),
    UNHEALTHY(
        label = "Poor",
        rangeText = "151 - 200",
        color = AqiUnhealthyColor,
        healthAdvice = "Increased likelihood of adverse effects. Wear N95 masks outdoors and keep windows closed."
    ),
    VERY_UNHEALTHY(
        label = "Very Poor",
        rangeText = "201 - 300",
        color = AqiVeryUnhealthyColor,
        healthAdvice = "Health alert: The risk of respiratory harm is high for everyone. Avoid prolonged outdoor exertion."
    ),
    SEVERE(
        label = "Severe / Hazardous",
        rangeText = "301+",
        color = AqiSevereColor,
        healthAdvice = "Emergency health conditions. Everyone must remain indoors with air purifiers operating."
    );

    companion object {
        fun fromIndex(index: Int): AqiCategory = when {
            index <= 50 -> GOOD
            index <= 100 -> MODERATE
            index <= 150 -> SENSITIVE
            index <= 200 -> UNHEALTHY
            index <= 300 -> VERY_UNHEALTHY
            else -> SEVERE
        }
    }
}

data class Pollutant(
    val name: String,
    val fullName: String,
    val concentration: Double,
    val unit: String = "µg/m³",
    val standardLimit: Double,
    val status: String
)

data class AqiData(
    val aqi: Int,
    val category: AqiCategory = AqiCategory.fromIndex(aqi),
    val primaryPollutant: String,
    val healthSummary: String,
    val maskAdvisory: String,
    val outdoorAdvisory: String,
    val airPurifierAdvisory: String,
    val ventilationAdvisory: String,
    val pollutants: List<Pollutant>
)

data class HourlyForecast(
    val time: String,
    val tempC: Double,
    val condition: String,
    val pop: Int, // Probability of precipitation %
    val windKmh: Double,
    val isNow: Boolean = false
)

data class DailyForecast(
    val day: String,
    val date: String,
    val minTempC: Double,
    val maxTempC: Double,
    val condition: String,
    val pop: Int,
    val summary: String
)

data class AtmosphereDetails(
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirection: String,
    val windGustKmh: Double,
    val uvIndex: Double,
    val uvCategory: String,
    val pressureHpa: Int,
    val visibilityKm: Double,
    val dewPointC: Double,
    val sunrise: String,
    val sunset: String,
    val cloudCoverPercent: Int
)

enum class AlertSeverity(val title: String, val color: Color) {
    GREEN("No Warning", Color(0xFF10B981)),
    YELLOW("Watch / Be Updated", Color(0xFFEAB308)),
    ORANGE("Alert / Be Prepared", Color(0xFFF97316)),
    RED("Warning / Take Action", Color(0xFFEF4444))
}

data class WeatherAlert(
    val id: String,
    val severity: AlertSeverity,
    val headline: String,
    val description: String,
    val issuedBy: String,
    val validUntil: String,
    val instructions: List<String>
)

data class CityLocation(
    val id: String,
    val name: String,
    val state: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val isFavorite: Boolean = false,
    val isCurrent: Boolean = false,
    val tempC: Double,
    val condition: String,
    val aqi: Int
)

enum class RadarLayer(val title: String, val description: String) {
    PRECIPITATION("Precipitation", "Doppler radar rain and thunderstorm tracking"),
    SATELLITE("Satellite Cloud", "INSAT-3D thermal cloud cover and moisture"),
    WIND("Wind Streams", "Near-surface atmospheric wind stream vectors"),
    AQI("AQI Heatmap", "Real-time PM2.5 and PM10 particulate dispersion")
}
