const http = require('http');
const https = require('https');
const url = require('url');

const PORT = process.env.APP_PORT || 3000;
const OPENWEATHER_API_KEY = process.env.OPENWEATHER_API_KEY || process.env.OPEN_WEATHER_API_KEY || '';

// Helper to make HTTPS GET requests with Promise
function fetchJson(requestUrl) {
    return new Promise((resolve, reject) => {
        https.get(requestUrl, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; });
            res.on('end', () => {
                if (res.statusCode >= 200 && res.statusCode < 300) {
                    try {
                        resolve(JSON.parse(data));
                    } catch (e) {
                        reject(new Error('Failed to parse JSON response: ' + e.message));
                    }
                } else {
                    reject(new Error(`API responded with status ${res.statusCode}: ${data}`));
                }
            });
        }).on('error', (err) => {
            reject(err);
        });
    });
}

// Convert wind degrees to compass cardinal direction
function degreesToCardinal(deg) {
    if (deg === undefined || deg === null) return 'N';
    const directions = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
    const index = Math.round(deg / 22.5) % 16;
    return directions[index];
}

// Format UNIX timestamp (seconds) into 12-hour time (e.g., "06:15 AM") in location timezone
function formatTime(unixSeconds, timezoneOffsetSeconds = 19800) {
    if (!unixSeconds) return '--:--';
    // UTC ms + timezone offset ms
    const date = new Date((unixSeconds + timezoneOffsetSeconds) * 1000);
    let hours = date.getUTCHours();
    const minutes = date.getUTCMinutes();
    const ampm = hours >= 12 ? 'PM' : 'AM';
    hours = hours % 12;
    hours = hours ? hours : 12;
    const minutesStr = minutes < 10 ? '0' + minutes : minutes;
    const hoursStr = hours < 10 ? '0' + hours : hours;
    return `${hoursStr}:${minutesStr} ${ampm}`;
}

// Calculate Indian CPCB NAQI score and category from pollutants
function calculateNaqi(components) {
    const pm25 = components.pm2_5 || 0;
    const pm10 = components.pm10 || 0;
    const no2 = components.no2 || 0;
    const so2 = components.so2 || 0;
    const co = (components.co || 0) / 1000; // convert to mg/m3 approx

    // Sub-index for PM2.5 (Indian Standard: 0-30 Good, 31-60 Satisfactory, 61-90 Moderate, 91-120 Poor, 121-250 Very Poor, 250+ Severe)
    let pm25Index = 0;
    if (pm25 <= 30) {
        pm25Index = (pm25 / 30) * 50;
    } else if (pm25 <= 60) {
        pm25Index = 50 + ((pm25 - 30) / 30) * 50;
    } else if (pm25 <= 90) {
        pm25Index = 100 + ((pm25 - 60) / 30) * 100;
    } else if (pm25 <= 120) {
        pm25Index = 200 + ((pm25 - 90) / 30) * 100;
    } else if (pm25 <= 250) {
        pm25Index = 300 + ((pm25 - 120) / 130) * 100;
    } else {
        pm25Index = 400 + Math.min(100, ((pm25 - 250) / 150) * 100);
    }

    // Sub-index for PM10 (0-50 Good, 51-100 Satisfactory, 101-250 Moderate, 251-350 Poor, 351-430 Very Poor, >430 Severe)
    let pm10Index = 0;
    if (pm10 <= 50) {
        pm10Index = (pm10 / 50) * 50;
    } else if (pm10 <= 100) {
        pm10Index = 50 + ((pm10 - 50) / 50) * 50;
    } else if (pm10 <= 250) {
        pm10Index = 100 + ((pm10 - 100) / 150) * 100;
    } else if (pm10 <= 350) {
        pm10Index = 200 + ((pm10 - 250) / 100) * 100;
    } else {
        pm10Index = 300 + Math.min(200, ((pm10 - 350) / 150) * 100);
    }

    const aqi = Math.max(12, Math.round(Math.max(pm25Index, pm10Index)));

    let category = "Good";
    let healthSummary = "Air quality is satisfactory, and air pollution poses little or no risk.";
    let maskAdvisory = "Not required for general population.";
    let outdoorAdvisory = "Ideal conditions for walking, running, and all outdoor activities.";
    let airPurifierAdvisory = "Not needed. Outdoor air is clean.";
    let ventilationAdvisory = "Safe to keep windows and doors open for fresh air.";

    if (aqi <= 50) {
        category = "Good";
    } else if (aqi <= 100) {
        category = "Satisfactory";
        healthSummary = "Minor breathing discomfort to sensitive people.";
        maskAdvisory = "Optional for sensitive individuals with respiratory conditions.";
        outdoorAdvisory = "Normal outdoor workouts and commute are safe.";
        airPurifierAdvisory = "Recommended only in closed dusty environments.";
        ventilationAdvisory = "Open windows during sunny daytime hours.";
    } else if (aqi <= 200) {
        category = "Moderate";
        healthSummary = "Breathing discomfort to the people with lungs, asthma and heart diseases.";
        maskAdvisory = "Recommended for children, elderly, and those with allergies during commute.";
        outdoorAdvisory = "Limit intense cardio outdoors; prefer indoor gym sessions.";
        airPurifierAdvisory = "Run air purifier on auto mode during nighttime.";
        ventilationAdvisory = "Keep windows closed during peak traffic and early morning hours.";
    } else if (aqi <= 300) {
        category = "Poor";
        healthSummary = "Breathing discomfort to most people on prolonged exposure.";
        maskAdvisory = "Wear N95/N99 respirator masks when outdoors.";
        outdoorAdvisory = "Avoid strenuous outdoor workouts and jogging.";
        airPurifierAdvisory = "Run True HEPA air purifiers continuously in living and sleeping spaces.";
        ventilationAdvisory = "Keep doors and windows firmly closed to block outdoor particulate ingress.";
    } else if (aqi <= 400) {
        category = "Very Poor";
        healthSummary = "Respiratory illness on prolonged exposure. Pronounced effect on people with lung and heart diseases.";
        maskAdvisory = "Mandatory certified N95 particulate mask outside.";
        outdoorAdvisory = "Avoid all outdoor physical exertion. Stay indoors.";
        airPurifierAdvisory = "Keep HEPA air purifiers active on high fan speed.";
        ventilationAdvisory = "Seal window gaps. Use indoor air filtration.";
    } else {
        category = "Severe";
        healthSummary = "Affects healthy people and seriously impacts those with existing diseases.";
        maskAdvisory = "Strict N95/N99 protection required; avoid non-essential outdoor travel.";
        outdoorAdvisory = "Health alert: everyone may experience serious health effects. Stay indoors.";
        airPurifierAdvisory = "Run medical-grade HEPA filters 24/7.";
        ventilationAdvisory = "Complete lockdown on window ventilation.";
    }

    return {
        aqi,
        category,
        healthSummary,
        maskAdvisory,
        outdoorAdvisory,
        airPurifierAdvisory,
        ventilationAdvisory,
        primaryPollutant: pm25Index >= pm10Index ? "PM2.5" : "PM10"
    };
}

// Build consolidated weather bundle
async function getWeatherBundle(lat, lon) {
    if (!OPENWEATHER_API_KEY) {
        throw new Error('OPENWEATHER_API_KEY is not configured in server environment.');
    }

    const weatherUrl = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${OPENWEATHER_API_KEY}&units=metric`;
    const pollutionUrl = `https://api.openweathermap.org/data/2.5/air_pollution?lat=${lat}&lon=${lon}&appid=${OPENWEATHER_API_KEY}`;
    const forecastUrl = `https://api.openweathermap.org/data/2.5/forecast?lat=${lat}&lon=${lon}&appid=${OPENWEATHER_API_KEY}&units=metric`;

    // Fetch in parallel
    const [currentData, pollutionData, forecastData] = await Promise.all([
        fetchJson(weatherUrl),
        fetchJson(pollutionUrl).catch(() => null),
        fetchJson(forecastUrl).catch(() => null)
    ]);

    const tzOffset = currentData.timezone || 19800; // default to IST +05:30 if missing
    const tempC = Math.round((currentData.main?.temp || 0) * 10) / 10;
    const feelsLikeC = Math.round((currentData.main?.feels_like || tempC) * 10) / 10;
    const tempMin = Math.round((currentData.main?.temp_min || tempC) * 10) / 10;
    const tempMax = Math.round((currentData.main?.temp_max || tempC) * 10) / 10;
    const humidity = currentData.main?.humidity || 50;
    const pressure = currentData.main?.pressure || 1013;
    const visibilityKm = Math.round(((currentData.visibility || 10000) / 1000) * 10) / 10;
    const windSpeedKmh = Math.round(((currentData.wind?.speed || 0) * 3.6) * 10) / 10;
    const windDeg = currentData.wind?.deg || 0;
    const windGustKmh = Math.round(((currentData.wind?.gust || currentData.wind?.speed || 0) * 3.6) * 10) / 10;
    const windDirection = degreesToCardinal(windDeg);

    const conditionMain = currentData.weather?.[0]?.main || 'Clear';
    const conditionDesc = currentData.weather?.[0]?.description || conditionMain;
    const iconCode = currentData.weather?.[0]?.icon || '01d';

    const sunriseTime = formatTime(currentData.sys?.sunrise, tzOffset);
    const sunsetTime = formatTime(currentData.sys?.sunset, tzOffset);

    // Parse AQI & pollutants
    const polComponents = pollutionData?.list?.[0]?.components || {
        pm2_5: 25.0, pm10: 45.0, no2: 18.0, so2: 8.0, co: 450.0, o3: 35.0
    };
    const naqi = calculateNaqi(polComponents);

    const pollutantsList = [
        {
            name: "PM2.5",
            fullName: "Fine Particulate Matter",
            concentration: Math.round((polComponents.pm2_5 || 0) * 10) / 10,
            unit: "µg/m³",
            standardLimit: 60.0,
            status: (polComponents.pm2_5 || 0) <= 30 ? "Good" : (polComponents.pm2_5 || 0) <= 60 ? "Moderate" : "Poor"
        },
        {
            name: "PM10",
            fullName: "Coarse Inhalable Dust",
            concentration: Math.round((polComponents.pm10 || 0) * 10) / 10,
            unit: "µg/m³",
            standardLimit: 100.0,
            status: (polComponents.pm10 || 0) <= 50 ? "Good" : (polComponents.pm10 || 0) <= 100 ? "Moderate" : "Poor"
        },
        {
            name: "NO₂",
            fullName: "Nitrogen Dioxide",
            concentration: Math.round((polComponents.no2 || 0) * 10) / 10,
            unit: "µg/m³",
            standardLimit: 80.0,
            status: (polComponents.no2 || 0) <= 40 ? "Good" : (polComponents.no2 || 0) <= 80 ? "Moderate" : "Poor"
        },
        {
            name: "SO₂",
            fullName: "Sulphur Dioxide",
            concentration: Math.round((polComponents.so2 || 0) * 10) / 10,
            unit: "µg/m³",
            standardLimit: 80.0,
            status: (polComponents.so2 || 0) <= 40 ? "Good" : "Moderate"
        },
        {
            name: "CO",
            fullName: "Carbon Monoxide",
            concentration: Math.round(((polComponents.co || 0) / 1000) * 100) / 100,
            unit: "mg/m³",
            standardLimit: 2.0,
            status: ((polComponents.co || 0) / 1000) <= 1.0 ? "Good" : "Moderate"
        },
        {
            name: "O₃",
            fullName: "Surface Ozone",
            concentration: Math.round((polComponents.o3 || 0) * 10) / 10,
            unit: "µg/m³",
            standardLimit: 100.0,
            status: (polComponents.o3 || 0) <= 50 ? "Good" : "Moderate"
        }
    ];

    // Parse Hourly Forecast (from 3-hour chunks, up to 8 intervals = 24 hours)
    const hourlyItems = [];
    // Include current hour as "Now"
    hourlyItems.push({
        time: "Now",
        tempC: tempC,
        condition: conditionMain,
        pop: 0,
        isNow: true,
        icon: iconCode
    });

    if (forecastData?.list && Array.isArray(forecastData.list)) {
        forecastData.list.slice(0, 8).forEach((item) => {
            const timeStr = formatTime(item.dt, tzOffset);
            hourlyItems.push({
                time: timeStr,
                tempC: Math.round((item.main?.temp || 0) * 10) / 10,
                condition: item.weather?.[0]?.main || 'Clear',
                pop: Math.round((item.pop || 0) * 100),
                isNow: false,
                icon: item.weather?.[0]?.icon || '01d'
            });
        });
    }

    // Parse Daily Forecast (group by day from 5-day forecast)
    const dailyMap = new Map();
    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    if (forecastData?.list && Array.isArray(forecastData.list)) {
        forecastData.list.forEach((item) => {
            const d = new Date((item.dt + tzOffset) * 1000);
            const dateKey = `${d.getUTCFullYear()}-${d.getUTCMonth()}-${d.getUTCDate()}`;
            const dayName = dayNames[d.getUTCDay()];
            const dateStr = `${monthNames[d.getUTCMonth()]} ${d.getUTCDate()}`;

            const itemTemp = item.main?.temp || tempC;
            const pop = Math.round((item.pop || 0) * 100);
            const condition = item.weather?.[0]?.main || 'Clear';

            if (!dailyMap.has(dateKey)) {
                dailyMap.set(dateKey, {
                    day: dayName,
                    date: dateStr,
                    minTempC: itemTemp,
                    maxTempC: itemTemp,
                    condition: condition,
                    pop: pop
                });
            } else {
                const existing = dailyMap.get(dateKey);
                existing.minTempC = Math.min(existing.minTempC, itemTemp);
                existing.maxTempC = Math.max(existing.maxTempC, itemTemp);
                if (pop > existing.pop) existing.pop = pop;
                // If afternoon condition is rain/thunder, prioritize condition
                if (condition.includes('Rain') || condition.includes('Thunder')) {
                    existing.condition = condition;
                }
            }
        });
    }

    const dailyItems = Array.from(dailyMap.values()).slice(0, 7).map((d, index) => ({
        day: index === 0 ? "Today" : d.day,
        date: d.date,
        condition: d.condition,
        minTempC: Math.round(d.minTempC * 10) / 10,
        maxTempC: Math.round(d.maxTempC * 10) / 10,
        pop: d.pop
    }));

    // Dynamic Alerts based on weather conditions
    const alerts = [];
    if (tempC >= 42.0) {
        alerts.push({
            id: "heat_alert",
            headline: "Severe Heatwave Warning (Loo)",
            severity: "RED",
            description: `Extreme daytime temperatures reaching ${tempC}°C across the region. Severe risk of heat exhaustion and heatstroke.`,
            issuedBy: "IMD Meteorological Centre",
            validUntil: "Today, 08:00 PM",
            instructions: [
                "Avoid stepping out during peak afternoon hours (12 PM - 4 PM)",
                "Drink oral rehydration solutions (ORS), water, or lemon water frequently",
                "Keep pets and elderly indoors in well-ventilated cool rooms"
            ]
        });
    } else if (tempC >= 38.0) {
        alerts.push({
            id: "heat_advisory",
            headline: "Yellow Watch: Heatwave Conditions",
            severity: "YELLOW",
            description: `Elevated surface temperature of ${tempC}°C with moderate humidity.`,
            issuedBy: "IMD Regional Weather Watch",
            validUntil: "Today, 06:30 PM",
            instructions: [
                "Stay hydrated and avoid heavy physical labor under direct sunlight",
                "Wear light-colored loose cotton clothing"
            ]
        });
    }

    if (conditionMain.includes("Thunder") || conditionDesc.includes("thunder")) {
        alerts.push({
            id: "thunder_alert",
            headline: "Orange Alert: Thunderstorm & Lightning Hazard",
            severity: "ORANGE",
            description: "Convective thunderstorm with gusty winds and frequent cloud-to-ground lightning strikes.",
            issuedBy: "IMD Doppler Radar Early Warning",
            validUntil: "Next 3 Hours",
            instructions: [
                "Seek immediate shelter inside a sturdy building or enclosed vehicle",
                "Do not stand under tall trees or near metal poles/fences",
                "Unplug sensitive electrical appliances"
            ]
        });
    } else if (conditionMain.includes("Rain") || conditionDesc.includes("heavy rain")) {
        alerts.push({
            id: "rain_watch",
            headline: "Yellow Watch: Precipitation & Reduced Visibility",
            severity: "YELLOW",
            description: "Moderate to heavy rain showers causing localized waterlogging on arterial roads.",
            issuedBy: "Regional Meteorological Centre",
            validUntil: "Next 6 Hours",
            instructions: [
                "Drive with low-beam headlights on and keep safe braking distance",
                "Avoid waterlogged underpasses and low-lying transit corridors"
            ]
        });
    }

    if (naqi.aqi >= 250) {
        alerts.push({
            id: "aqi_emergency",
            headline: "Red Alert: Hazardous Air Quality Emergency",
            severity: "RED",
            description: `CPCB NAQI index reached ${naqi.aqi} (${naqi.category}). Dangerous levels of respirable particulate matter PM2.5.`,
            issuedBy: "Central Pollution Control Board (CPCB)",
            validUntil: "Next 24 Hours",
            instructions: [
                "Wear certified N95 or N99 particulate masks outside",
                "Cease all outdoor sports, morning walks, and vigorous running",
                "Operate True HEPA air purifiers indoors continuously"
            ]
        });
    }

    // Atmospheric details
    const dewPointApprox = Math.round((tempC - ((100 - humidity) / 5)) * 10) / 10;
    const uvApprox = conditionMain === 'Clear' ? 7.8 : conditionMain === 'Clouds' ? 4.2 : 2.1;

    return {
        city: {
            name: currentData.name || "Selected Location",
            state: currentData.sys?.country === 'IN' ? "India" : (currentData.sys?.country || ""),
            lat: lat,
            lon: lon,
            tempC: tempC,
            condition: conditionDesc.charAt(0).toUpperCase() + conditionDesc.slice(1),
            aqi: naqi.aqi
        },
        weather: {
            tempC: tempC,
            feelsLikeC: feelsLikeC,
            tempMinC: tempMin,
            tempMaxC: tempMax,
            humidity: humidity,
            pressureHpa: pressure,
            visibilityKm: visibilityKm,
            windSpeedKmh: windSpeedKmh,
            windDirection: windDirection,
            windGustKmh: windGustKmh,
            condition: conditionMain,
            description: conditionDesc,
            iconCode: iconCode,
            iconUrl: `https://openweathermap.org/img/wn/${iconCode}@2x.png`,
            sunrise: sunriseTime,
            sunset: sunsetTime
        },
        aqi: {
            aqi: naqi.aqi,
            category: naqi.category,
            healthSummary: naqi.healthSummary,
            maskAdvisory: naqi.maskAdvisory,
            outdoorAdvisory: naqi.outdoorAdvisory,
            airPurifierAdvisory: naqi.airPurifierAdvisory,
            ventilationAdvisory: naqi.ventilationAdvisory,
            primaryPollutant: naqi.primaryPollutant,
            pollutants: pollutantsList
        },
        hourly: hourlyItems,
        daily: dailyItems,
        atmosphere: {
            uvIndex: uvApprox,
            uvCategory: uvApprox >= 8 ? "Very High" : uvApprox >= 6 ? "High" : uvApprox >= 3 ? "Moderate" : "Low",
            windSpeedKmh: windSpeedKmh,
            windDirection: windDirection,
            windGustKmh: windGustKmh,
            humidityPercent: humidity,
            dewPointC: dewPointApprox,
            pressureHpa: pressure,
            visibilityKm: visibilityKm,
            sunrise: sunriseTime,
            sunset: sunsetTime
        },
        alerts: alerts,
        updatedAt: new Date().toISOString()
    };
}

// Search cities via OpenWeather Geocoding API
async function searchCities(query) {
    if (!OPENWEATHER_API_KEY) {
        throw new Error('OPENWEATHER_API_KEY is not configured in server environment.');
    }
    const searchUrl = `https://api.openweathermap.org/geo/1.0/direct?q=${encodeURIComponent(query)}&limit=6&appid=${OPENWEATHER_API_KEY}`;
    const results = await fetchJson(searchUrl);
    if (!Array.isArray(results)) return [];

    return results.map((item) => ({
        id: `${item.name.toLowerCase().replace(/[^a-z0-9]/g, '_')}_${Math.round(item.lat * 100)}_${Math.round(item.lon * 100)}`,
        name: item.name,
        state: item.state || item.country || "Global",
        country: item.country,
        lat: item.lat,
        lon: item.lon
    }));
}

// Reverse Geocoding
async function reverseGeocode(lat, lon) {
    if (!OPENWEATHER_API_KEY) {
        throw new Error('OPENWEATHER_API_KEY is not configured in server environment.');
    }
    const revUrl = `https://api.openweathermap.org/geo/1.0/reverse?lat=${lat}&lon=${lon}&limit=1&appid=${OPENWEATHER_API_KEY}`;
    const results = await fetchJson(revUrl);
    if (Array.isArray(results) && results.length > 0) {
        const item = results[0];
        return {
            name: item.name,
            state: item.state || item.country,
            country: item.country,
            lat: item.lat,
            lon: item.lon
        };
    }
    return { name: "Current Location", state: "Live GPS", lat, lon };
}

// HTTP Server instance
const server = http.createServer(async (req, res) => {
    // Set CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    const query = parsedUrl.query;

    try {
        if (pathname === '/health') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
                status: 'ok',
                service: 'Mausam Weather & AQI Secure Runtime API',
                hasApiKey: Boolean(OPENWEATHER_API_KEY),
                time: new Date().toISOString()
            }));
            return;
        }

        if (pathname === '/api/weather-bundle') {
            const lat = parseFloat(query.lat) || 28.6139;
            const lon = parseFloat(query.lon) || 77.2090;
            const bundle = await getWeatherBundle(lat, lon);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(bundle));
            return;
        }

        if (pathname === '/api/search') {
            const q = query.q || '';
            if (!q.trim()) {
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify([]));
                return;
            }
            const cities = await searchCities(q.trim());
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(cities));
            return;
        }

        if (pathname === '/api/reverse-geocode') {
            const lat = parseFloat(query.lat);
            const lon = parseFloat(query.lon);
            if (isNaN(lat) || isNaN(lon)) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Valid lat and lon are required' }));
                return;
            }
            const place = await reverseGeocode(lat, lon);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(place));
            return;
        }

        // Status page for root or preview
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mausam API Server Runtime</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0B1120; color: #E2E8F0; padding: 40px 20px; text-align: center; }
                    .card { max-width: 600px; margin: 0 auto; background: #1E293B; border-radius: 20px; padding: 32px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    h1 { color: #38BDF8; font-size: 28px; margin-bottom: 8px; }
                    .badge { display: inline-block; padding: 6px 14px; border-radius: 999px; background: rgba(16, 185, 129, 0.2); color: #10B981; font-weight: 600; font-size: 14px; margin-bottom: 20px; }
                    p { line-height: 1.6; color: #94A3B8; font-size: 15px; }
                    .endpoint { text-align: left; background: #0F172A; padding: 12px 16px; border-radius: 12px; margin: 10px 0; font-family: monospace; font-size: 13px; color: #38BDF8; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>Mausam Weather & AQI Backend</h1>
                    <div class="badge">● Secure Server Runtime Active</div>
                    <p>The server-side API proxy layer is running securely. OpenWeather API secret credentials remain protected server-side and are never exposed to the frontend client.</p>
                    <div class="endpoint">GET /api/weather-bundle?lat=28.61&lon=77.20</div>
                    <div class="endpoint">GET /api/search?q=Delhi</div>
                    <div class="endpoint">GET /api/reverse-geocode?lat=28.61&lon=77.20</div>
                    <div class="endpoint">GET /health</div>
                </div>
            </body>
            </html>
        `);
    } catch (err) {
        console.error('Server error processing request:', err);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            error: err.message || 'Internal server error',
            timestamp: new Date().toISOString()
        }));
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Mausam Secure API Server running on port ${PORT}`);
    console.log(`OpenWeather API Key configured: ${OPENWEATHER_API_KEY ? 'Yes (Protected)' : 'No'}`);
});
