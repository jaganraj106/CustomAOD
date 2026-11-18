package com.customaod.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.customaod.R;
import com.customaod.data.AodSettings;
import com.customaod.databinding.ActivitySettingsBinding;
import com.customaod.ui.adapter.ClockStyleAdapter;
import com.customaod.ui.adapter.ColorSwatchAdapter;
import com.customaod.util.ScheduleUtil;
import com.customaod.viewmodel.SettingsViewModel;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.util.function.IntConsumer;

/**
 * Customization screen. The UI never writes views and prefs in a loop:
 * user input -> ViewModel -> SharedPreferences -> LiveData -> render(),
 * so there is exactly one direction of data flow.
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding b;
    private SettingsViewModel vm;
    private ClockStyleAdapter styleAdapter;
    private ColorSwatchAdapter colorAdapter;

    /** True while render() writes to views, so listeners ignore programmatic changes. */
    private boolean rendering = false;

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) vm.setLogo(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(SettingsViewModel.class);

        b.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        AodSettings s = vm.settings().getValue();
        styleAdapter = new ClockStyleAdapter(s.clockStyle, s.clockColor, vm::setClockStyle);
        b.rvStyles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        b.rvStyles.setAdapter(styleAdapter);

        colorAdapter = new ColorSwatchAdapter(s.clockColor, (IntConsumer) vm::setClockColor);
        b.rvColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        b.rvColors.setAdapter(colorAdapter);

        wireInputs();
        vm.settings().observe(this, this::render);
        vm.imageError().observe(this, failed -> {
            if (Boolean.TRUE.equals(failed)) {
                Toast.makeText(this, "Could not load that image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void wireInputs() {
        b.sliderSize.addOnChangeListener((sl, value, fromUser) -> {
            if (fromUser) vm.setClockSize((int) value);
        });
        b.sliderBrightness.addOnChangeListener((sl, value, fromUser) -> {
            if (fromUser) vm.setBrightness((int) value);
        });

        b.etCustomText.addTextChangedListener(new SimpleWatcher(t -> vm.setCustomText(t)));
        b.etCity.addTextChangedListener(new SimpleWatcher(t -> vm.setCity(t)));

        b.btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        b.btnClearImage.setOnClickListener(v -> vm.clearLogo());

        bind(b.switchBattery, vm::setShowBattery);
        bind(b.switchNotifications, vm::setShowNotifications);
        bind(b.switchMusic, vm::setShowMusic);
        bind(b.switchWeather, vm::setShowWeather);
        bind(b.switchNeon, vm::setNeon);
        bind(b.switchTapWake, vm::setTapToWake);
        bind(b.switchFingerprint, vm::setFingerprint);
        bind(b.switchSchedule, vm::setScheduleEnabled);

        b.btnStartTime.setOnClickListener(v -> pickTime(true));
        b.btnEndTime.setOnClickListener(v -> pickTime(false));
    }

    private interface BoolSetter { void set(boolean v); }

    private void bind(CompoundButton sw, BoolSetter setter) {
        sw.setOnCheckedChangeListener((v, checked) -> {
            if (!rendering) setter.set(checked);
        });
    }

    /** Draws the current settings into the views. */
    private void render(AodSettings s) {
        rendering = true;

        styleAdapter.setSelected(s.clockStyle, s.clockColor);
        colorAdapter.setSelected(s.clockColor);

        if (b.sliderSize.getValue() != s.clockSizeSp) b.sliderSize.setValue(s.clockSizeSp);
        if (b.sliderBrightness.getValue() != s.brightnessPercent) {
            b.sliderBrightness.setValue(s.brightnessPercent);
        }

        // Do not touch text while the user is typing (would reset the cursor).
        if (!b.etCustomText.hasFocus() && !b.etCustomText.getText().toString().equals(s.customText)) {
            b.etCustomText.setText(s.customText);
        }
        if (!b.etCity.hasFocus() && !b.etCity.getText().toString().equals(s.weatherCity)) {
            b.etCity.setText(s.weatherCity);
        }

        if (s.logoPath != null) {
            b.ivImagePreview.setImageBitmap(android.graphics.BitmapFactory.decodeFile(s.logoPath));
        } else {
            b.ivImagePreview.setImageDrawable(null);
        }
        b.btnClearImage.setEnabled(s.logoPath != null);

        b.switchBattery.setChecked(s.showBattery);
        b.switchNotifications.setChecked(s.showNotifications);
        b.switchMusic.setChecked(s.showMusic);
        b.switchWeather.setChecked(s.showWeather);
        b.switchNeon.setChecked(s.neonWhileCharging);
        b.switchTapWake.setChecked(s.tapToWake);
        b.switchFingerprint.setChecked(s.fingerprintUnlock);
        b.switchSchedule.setChecked(s.scheduleEnabled);

        b.btnStartTime.setText(getString(R.string.schedule_start, ScheduleUtil.format(s.scheduleStartMinutes)));
        b.btnEndTime.setText(getString(R.string.schedule_end, ScheduleUtil.format(s.scheduleEndMinutes)));
        b.btnStartTime.setEnabled(s.scheduleEnabled);
        b.btnEndTime.setEnabled(s.scheduleEnabled);

        rendering = false;
    }

    private void pickTime(boolean start) {
        AodSettings s = vm.settings().getValue();
        int current = start ? s.scheduleStartMinutes : s.scheduleEndMinutes;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current / 60)
                .setMinute(current % 60)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int minutes = picker.getHour() * 60 + picker.getMinute();
            if (start) vm.setScheduleStart(minutes); else vm.setScheduleEnd(minutes);
        });
        picker.show(getSupportFragmentManager(), "time");
    }

    /** TextWatcher that only reports the final text. */
    private static class SimpleWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> sink;

        SimpleWatcher(java.util.function.Consumer<String> sink) { this.sink = sink; }

        @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
        @Override public void onTextChanged(CharSequence s, int a, int c, int d) { }
        @Override public void afterTextChanged(Editable e) { sink.accept(e.toString()); }
    }
}
