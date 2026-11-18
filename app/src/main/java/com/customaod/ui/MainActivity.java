package com.customaod.ui;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.customaod.R;
import com.customaod.databinding.ActivityMainBinding;
import com.customaod.util.PermissionUtil;
import com.customaod.viewmodel.MainViewModel;

/**
 * Home screen: master switch + setup checklist.
 * Each checklist card shows its state and opens the right system screen when tapped.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel vm;
    private boolean updatingSwitch = false;

    private final ActivityResultLauncher<String> postNotifLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> refreshStatus());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        vm = new ViewModelProvider(this).get(MainViewModel.class);

        vm.enabled().observe(this, on -> {
            updatingSwitch = true;
            binding.switchAod.setChecked(on);
            updatingSwitch = false;
        });
        binding.switchAod.setOnCheckedChangeListener((v, checked) -> {
            if (!updatingSwitch) vm.setEnabled(checked);
        });

        binding.cardNotifAccess.setOnClickListener(v ->
                startActivity(PermissionUtil.notificationAccessIntent()));
        binding.cardPostNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                postNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });
        binding.cardFullScreen.setOnClickListener(v ->
                startActivity(PermissionUtil.fullScreenIntentSettings(this)));
        binding.cardBatteryOpt.setOnClickListener(v ->
                startActivity(PermissionUtil.batteryOptimizationSettings()));

        binding.btnCustomize.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.btnPreview.setOnClickListener(v ->
                startActivity(AodActivity.previewIntent(this)));
    }

    /** The user returns from system screens here, so re-check every time. */
    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        setStatus(binding.tvStatusNotifAccess, PermissionUtil.hasNotificationAccess(this));
        setStatus(binding.tvStatusPostNotif, PermissionUtil.hasPostNotifications(this));
        setStatus(binding.tvStatusFullScreen, PermissionUtil.hasFullScreenIntent(this));
        setStatus(binding.tvStatusBatteryOpt, PermissionUtil.isIgnoringBatteryOptimizations(this));
    }

    private void setStatus(android.widget.TextView tv, boolean granted) {
        tv.setText(granted ? R.string.status_granted : R.string.status_missing);
        tv.setAlpha(granted ? 0.6f : 1f);
    }
}
