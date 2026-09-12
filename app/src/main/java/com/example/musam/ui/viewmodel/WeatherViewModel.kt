package com.example.musam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musam.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    val cities = repository.cities
    val selectedCity = repository.selectedCity

    private val _isCelsius = MutableStateFlow(true)
    val isCelsius: StateFlow<Boolean> = _isCelsius.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRadarLayer = MutableStateFlow(RadarLayer.PRECIPITATION)
    val selectedRadarLayer: StateFlow<RadarLayer> = _selectedRadarLayer.asStateFlow()

    private val _radarFrame = MutableStateFlow(2) // 0 to 4 (e.g. -2h, -1h, Now, +30m, +1h)
    val radarFrame: StateFlow<Int> = _radarFrame.asStateFlow()

    private val _isRadarPlaying = MutableStateFlow(false)
    val isRadarPlaying: StateFlow<Boolean> = _isRadarPlaying.asStateFlow()

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
    }

    fun toggleFavorite(cityId: String) {
        repository.toggleFavorite(cityId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addCity(name: String) {
        if (name.isNotBlank()) {
            repository.addCity(name.trim())
            _searchQuery.value = ""
        }
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
            delay(700)
            // Re-trigger by copying current city with minor random perturbation
            val current = repository.selectedCity.value
            val refreshed = current.copy(
                tempC = ((current.tempC * 10).toInt() + (-3..3).random()) / 10.0,
                aqi = (current.aqi + (-5..5).random()).coerceAtLeast(15)
            )
            repository.selectCity(refreshed)
            _isRefreshing.value = false
        }
    }
}
