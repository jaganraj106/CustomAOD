package com.customaod.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches current weather from Open-Meteo (free, no API key, HTTPS).
 * Results are cached for 30 minutes so the AOD never causes frequent network wake-ups.
 */
public final class WeatherRepository {

    private static final long CACHE_MS = 30 * 60 * 1000L;
    private static final WeatherRepository INSTANCE = new WeatherRepository();

    private final MutableLiveData<WeatherInfo> weather = new MutableLiveData<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private String lastCity = "";
    private long lastFetch = 0;
    private boolean inFlight = false;

    private WeatherRepository() { }

    public static WeatherRepository get() {
        return INSTANCE;
    }

    public LiveData<WeatherInfo> weather() {
        return weather;
    }

    /** Safe to call often; does nothing while the cache is fresh. */
    public synchronized void refresh(String city) {
        if (city == null || city.trim().isEmpty()) return;
        boolean fresh = city.equals(lastCity) && System.currentTimeMillis() - lastFetch < CACHE_MS;
        if (fresh || inFlight) return;

        inFlight = true;
        final String target = city.trim();
        io.execute(() -> {
            try {
                WeatherInfo info = fetch(target);
                if (info != null) {
                    weather.postValue(info);
                    synchronized (WeatherRepository.this) {
                        lastCity = city;
                        lastFetch = System.currentTimeMillis();
                    }
                }
            } catch (Exception ignored) {
                // Offline or API error: keep showing the previous value (or nothing).
            } finally {
                synchronized (WeatherRepository.this) {
                    inFlight = false;
                }
            }
        });
    }

    private WeatherInfo fetch(String city) throws Exception {
        // 1) City name -> coordinates
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?count=1&name="
                + URLEncoder.encode(city, StandardCharsets.UTF_8.name());
        JSONArray results = new JSONObject(httpGet(geoUrl)).optJSONArray("results");
        if (results == null || results.length() == 0) return null;
        JSONObject place = results.getJSONObject(0);

        // 2) Coordinates -> current conditions
        String wxUrl = "https://api.open-meteo.com/v1/forecast?current=temperature_2m,weather_code"
                + "&latitude=" + place.getDouble("latitude")
                + "&longitude=" + place.getDouble("longitude");
        JSONObject current = new JSONObject(httpGet(wxUrl)).getJSONObject("current");

        return new WeatherInfo(place.optString("name", city),
                (int) Math.round(current.getDouble("temperature_2m")),
                current.getInt("weather_code"));
    }

    private String httpGet(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }
}
