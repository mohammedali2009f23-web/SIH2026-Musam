package com.example.musam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musam.data.AqiCategory
import com.example.musam.data.CityLocation
import com.example.musam.ui.components.getWeatherIcon
import com.example.musam.ui.components.getWeatherIconTint

@Composable
fun CitiesScreen(
    currentCity: CityLocation,
    allCities: List<CityLocation>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAddCity: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onToggleFavorite: (String) -> Unit,
    formatTemp: (Double) -> String,
    modifier: Modifier = Modifier
) {
    var newCityInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredCities = remember(allCities, searchQuery) {
        if (searchQuery.isBlank()) allCities
        else allCities.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.state.contains(searchQuery, ignoreCase = true)
        }
    }

    val favoriteCities = remember(filteredCities) {
        filteredCities.filter { it.isFavorite }
    }

    val otherCities = remember(filteredCities) {
        filteredCities.filter { !it.isFavorite }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cities_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Add City action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search city (Delhi, Mumbai, etc.)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("city_search_input")
            )

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("add_city_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add custom city")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (favoriteCities.isNotEmpty()) {
                item {
                    Text(
                        text = "Favorite Locations (${favoriteCities.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(favoriteCities, key = { it.id }) { city ->
                    CityCardItem(
                        city = city,
                        isSelected = city.id == currentCity.id,
                        onSelect = { onSelectCity(city) },
                        onToggleFavorite = { onToggleFavorite(city.id) },
                        formatTemp = formatTemp
                    )
                }
            }

            if (otherCities.isNotEmpty()) {
                item {
                    Text(
                        text = "All Locations (${otherCities.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(otherCities, key = { it.id }) { city ->
                    CityCardItem(
                        city = city,
                        isSelected = city.id == currentCity.id,
                        onSelect = { onSelectCity(city) },
                        onToggleFavorite = { onToggleFavorite(city.id) },
                        formatTemp = formatTemp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Location") },
            text = {
                Column {
                    Text(
                        "Enter the name of a city or region to monitor its real-time weather and air quality:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCityInput,
                        onValueChange = { newCityInput = it },
                        placeholder = { Text("e.g., Lucknow, Varanasi, Chandigarh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCityInput.isNotBlank()) {
                            onAddCity(newCityInput.trim())
                            newCityInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add & View")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CityCardItem(
    city: CityLocation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    formatTemp: (Double) -> String
) {
    val aqiCat = AqiCategory.fromIndex(city.aqi)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect() }
            .testTag("city_card_${city.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (city.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Toggle favorite",
                        tint = if (city.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (city.isCurrent) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${city.state} • ${city.condition}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Temperature & AQI Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = getWeatherIcon(city.condition),
                    contentDescription = city.condition,
                    tint = getWeatherIconTint(city.condition),
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = formatTemp(city.tempC),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = aqiCat.color.copy(alpha = 0.18f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${city.aqi}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = aqiCat.color
                        )
                        Text(
                            text = aqiCat.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = aqiCat.color
                        )
                    }
                }
            }
        }
    }
}
