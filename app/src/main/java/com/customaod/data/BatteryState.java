package com.customaod.data;

/** Immutable battery snapshot. */
public final class BatteryState {

    /** Below this level all non-essential animation is paused to save power. */
    public static final int LOW_BATTERY_THRESHOLD = 15;

    public final int percent;
    public final boolean charging;

    public BatteryState(int percent, boolean charging) {
        this.percent = percent;
        this.charging = charging;
    }

    /** True when animations (floating clock, neon sweep, fades) must be paused. */
    public boolean isLow() {
        return percent < LOW_BATTERY_THRESHOLD;
    }
}
