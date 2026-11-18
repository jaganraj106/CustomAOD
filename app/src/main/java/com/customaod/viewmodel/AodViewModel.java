package com.customaod.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.customaod.data.AodSettings;
import com.customaod.data.BatteryLiveData;
import com.customaod.data.BatteryState;
import com.customaod.data.MusicInfo;
import com.customaod.data.NotificationItem;
import com.customaod.data.NotificationStore;
import com.customaod.data.SettingsRepository;
import com.customaod.data.WeatherInfo;
import com.customaod.data.WeatherRepository;

import java.util.List;

/**
 * ViewModel of the AOD screen. It only exposes data; it never touches views.
 * AodActivity observes these streams and renders them.
 */
public class AodViewModel extends AndroidViewModel {

    private final SettingsRepository settingsRepo;
    private final BatteryLiveData battery;
    private final LiveData<Boolean> lowBattery;

    public AodViewModel(@NonNull Application app) {
        super(app);
        settingsRepo = SettingsRepository.getInstance(app);
        battery = new BatteryLiveData(app);
        // Drives "pause animations below 15%". distinctUntilChanged avoids redundant callbacks.
        lowBattery = Transformations.distinctUntilChanged(
                Transformations.map(battery, BatteryState::isLow));
    }

    public LiveData<AodSettings> settings() { return settingsRepo.observe(); }
    public LiveData<BatteryState> battery() { return battery; }
    public LiveData<Boolean> lowBattery() { return lowBattery; }
    public LiveData<List<NotificationItem>> notifications() { return NotificationStore.get().notifications(); }
    public LiveData<MusicInfo> music() { return NotificationStore.get().music(); }
    public LiveData<WeatherInfo> weather() { return WeatherRepository.get().weather(); }

    /** Cheap to call every minute: the repository caches for 30 minutes. */
    public void refreshWeather() {
        AodSettings s = settingsRepo.get();
        if (s.showWeather) WeatherRepository.get().refresh(s.weatherCity);
    }
}
