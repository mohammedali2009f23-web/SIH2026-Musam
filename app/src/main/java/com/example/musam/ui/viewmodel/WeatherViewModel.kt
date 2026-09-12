package com.example.musam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musam.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    val cities = repository.cities
    val selectedCity = repository.selectedCity
    val isLoading = repository.isLoading
    val errorMessage = repository.errorMessage

    private val _isCelsius = MutableStateFlow(true)
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CityLocation>>(emptyList())
    val searchResults: StateFlow<List<CityLocation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _locationPermissionDenied = MutableStateFlow(false)
    val locationPermissionDenied: StateFlow<Boolean> = _locationPermissionDenied.asStateFlow()

    private val _selectedRadarLayer = MutableStateFlow(RadarLayer.PRECIPITATION)
    val selectedRadarLayer: StateFlow<RadarLayer> = _selectedRadarLayer.asStateFlow()

    private val _radarFrame = MutableStateFlow(2) // 0 to 4 (e.g. -2h, -1h, Now, +30m, +1h)
    val radarFrame: StateFlow<Int> = _radarFrame.asStateFlow()

    private val _isRadarPlaying = MutableStateFlow(false)
    val isRadarPlaying: StateFlow<Boolean> = _isRadarPlaying.asStateFlow()

    private var searchJob: Job? = null

    val currentAqi: StateFlow<AqiData> = selectedCity.map { city ->
        repository.getAqiData(city)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getAqiData(repository.selectedCity.value))

    val hourlyForecast: StateFlow<List<HourlyForecast>> = selectedCity.map { city ->
        repository.getHourlyForecast(city)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getHourlyForecast(repository.selectedCity.value))

    val dailyForecast: StateFlow<List<DailyForecast>> = selectedCity.map { city ->
        repository.getDailyForecast(city)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getDailyForecast(repository.selectedCity.value))

    val atmosphere: StateFlow<AtmosphereDetails> = selectedCity.map { city ->
        repository.getAtmosphereDetails(city)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getAtmosphereDetails(repository.selectedCity.value))

    val alerts: StateFlow<List<WeatherAlert>> = selectedCity.map { city ->
        repository.getAlerts(city)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getAlerts(repository.selectedCity.value))

    init {
        // Initial live weather fetch for starting city
        viewModelScope.launch {
            repository.loadWeatherForCity(repository.selectedCity.value)
        }

        // Timeline playback loop
        viewModelScope.launch {
            while (true) {
                delay(1200)
                if (_isRadarPlaying.value) {
                    _radarFrame.value = (_radarFrame.value + 1) % 5
                }
            }
        }
    }

    fun toggleUnit() {
        _isCelsius.value = !_isCelsius.value
    }

    fun formatTemp(celsius: Double): String {
        return if (_isCelsius.value) {
            "${celsius.toInt()}°C"
        } else {
            val f = (celsius * 9 / 5) + 32
            "${f.toInt()}°F"
        }
    }

    fun selectCity(city: CityLocation) {
        repository.selectCity(city)
        viewModelScope.launch {
            repository.loadWeatherForCity(city)
        }
    }

    fun toggleFavorite(cityId: String) {
        repository.toggleFavorite(cityId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.trim().length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400) // Debounce typing
                _isSearching.value = true
                val results = repository.searchCitiesOnline(query.trim())
                _searchResults.value = results
                _isSearching.value = false
            }
        } else {
            _searchResults.value = emptyList()
            _isSearching.value = false
        }
    }

    fun addCity(name: String) {
        if (name.isNotBlank()) {
            val newCity = repository.addCity(name.trim())
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            viewModelScope.launch {
                repository.loadWeatherForCity(newCity)
            }
        }
    }

    fun addSelectedSearchResult(city: CityLocation) {
        val existing = cities.value.find { it.name.equals(city.name, ignoreCase = true) }
        val target = existing ?: repository.addCity(city.name, city.state, city.lat, city.lon)
        selectCity(target)
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun onGpsLocationReceived(lat: Double, lon: Double) {
        _locationPermissionDenied.value = false
        viewModelScope.launch {
            repository.setLocationFromGps(lat, lon)
        }
    }

    fun onLocationPermissionDenied() {
        _locationPermissionDenied.value = true
    }

    fun dismissLocationDeniedNotice() {
        _locationPermissionDenied.value = false
    }

    fun clearError() {
        repository.clearError()
    }

    fun setRadarLayer(layer: RadarLayer) {
        _selectedRadarLayer.value = layer
    }

    fun toggleRadarPlayback() {
        _isRadarPlaying.value = !_isRadarPlaying.value
    }

    fun setRadarFrame(frame: Int) {
        _radarFrame.value = frame.coerceIn(0, 4)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.loadWeatherForCity(repository.selectedCity.value)
            _isRefreshing.value = false
        }
    }
}
