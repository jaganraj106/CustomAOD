package com.customaod.data;

import androidx.annotation.StringRes;

import com.customaod.R;

/**
 * Selectable clock looks. The AOD screen decides how to lay out and animate
 * the clock from this value (see ui.AodActivity#applyClockStyle).
 */
public enum ClockStyle {
    /** Funtouch-like: thin horizontal digits that gently float up and down. */
    FLOATING(R.string.style_floating),
    /** Hours stacked above minutes, digits cross-fade when they change. */
    STACKED(R.string.style_stacked),
    /** Heavier weight digits, no floating motion (cheapest to render). */
    BOLD(R.string.style_bold);

    @StringRes
    public final int labelRes;

    ClockStyle(@StringRes int labelRes) {
        this.labelRes = labelRes;
    }

    /** Safe lookup used when reading from SharedPreferences. */
    public static ClockStyle fromName(String name) {
        for (ClockStyle s : values()) {
            if (s.name().equals(name)) return s;
        }
        return FLOATING;
    }
}
