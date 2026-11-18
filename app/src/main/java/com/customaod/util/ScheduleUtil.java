package com.customaod.util;

import com.customaod.data.AodSettings;

import java.util.Calendar;
import java.util.Locale;

/** AOD scheduling helpers (start/stop time window, may cross midnight). */
public final class ScheduleUtil {

    private ScheduleUtil() { }

    /** True when the AOD is allowed to show right now. */
    public static boolean isActiveNow(AodSettings s) {
        if (!s.scheduleEnabled) return true;
        Calendar c = Calendar.getInstance();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return isWithin(now, s.scheduleStartMinutes, s.scheduleEndMinutes);
    }

    /** Handles windows like 22:00 -> 07:00 that wrap past midnight. */
    static boolean isWithin(int now, int start, int end) {
        if (start == end) return true;               // treat as "all day"
        if (start < end) return now >= start && now < end;
        return now >= start || now < end;            // crosses midnight
    }

    public static String format(int minutesAfterMidnight) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                minutesAfterMidnight / 60, minutesAfterMidnight % 60);
    }
}
