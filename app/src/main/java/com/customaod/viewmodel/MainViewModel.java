package com.customaod.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.customaod.data.AodSettings;
import com.customaod.data.SettingsRepository;
import com.customaod.service.AodService;

/** ViewModel of MainActivity: master on/off switch. */
public class MainViewModel extends AndroidViewModel {

    private final SettingsRepository repo;
    private final LiveData<Boolean> enabled;

    public MainViewModel(@NonNull Application app) {
        super(app);
        repo = SettingsRepository.getInstance(app);
        enabled = Transformations.distinctUntilChanged(
                Transformations.map(repo.observe(), (AodSettings s) -> s.aodEnabled));
    }

    public LiveData<Boolean> enabled() { return enabled; }

    /** Persists the choice and starts/stops the foreground service accordingly. */
    public void setEnabled(boolean on) {
        repo.putBoolean(SettingsRepository.KEY_ENABLED, on);
        if (on) {
            AodService.start(getApplication());
        } else {
            AodService.stop(getApplication());
        }
    }
}
