package com.customaod.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.customaod.R;
import com.customaod.receiver.ScreenStateReceiver;
import com.customaod.ui.MainActivity;

/**
 * Foreground service that keeps the {@link ScreenStateReceiver} alive.
 * It does no work of its own: it only exists so Android does not kill the receiver.
 */
public class AodService extends Service {

    private static final String CHANNEL_ID = "aod_service";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_STOP = "com.customaod.action.STOP_SERVICE";

    private final ScreenStateReceiver receiver = new ScreenStateReceiver();
    private boolean registered = false;

    public static void start(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, AodService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, AodService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForegroundCompat();

        if (!registered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            // System broadcasts: RECEIVER_EXPORTED is required to receive them from the system.
            ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
            registered = true;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (registered) {
            unregisterReceiver(receiver);
            registered = false;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundCompat() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        // IMPORTANCE_MIN: no sound, no status-bar icon clutter.
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, getString(R.string.fgs_channel_name), NotificationManager.IMPORTANCE_MIN));

        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, AodService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_aod)
                .setContentTitle(getString(R.string.fgs_title))
                .setContentText(getString(R.string.fgs_text))
                .setContentIntent(open)
                .addAction(0, getString(R.string.fgs_stop), stop)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }
}
