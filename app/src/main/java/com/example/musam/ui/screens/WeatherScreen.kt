package com.example.musam.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musam.R
import com.example.musam.data.*
import com.example.musam.ui.components.*

@Composable
fun WeatherScreen(
    city: CityLocation,
    aqiData: AqiData,
    hourlyForecasts: List<HourlyForecast>,
    dailyForecasts: List<DailyForecast>,
    atmosphere: AtmosphereDetails,
    alerts: List<WeatherAlert>,
    formatTemp: (Double) -> String,
    onNavigateToAqi: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("weather_screen")
    ) {
        // Hero Weather Banner
        WeatherHeroSection(
            city = city,
            formatTemp = formatTemp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Severe weather alert ticker if present
        if (alerts.isNotEmpty()) {
            val primaryAlert = alerts.first()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primaryAlert.severity.color.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToAlerts() }
                    .testTag("weather_alert_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Severe Weather Alert",
                        tint = primaryAlert.severity.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = primaryAlert.headline,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryAlert.severity.color
                        )
                        Text(
                            text = "Tap to view IMD safety guidelines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prominent AQI Gauge Card
        AqiGaugeCard(
            aqiData = aqiData,
            onViewDetail = onNavigateToAqi,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Hourly Forecast Row
        HourlyForecastRow(
            forecasts = hourlyForecasts,
            formatTemp = formatTemp,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 7-Day Forecast Section
        DailyForecastSection(
            forecasts = dailyForecasts,
            formatTemp = formatTemp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Atmospheric Conditions Details Grid
        AtmosphereGrid(
            details = atmosphere,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun WeatherHeroSection(
    city: CityLocation,
    formatTemp: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .testTag("weather_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Artwork
            Image(
                painter = painterResource(id = R.drawable.weather_hero_art),
                contentDescription = "Weather Illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay for Readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            // Hero Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Condition Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = city.condition,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Feels like
                    Text(
                        text = "Feels like ${formatTemp(city.tempC + 1.2)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Bottom Row: Large Temperature & Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = formatTemp(city.tempC),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 62.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "H: ${formatTemp(city.tempC + 3.5)}  •  L: ${formatTemp(city.tempC - 4.5)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Weather condition icon & descriptor
                    Column(horizontalAlignment = Alignment.End) {
                        val icon = getWeatherIcon(city.condition)
                        Icon(
                            imageVector = icon,
                            contentDescription = city.condition,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Live Conditions",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
