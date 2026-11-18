package com.customaod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.customaod.data.SettingsRepository;
import com.customaod.service.AodService;

/** Restarts the AOD service after reboot if the user had it enabled. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                && SettingsRepository.getInstance(context).get().aodEnabled) {
            AodService.start(context);
        }
    }
}
