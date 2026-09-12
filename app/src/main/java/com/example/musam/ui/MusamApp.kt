package com.example.musam.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musam.ui.components.WeatherTopBar
import com.example.musam.ui.screens.*
import com.example.musam.ui.viewmodel.WeatherViewModel
import com.example.musam.utils.LocationHelper

enum class MusamDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    WEATHER("Weather", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
    AQI("AQI", Icons.Filled.Air, Icons.Outlined.Air),
    RADAR("Radar", Icons.Filled.Map, Icons.Outlined.Map),
    ALERTS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    CITIES("Cities", Icons.Filled.LocationCity, Icons.Outlined.LocationCity)
}

@Composable
fun MusamApp(
    viewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(MusamDestination.WEATHER) }

    val currentCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val allCities by viewModel.cities.collectAsStateWithLifecycle()
    val isCelsius by viewModel.isCelsius.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val locationPermissionDenied by viewModel.locationPermissionDenied.collectAsStateWithLifecycle()

    val currentAqi by viewModel.currentAqi.collectAsStateWithLifecycle()
    val hourlyForecast by viewModel.hourlyForecast.collectAsStateWithLifecycle()
    val dailyForecast by viewModel.dailyForecast.collectAsStateWithLifecycle()
    val atmosphere by viewModel.atmosphere.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    val selectedRadarLayer by viewModel.selectedRadarLayer.collectAsStateWithLifecycle()
    val radarFrame by viewModel.radarFrame.collectAsStateWithLifecycle()
    val isRadarPlaying by viewModel.isRadarPlaying.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            LocationHelper.getCurrentLocation(
                context = context,
                onLocationReceived = { lat, lon ->
                    viewModel.onGpsLocationReceived(lat, lon)
                },
                onError = {
                    // Fallback to default city gracefully
                }
            )
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    // Attempt location fetch if permission is already granted on launch
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            LocationHelper.getCurrentLocation(
                context = context,
                onLocationReceived = { lat, lon ->
                    viewModel.onGpsLocationReceived(lat, lon)
                },
                onError = { /* Keep default starting city */ }
            )
        } else {
            // Prompt for location permissions on first launch
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            WeatherTopBar(
                city = currentCity,
                isCelsius = isCelsius,
                onToggleUnit = { viewModel.toggleUnit() },
                onRefresh = { viewModel.refresh() },
                onOpenSearch = { currentDestination = MusamDestination.CITIES },
                onToggleFavorite = { viewModel.toggleFavorite(currentCity.id) }
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                MusamDestination.values().forEach { destination ->
                    val isSelected = destination == currentDestination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(destination.title) },
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                MusamDestination.WEATHER -> {
                    WeatherScreen(
                        city = currentCity,
                        aqiData = currentAqi,
                        hourlyForecasts = hourlyForecast,
                        dailyForecasts = dailyForecast,
                        atmosphere = atmosphere,
                        alerts = alerts,
                        isLoading = isLoading || isRefreshing,
                        errorMessage = errorMessage,
                        locationPermissionDenied = locationPermissionDenied,
                        formatTemp = { viewModel.formatTemp(it) },
                        onNavigateToAqi = { currentDestination = MusamDestination.AQI },
                        onNavigateToAlerts = { currentDestination = MusamDestination.ALERTS },
                        onRetry = { viewModel.refresh() },
                        onDismissError = { viewModel.clearError() },
                        onDismissLocationDenied = { viewModel.dismissLocationDeniedNotice() },
                        onOpenSearch = { currentDestination = MusamDestination.CITIES },
                        onRequestLocation = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }
                MusamDestination.AQI -> {
                    AqiDetailScreen(
                        city = currentCity,
                        aqiData = currentAqi
                    )
                }
                MusamDestination.RADAR -> {
                    RadarMapScreen(
                        currentCity = currentCity,
                        allCities = allCities,
                        selectedLayer = selectedRadarLayer,
                        onSelectLayer = { viewModel.setRadarLayer(it) },
                        radarFrame = radarFrame,
                        onSetRadarFrame = { viewModel.setRadarFrame(it) },
                        isPlaying = isRadarPlaying,
                        onTogglePlay = { viewModel.toggleRadarPlayback() },
                        onSelectCity = {
                            viewModel.selectCity(it)
                            currentDestination = MusamDestination.WEATHER
                        },
                        formatTemp = { viewModel.formatTemp(it) }
                    )
                }
                MusamDestination.ALERTS -> {
                    AlertsScreen(
                        city = currentCity,
                        alerts = alerts
                    )
                }
                MusamDestination.CITIES -> {
                    CitiesScreen(
                        currentCity = currentCity,
                        allCities = allCities,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        isLocationPermissionDenied = locationPermissionDenied,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onAddCity = {
                            viewModel.addCity(it)
                            currentDestination = MusamDestination.WEATHER
                        },
                        onSelectCity = {
                            viewModel.selectCity(it)
                            currentDestination = MusamDestination.WEATHER
                        },
                        onSelectSearchResult = { result ->
                            viewModel.addSelectedSearchResult(result)
                            currentDestination = MusamDestination.WEATHER
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        formatTemp = { viewModel.formatTemp(it) },
                        onRequestLocation = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}
