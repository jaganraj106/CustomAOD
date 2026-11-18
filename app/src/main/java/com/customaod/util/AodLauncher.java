package com.customaod.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.customaod.R;
import com.customaod.ui.AodActivity;

/**
 * Starts the AOD screen while the device is asleep.
 *
 * Since Android 10 an app in the background may not call startActivity().
 * The supported way to put an Activity on screen while the display is off is a
 * high-priority notification with a full-screen intent (the same mechanism alarm
 * and call apps use). That is why the app asks for the full-screen-intent permission.
 */
public final class AodLauncher {

    private static final String CHANNEL_ID = "aod_launch";
    private static final int NOTIFICATION_ID = 1002;

    private AodLauncher() { }

    public static void launch(Context context) {
        if (Build.VERSION.SDK_INT < 29) {
            context.startActivity(AodActivity.launchIntent(context));
            return;
        }

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, context.getString(R.string.fgs_channel_name) + " launcher",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(null, null);            // silent
        channel.enableVibration(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        nm.createNotificationChannel(channel);

        PendingIntent pi = PendingIntent.getActivity(context, 0,
                AodActivity.launchIntent(context), PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_aod)
                .setContentTitle(context.getString(R.string.app_name))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .setTimeoutAfter(5000)
                .build();
        nm.notify(NOTIFICATION_ID, n);
    }

    /** Removes the launcher notification once the AOD is on screen. */
    public static void cancel(Context context) {
        context.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
    }
}
