package com.customaod.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;

/**
 * Single source of truth for user settings (the "Model" in MVVM).
 *
 * Settings live in SharedPreferences. Every change is re-published as an
 * {@link AodSettings} snapshot through LiveData, so the Settings screen, the AOD
 * screen and the service all react instantly, even though they never talk to each other.
 */
public final class SettingsRepository {

    private static final String PREFS_NAME = "custom_aod_prefs";

    // Preference keys
    public static final String KEY_ENABLED = "aod_enabled";
    public static final String KEY_STYLE = "clock_style";
    public static final String KEY_COLOR = "clock_color";
    public static final String KEY_SIZE = "clock_size_sp";
    public static final String KEY_TEXT = "custom_text";
    public static final String KEY_LOGO = "logo_path";
    public static final String KEY_BATTERY = "show_battery";
    public static final String KEY_NOTIFS = "show_notifications";
    public static final String KEY_MUSIC = "show_music";
    public static final String KEY_WEATHER = "show_weather";
    public static final String KEY_CITY = "weather_city";
    public static final String KEY_NEON = "neon_charging";
    public static final String KEY_TAP = "tap_to_wake";
    public static final String KEY_FINGERPRINT = "fingerprint_unlock";
    public static final String KEY_BRIGHTNESS = "brightness_percent";
    public static final String KEY_SCHEDULE = "schedule_enabled";
    public static final String KEY_SCHED_START = "schedule_start_min";
    public static final String KEY_SCHED_END = "schedule_end_min";

    private static volatile SettingsRepository instance;

    private final SharedPreferences prefs;
    private final Context appContext;
    private final MutableLiveData<AodSettings> live = new MutableLiveData<>();

    // SharedPreferences keeps listeners in a WeakHashMap, so we must hold a strong reference.
    private final SharedPreferences.OnSharedPreferenceChangeListener listener =
            (p, key) -> live.postValue(read());

    private SettingsRepository(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        live.setValue(read());
    }

    public static SettingsRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (SettingsRepository.class) {
                if (instance == null) instance = new SettingsRepository(context);
            }
        }
        return instance;
    }

    public LiveData<AodSettings> observe() {
        return live;
    }

    /** Fresh snapshot, for non-UI callers such as the service. */
    public AodSettings get() {
        return read();
    }

    // ───────────────────────── Writers ─────────────────────────

    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    public void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public void removeLogo() {
        String path = prefs.getString(KEY_LOGO, null);
        if (path != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(path).delete();
        }
        prefs.edit().remove(KEY_LOGO).apply();
    }

    // ───────────────────────── Reader ─────────────────────────

    private AodSettings read() {
        String logo = prefs.getString(KEY_LOGO, null);
        if (logo != null && !new File(logo).exists()) logo = null;

        return new AodSettings(
                prefs.getBoolean(KEY_ENABLED, false),
                ClockStyle.fromName(prefs.getString(KEY_STYLE, ClockStyle.FLOATING.name())),
                prefs.getInt(KEY_COLOR, Color.parseColor("#00E5FF")),
                prefs.getInt(KEY_SIZE, 88),
                prefs.getString(KEY_TEXT, ""),
                logo,
                prefs.getBoolean(KEY_BATTERY, true),
                prefs.getBoolean(KEY_NOTIFS, true),
                prefs.getBoolean(KEY_MUSIC, true),
                prefs.getBoolean(KEY_WEATHER, false),
                prefs.getString(KEY_CITY, ""),
                prefs.getBoolean(KEY_NEON, true),
                prefs.getBoolean(KEY_TAP, true),
                prefs.getBoolean(KEY_FINGERPRINT, false),
                prefs.getInt(KEY_BRIGHTNESS, 2),
                prefs.getBoolean(KEY_SCHEDULE, false),
                prefs.getInt(KEY_SCHED_START, 22 * 60),   // 22:00
                prefs.getInt(KEY_SCHED_END, 7 * 60));     // 07:00
    }

    /** Where the custom logo file is stored (app-private, no storage permission needed). */
    public File logoFile() {
        return new File(appContext.getFilesDir(), "custom_logo.png");
    }
}
