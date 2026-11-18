package com.customaod.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.customaod.data.AodSettings;
import com.customaod.data.ClockStyle;
import com.customaod.data.SettingsRepository;
import com.customaod.util.ImageUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** ViewModel of SettingsActivity. All writes go through the repository. */
public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository repo;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Boolean> imageError = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application app) {
        super(app);
        repo = SettingsRepository.getInstance(app);
    }

    public LiveData<AodSettings> settings() { return repo.observe(); }
    public LiveData<Boolean> imageError() { return imageError; }

    public void setClockStyle(ClockStyle s) { repo.putString(SettingsRepository.KEY_STYLE, s.name()); }
    public void setClockColor(int color) { repo.putInt(SettingsRepository.KEY_COLOR, color); }
    public void setClockSize(int sp) { repo.putInt(SettingsRepository.KEY_SIZE, sp); }
    public void setCustomText(String t) { repo.putString(SettingsRepository.KEY_TEXT, t); }
    public void setCity(String c) { repo.putString(SettingsRepository.KEY_CITY, c.trim()); }
    public void setShowBattery(boolean b) { repo.putBoolean(SettingsRepository.KEY_BATTERY, b); }
    public void setShowNotifications(boolean b) { repo.putBoolean(SettingsRepository.KEY_NOTIFS, b); }
    public void setShowMusic(boolean b) { repo.putBoolean(SettingsRepository.KEY_MUSIC, b); }
    public void setShowWeather(boolean b) { repo.putBoolean(SettingsRepository.KEY_WEATHER, b); }
    public void setNeon(boolean b) { repo.putBoolean(SettingsRepository.KEY_NEON, b); }
    public void setTapToWake(boolean b) { repo.putBoolean(SettingsRepository.KEY_TAP, b); }
    public void setFingerprint(boolean b) { repo.putBoolean(SettingsRepository.KEY_FINGERPRINT, b); }
    public void setBrightness(int percent) { repo.putInt(SettingsRepository.KEY_BRIGHTNESS, percent); }
    public void setScheduleEnabled(boolean b) { repo.putBoolean(SettingsRepository.KEY_SCHEDULE, b); }
    public void setScheduleStart(int min) { repo.putInt(SettingsRepository.KEY_SCHED_START, min); }
    public void setScheduleEnd(int min) { repo.putInt(SettingsRepository.KEY_SCHED_END, min); }

    /** Copies + downsizes the picked image off the main thread, then stores its path. */
    public void setLogo(Uri uri) {
        io.execute(() -> {
            File target = repo.logoFile();
            boolean ok = ImageUtils.saveScaledLogo(getApplication(), uri, target);
            if (ok) {
                // New path string each time so the pref listener fires even for the same file.
                repo.putString(SettingsRepository.KEY_LOGO, target.getAbsolutePath());
                repo.putInt("logo_version", (int) (System.currentTimeMillis() / 1000));
            }
            imageError.postValue(!ok);
        });
    }

    public void clearLogo() { repo.removeLogo(); }

    @Override
    protected void onCleared() {
        io.shutdown();
    }
}
