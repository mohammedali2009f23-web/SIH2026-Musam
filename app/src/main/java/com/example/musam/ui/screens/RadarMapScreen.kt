package com.example.musam.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musam.data.CityLocation
import com.example.musam.data.RadarLayer
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarMapScreen(
    currentCity: CityLocation,
    allCities: List<CityLocation>,
    selectedLayer: RadarLayer,
    onSelectLayer: (RadarLayer) -> Unit,
    radarFrame: Int,
    onSetRadarFrame: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    formatTemp: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val timelineLabels = listOf("-2 hrs", "-1 hr", "Live Now", "+30 min", "+1 hr")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("radar_map_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Layer Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadarLayer.values().forEach { layer ->
                val isSelected = layer == selectedLayer
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectLayer(layer) },
                    label = {
                        Text(
                            text = layer.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }

        // Radar Map Viewport
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("radar_viewport_card")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Interactive Canvas Radar Simulation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Background Grid / Lat-Long coordinates lines
                    val gridColor = Color(0xFF1E293B)
                    for (i in 1..5) {
                        val y = h * (i / 6f)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                    }
                    for (i in 1..4) {
                        val x = w * (i / 5f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                    }

                    // Draw stylized subcontinental map coastline / border outline
                    val borderPath = Path().apply {
                        moveTo(w * 0.25f, h * 0.12f)
                        lineTo(w * 0.45f, h * 0.10f)
                        lineTo(w * 0.70f, h * 0.20f)
                        lineTo(w * 0.85f, h * 0.35f)
                        lineTo(w * 0.80f, h * 0.55f)
                        lineTo(w * 0.60f, h * 0.85f)
                        lineTo(w * 0.45f, h * 0.92f)
                        lineTo(w * 0.35f, h * 0.75f)
                        lineTo(w * 0.22f, h * 0.55f)
                        lineTo(w * 0.18f, h * 0.35f)
                        close()
                    }
                    drawPath(
                        path = borderPath,
                        color = Color(0xFF334155),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw Radar Layer Phenomenon based on radarFrame animation offset
                    val frameShift = (radarFrame - 2) * 18f

                    when (selectedLayer) {
                        RadarLayer.PRECIPITATION -> {
                            // Convective rain cells / doppler bands
                            val cellCenter1 = Offset(w * 0.42f + frameShift, h * 0.40f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color.Transparent),
                                    center = cellCenter1,
                                    radius = w * 0.22f
                                ),
                                center = cellCenter1,
                                radius = w * 0.22f
                            )

                            val cellCenter2 = Offset(w * 0.65f + frameShift * 0.8f, h * 0.62f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4), Color.Transparent),
                                    center = cellCenter2,
                                    radius = w * 0.26f
                                ),
                                center = cellCenter2,
                                radius = w * 0.26f
                            )
                        }
                        RadarLayer.SATELLITE -> {
                            // Cloud moisture swirls
                            val cloudCenter = Offset(w * 0.5f + frameShift * 1.2f, h * 0.45f)
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.55f), Color(0xFF94A3B8).copy(alpha = 0.25f), Color.Transparent),
                                    center = cloudCenter,
                                    radius = w * 0.40f
                                ),
                                topLeft = Offset(cloudCenter.x - w * 0.35f, cloudCenter.y - h * 0.25f),
                                size = Size(w * 0.7f, h * 0.5f)
                            )
                        }
                        RadarLayer.WIND -> {
                            // Wind vector streamlines
                            for (row in 1..7) {
                                for (col in 1..6) {
                                    val start = Offset(col * (w / 7f) + (frameShift % 20), row * (h / 8f))
                                    val angle = Math.toRadians((45.0 + row * 8.0))
                                    val length = 24.dp.toPx()
                                    val end = Offset(
                                        start.x + (length * cos(angle)).toFloat(),
                                        start.y + (length * sin(angle)).toFloat()
                                    )
                                    drawLine(
                                        color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                                        start = start,
                                        end = end,
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }
                        RadarLayer.AQI -> {
                            // Smog / Particulate Heatmap
                            val delhiAqiCenter = Offset(w * 0.38f, h * 0.28f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF881337).copy(alpha = 0.8f), Color(0xFFEF4444).copy(alpha = 0.5f), Color.Transparent),
                                    center = delhiAqiCenter,
                                    radius = w * 0.28f
                                ),
                                center = delhiAqiCenter,
                                radius = w * 0.28f
                            )

                            val eastAqiCenter = Offset(w * 0.68f, h * 0.42f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.7f), Color(0xFF84CC16).copy(alpha = 0.4f), Color.Transparent),
                                    center = eastAqiCenter,
                                    radius = w * 0.24f
                                ),
                                center = eastAqiCenter,
                                radius = w * 0.24f
                            )
                        }
                    }
                }

                // City Markers Overlay
                val markerPositions = listOf(
                    Triple("delhi", 0.38f, 0.28f),
                    Triple("mumbai", 0.25f, 0.58f),
                    Triple("bengaluru", 0.40f, 0.78f),
                    Triple("kolkata", 0.72f, 0.42f),
                    Triple("srinagar", 0.32f, 0.12f),
                    Triple("jaipur", 0.30f, 0.35f),
                    Triple("chennai", 0.48f, 0.80f),
                    Triple("hyderabad", 0.44f, 0.64f)
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxW = maxWidth
                    val maxH = maxHeight

                    markerPositions.forEach { (cityId, relX, relY) ->
                        val city = allCities.firstOrNull { it.id == cityId }
                        if (city != null) {
                            val isSelected = city.id == currentCity.id
                            Box(
                                modifier = Modifier
                                    .offset(x = maxW * relX - 30.dp, y = maxH * relY - 14.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B).copy(alpha = 0.92f)
                                    )
                                    .clickable { onSelectCity(city) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                when {
                                                    city.aqi <= 50 -> Color(0xFF10B981)
                                                    city.aqi <= 100 -> Color(0xFF84CC16)
                                                    city.aqi <= 150 -> Color(0xFFF59E0B)
                                                    else -> Color(0xFFEF4444)
                                                },
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${city.name} ${formatTemp(city.tempC)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Legend at top right of viewport
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${selectedLayer.title} • IMD Doppler",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Timeline Slider & Playback Controls
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("radar_play_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play radar loop"
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = timelineLabels[radarFrame],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Radar Animation Timeline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reset to Live
                    OutlinedButton(
                        onClick = { onSetRadarFrame(2) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Live Now", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = radarFrame.toFloat(),
                    onValueChange = { onSetRadarFrame(it.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth().testTag("radar_timeline_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    timelineLabels.forEachIndexed { idx, label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (idx == radarFrame) FontWeight.Bold else FontWeight.Normal,
                            color = if (idx == radarFrame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
