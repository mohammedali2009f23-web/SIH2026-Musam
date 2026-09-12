package com.example.musam.ui.components

import androidx.compose.foundation.layout.*
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
import com.example.musam.data.AtmosphereDetails

@Composable
fun AtmosphereGrid(
    details: AtmosphereDetails,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("atmosphere_grid")
    ) {
        Text(
            text = "Atmospheric Conditions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AtmosphereCard(
                title = "UV Index",
                value = "${details.uvIndex}",
                subtitle = details.uvCategory,
                icon = Icons.Default.WbSunny,
                iconTint = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
            AtmosphereCard(
                title = "Wind",
                value = "${details.windSpeedKmh} km/h",
                subtitle = "${details.windDirection} • Gusts ${details.windGustKmh.toInt()} km/h",
                icon = Icons.Default.Air,
                iconTint = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AtmosphereCard(
                title = "Humidity",
                value = "${details.humidityPercent}%",
                subtitle = "Dew point ${details.dewPointC.toInt()}°C",
                icon = Icons.Default.WaterDrop,
                iconTint = Color(0xFF0EA5E9),
                modifier = Modifier.weight(1f)
            )
            AtmosphereCard(
                title = "Pressure",
                value = "${details.pressureHpa} hPa",
                subtitle = "Standard atmospheric",
                icon = Icons.Default.Speed,
                iconTint = Color(0xFF6366F1),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AtmosphereCard(
                title = "Visibility",
                value = "${details.visibilityKm} km",
                subtitle = if (details.visibilityKm < 5.0) "Reduced by haze" else "Clear horizon",
                icon = Icons.Default.Visibility,
                iconTint = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            AtmosphereCard(
                title = "Sun Schedule",
                value = details.sunset,
                subtitle = "Sunrise ${details.sunrise}",
                icon = Icons.Default.WbTwilight,
                iconTint = Color(0xFFEA580C),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AtmosphereCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
