package com.customaod.data;

/** Current weather for the city chosen in settings. */
public final class WeatherInfo {

    public final String city;
    public final int temperatureC;
    public final int wmoCode;

    public WeatherInfo(String city, int temperatureC, int wmoCode) {
        this.city = city;
        this.temperatureC = temperatureC;
        this.wmoCode = wmoCode;
    }

    /** Short text for the AOD, e.g. "28° Partly cloudy". */
    public String displayText() {
        return temperatureC + "° " + describe(wmoCode);
    }

    /** Maps WMO weather interpretation codes (used by Open-Meteo) to plain words. */
    private static String describe(int code) {
        if (code == 0) return "Clear";
        if (code <= 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 95) return "Thunderstorm";
        return "";
    }
}
