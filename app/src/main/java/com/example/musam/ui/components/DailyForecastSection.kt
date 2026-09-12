package com.example.musam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musam.data.DailyForecast

@Composable
fun DailyForecastSection(
    forecasts: List<DailyForecast>,
    formatTemp: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_forecast_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-Day Outlook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Weekly Trends",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val globalMin = forecasts.minOfOrNull { it.minTempC } ?: 15.0
            val globalMax = forecasts.maxOfOrNull { it.maxTempC } ?: 35.0
            val tempSpan = (globalMax - globalMin).coerceAtLeast(1.0)

            forecasts.forEachIndexed { index, item ->
                DailyForecastRowItem(
                    forecast = item,
                    globalMin = globalMin,
                    globalSpan = tempSpan,
                    formatTemp = formatTemp
                )
                if (index < forecasts.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRowItem(
    forecast: DailyForecast,
    globalMin: Double,
    globalSpan: Double,
    formatTemp: (Double) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day + Date
        Column(modifier = Modifier.width(68.dp)) {
            Text(
                text = forecast.day,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = forecast.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Icon + Rain chance
        Row(
            modifier = Modifier.width(74.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getWeatherIcon(forecast.condition),
                contentDescription = forecast.condition,
                tint = getWeatherIconTint(forecast.condition),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (forecast.pop > 20) {
                Text(
                    text = "${forecast.pop}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
            }
        }

        // Low Temp
        Text(
            text = formatTemp(forecast.minTempC),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )

        // Temperature Range Visual Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .padding(horizontal = 8.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
        ) {
            val startWeight = ((forecast.minTempC - globalMin) / globalSpan).coerceIn(0.0, 1.0).toFloat()
            val endWeight = ((forecast.maxTempC - globalMin) / globalSpan).coerceIn(0.0, 1.0).toFloat()
            val barSpan = (endWeight - startWeight).coerceAtLeast(0.15f)

            Row(modifier = Modifier.fillMaxSize()) {
                if (startWeight > 0.05f) {
                    Spacer(modifier = Modifier.weight(startWeight))
                }
                Box(
                    modifier = Modifier
                        .weight(barSpan)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFFF59E0B))
                            )
                        )
                )
                val remaining = (1f - (startWeight + barSpan)).coerceAtLeast(0f)
                if (remaining > 0.05f) {
                    Spacer(modifier = Modifier.weight(remaining))
                }
            }
        }

        // High Temp
        Text(
            text = formatTemp(forecast.maxTempC),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp)
        )
    }
}
