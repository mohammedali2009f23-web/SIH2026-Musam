# Musam - Weather & Air Quality Index (AQI)

A native Android application built with **Kotlin** and **Jetpack Compose** (Material Design 3), inspired by the India Meteorological Department (IMD) Mausam system and Smart India Hackathon environmental intelligence initiatives.

## Features

- **Live Weather Dashboard**: Real-time temperature, condition summaries, feels-like temperature, dynamic weather hero artwork, and high/low daily forecasts.
- **Air Quality Index (AQI)**: Central Pollution Control Board (CPCB) NAQI scale circular arc gauge with real-time pollutant tracking (PM2.5, PM10, NO₂, SO₂, CO, O₃) and health advisories.
- **Health & Citizen Advisories**: Actionable recommendations for N95 masks, outdoor workouts, HEPA air purifiers, and indoor ventilation.
- **24-Hour Forecast**: Hourly forecast cards featuring precipitation probability, wind velocity, and condition icons.
- **7-Day Outlook**: Weekly forecast with visual temperature range indicators and rain chance.
- **Atmospheric Conditions**: UV index, wind speed/direction, humidity, barometric pressure, visibility, and solar schedule (sunrise/sunset).
- **Interactive Radar & Satellite Maps**: Interactive Canvas map with layers for Doppler Rain, Satellite Cloud Cover, Wind Stream vectors, and AQI Smog Heatmaps, with a 5-step animation timeline player.
- **IMD Early Warnings**: Color-coded alert badges (Red, Orange, Yellow, Green) with official advisories and emergency helplines.
- **City Directory**: Multi-city management with quick search, live temperature/AQI chips, and custom location addition.

## Tech Stack

- **Framework**: Jetpack Compose, Material 3
- **Language**: Kotlin 2.2.10
- **Build System**: Gradle 9.3.1 (Kotlin DSL), Android Gradle Plugin 9.1.1
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow
- **Minimum SDK**: Android API 26 (Oreo)
- **Target SDK**: Android API 36
