package com.customaod.data;

import androidx.annotation.NonNull;

/** Minimal view of an active notification: just enough to draw an app icon. */
public final class NotificationItem {
    @NonNull public final String packageName;
    public final int count;

    public NotificationItem(@NonNull String packageName, int count) {
        this.packageName = packageName;
        this.count = count;
    }
}
