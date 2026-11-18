package com.customaod.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable snapshot of every user preference.
 * Produced by {@link SettingsRepository}; consumed by ViewModels and the AOD UI.
 * Being immutable makes it safe to hand across threads and to compare in LiveData.
 */
public final class AodSettings {

    public final boolean aodEnabled;

    // Clock
    @NonNull public final ClockStyle clockStyle;
    public final int clockColor;
    public final int clockSizeSp;

    // Content
    @NonNull public final String customText;
    @Nullable public final String logoPath;
    public final boolean showBattery;
    public final boolean showNotifications;
    public final boolean showMusic;
    public final boolean showWeather;
    @NonNull public final String weatherCity;

    // Effects / behavior
    public final boolean neonWhileCharging;
    public final boolean tapToWake;
    public final boolean fingerprintUnlock;
    /** Window brightness in percent (1..20). Kept extremely low on purpose. */
    public final int brightnessPercent;

    // Schedule (minutes after midnight)
    public final boolean scheduleEnabled;
    public final int scheduleStartMinutes;
    public final int scheduleEndMinutes;

    public AodSettings(boolean aodEnabled, @NonNull ClockStyle clockStyle, int clockColor,
                       int clockSizeSp, @NonNull String customText, @Nullable String logoPath,
                       boolean showBattery, boolean showNotifications, boolean showMusic,
                       boolean showWeather, @NonNull String weatherCity,
                       boolean neonWhileCharging, boolean tapToWake, boolean fingerprintUnlock,
                       int brightnessPercent, boolean scheduleEnabled,
                       int scheduleStartMinutes, int scheduleEndMinutes) {
        this.aodEnabled = aodEnabled;
        this.clockStyle = clockStyle;
        this.clockColor = clockColor;
        this.clockSizeSp = clockSizeSp;
        this.customText = customText;
        this.logoPath = logoPath;
        this.showBattery = showBattery;
        this.showNotifications = showNotifications;
        this.showMusic = showMusic;
        this.showWeather = showWeather;
        this.weatherCity = weatherCity;
        this.neonWhileCharging = neonWhileCharging;
        this.tapToWake = tapToWake;
        this.fingerprintUnlock = fingerprintUnlock;
        this.brightnessPercent = brightnessPercent;
        this.scheduleEnabled = scheduleEnabled;
        this.scheduleStartMinutes = scheduleStartMinutes;
        this.scheduleEndMinutes = scheduleEndMinutes;
    }
}
