package com.customaod.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

/**
 * Battery status as LiveData.
 *
 * The system receiver is registered only while someone is observing
 * (onActive / onInactive), so there is zero battery monitoring cost when the AOD is hidden.
 * ACTION_BATTERY_CHANGED is a sticky broadcast: registering returns the latest value
 * immediately, and no permission is required to read it.
 */
public final class BatteryLiveData extends LiveData<BatteryState> {

    private final Context context;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            publish(intent);
        }
    };

    public BatteryLiveData(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onActive() {
        Intent sticky = ContextCompat.registerReceiver(
                context, receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        publish(sticky);
    }

    @Override
    protected void onInactive() {
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered.
        }
    }

    private void publish(Intent intent) {
        if (intent == null) return;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (level < 0 || scale <= 0) return;

        int percent = Math.round(level * 100f / scale);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;

        BatteryState old = getValue();
        if (old == null || old.percent != percent || old.charging != charging) {
            setValue(new BatteryState(percent, charging));
        }
    }
}
