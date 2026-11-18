package com.customaod;

import android.app.Application;
import android.content.Intent;

import com.customaod.ui.CrashActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Application class. Installs a last-resort crash handler that opens
 * {@link CrashActivity} (in a separate process) with the stack trace, so a crash
 * on a phone without adb/logcat can still be read, copied and reported.
 */
public class CustomAodApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                StringWriter sw = new StringWriter();
                throwable.printStackTrace(new PrintWriter(sw));
                Intent i = new Intent(this, CrashActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(CrashActivity.EXTRA_TRACE, sw.toString());
                startActivity(i);
            } catch (Throwable ignored) {
                // Never let the reporter itself hide the real crash.
            }
            if (previous != null) previous.uncaughtException(thread, throwable);
        });
    }
}
