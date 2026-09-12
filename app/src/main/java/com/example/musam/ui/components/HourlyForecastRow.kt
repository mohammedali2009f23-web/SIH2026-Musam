package com.example.musam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musam.data.HourlyForecast

@Composable
fun HourlyForecastRow(
    forecasts: List<HourlyForecast>,
    formatTemp: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hourly Forecast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Next 24 Hours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            forecasts.forEach { item ->
                HourlyItemCard(item = item, formatTemp = formatTemp)
            }
        }
    }
}

@Composable
private fun HourlyItemCard(
    item: HourlyForecast,
    formatTemp: (Double) -> String
) {
    val isCurrent = item.isNow
    val cardColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isCurrent) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 4.dp else 1.dp),
        modifier = Modifier
            .width(74.dp)
            .testTag("hourly_card_${item.time}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.time,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = subColor
            )

            val icon = getWeatherIcon(item.condition)
            Icon(
                imageVector = icon,
                contentDescription = item.condition,
                tint = if (isCurrent) Color.White else getWeatherIconTint(item.condition),
                modifier = Modifier.size(26.dp)
            )

            Text(
                text = formatTemp(item.tempC),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            // Rain probability
            if (item.pop > 10) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Rain chance",
                        tint = if (isCurrent) Color.White else Color(0xFF0284C7),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${item.pop}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCurrent) Color.White else Color(0xFF0284C7)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

fun getWeatherIcon(condition: String): ImageVector = when {
    condition.contains("Thunder", ignoreCase = true) -> Icons.Default.Thunderstorm
    condition.contains("Rain", ignoreCase = true) || condition.contains("Shower", ignoreCase = true) -> Icons.Default.WaterDrop
    condition.contains("Cloud", ignoreCase = true) -> Icons.Default.Cloud
    condition.contains("Mist", ignoreCase = true) || condition.contains("Haze", ignoreCase = true) -> Icons.Default.Grain
    condition.contains("Night", ignoreCase = true) -> Icons.Default.NightsStay
    else -> Icons.Default.WbSunny
}

fun getWeatherIconTint(condition: String): Color = when {
    condition.contains("Thunder", ignoreCase = true) -> Color(0xFF6366F1)
    condition.contains("Rain", ignoreCase = true) || condition.contains("Shower", ignoreCase = true) -> Color(0xFF0284C7)
    condition.contains("Cloud", ignoreCase = true) -> Color(0xFF64748B)
    condition.contains("Mist", ignoreCase = true) || condition.contains("Haze", ignoreCase = true) -> Color(0xFFD97706)
    condition.contains("Night", ignoreCase = true) -> Color(0xFF818CF8)
    else -> Color(0xFFF59E0B)
}
