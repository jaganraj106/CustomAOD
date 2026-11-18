package com.customaod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.customaod.data.AodSettings;
import com.customaod.data.SettingsRepository;
import com.customaod.ui.AodActivity;
import com.customaod.util.AodLauncher;
import com.customaod.util.ScheduleUtil;

/**
 * Reacts to screen OFF / user unlock.
 *
 * SCREEN_OFF cannot be declared in the manifest, so this receiver is registered at
 * runtime by {@link com.customaod.service.AodService}; that is why the service must
 * run in the foreground.
 *
 * Note: SCREEN_ON is deliberately NOT handled. The AOD itself lights the panel, so
 * SCREEN_ON would fire right after we show it and would close it again.
 */
public class ScreenStateReceiver extends BroadcastReceiver {

    /** Sent to AodActivity so it finishes itself. */
    public static final String ACTION_FINISH_AOD = "com.customaod.action.FINISH_AOD";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            if (AodActivity.isShowing()) {
                // The AOD is already up, so this is the user pressing the power button
                // to switch the display fully off. Respect that: close the AOD and stay dark.
                sendFinish(context);
                return;
            }
            AodSettings s = SettingsRepository.getInstance(context).get();
            if (s.aodEnabled && ScheduleUtil.isActiveNow(s)) {
                AodLauncher.launch(context);
            }
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            // Device unlocked: the normal UI takes over.
            sendFinish(context);
        }
    }

    private static void sendFinish(Context context) {
        context.sendBroadcast(new Intent(ACTION_FINISH_AOD).setPackage(context.getPackageName()));
    }
}
