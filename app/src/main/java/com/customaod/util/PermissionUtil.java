package com.customaod.util;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.customaod.service.AodNotificationListener;

/** Permission checks and the Intents that open the matching system screens. */
public final class PermissionUtil {

    private PermissionUtil() { }

    public static boolean hasNotificationAccess(Context c) {
        return NotificationManagerCompat.getEnabledListenerPackages(c).contains(c.getPackageName());
    }

    public static Intent notificationAccessIntent() {
        return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }

    public static boolean hasPostNotifications(Context c) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Full-screen intents became a user-granted special access on Android 14. */
    public static boolean hasFullScreenIntent(Context c) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        return nm.canUseFullScreenIntent();
    }

    public static Intent fullScreenIntentSettings(Context c) {
        if (Build.VERSION.SDK_INT >= 34) {
            return new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + c.getPackageName()));
        }
        return appDetails(c);
    }

    public static boolean isIgnoringBatteryOptimizations(Context c) {
        PowerManager pm = c.getSystemService(PowerManager.class);
        return pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    /** Opens the list screen; avoids the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission. */
    public static Intent batteryOptimizationSettings() {
        return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    }

    public static Intent appDetails(Context c) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + c.getPackageName()));
    }

    /** Component name used to ask the system to rebind the listener if it was killed. */
    public static ComponentName listenerComponent(Context c) {
        return new ComponentName(c, AodNotificationListener.class);
    }
}
