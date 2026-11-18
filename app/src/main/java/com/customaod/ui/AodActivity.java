package com.customaod.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.customaod.R;
import com.customaod.data.AodSettings;
import com.customaod.data.BatteryState;
import com.customaod.data.ClockStyle;
import com.customaod.data.MusicInfo;
import com.customaod.data.NotificationItem;
import com.customaod.databinding.ActivityAodBinding;
import com.customaod.receiver.ScreenStateReceiver;
import com.customaod.ui.adapter.NotifIconAdapter;
import com.customaod.util.AodLauncher;
import com.customaod.util.BurnInShifter;
import com.customaod.util.ScheduleUtil;
import com.customaod.viewmodel.AodViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The Always-On Display surface.
 *
 * Design rules that keep it AMOLED friendly:
 *  - background is #000000 (pixels off), content is thin, dim and sparse;
 *  - brightness is forced to a very low window value;
 *  - the whole content block is moved to a new random spot every minute (burn-in);
 *  - animations are short bursts, and stop completely below 15% battery.
 */
public class AodActivity extends AppCompatActivity {

    private static final String EXTRA_PREVIEW = "preview";
    private static final long SHIFT_ANIM_MS = 1500;
    private static final int SAFE_MARGIN_DP = 24;
    private static final float FLOAT_AMPLITUDE_DP = 6f;

    /** Lets ScreenStateReceiver know that the AOD is currently on screen. */
    private static volatile boolean showing = false;

    public static boolean isShowing() { return showing; }

    public static Intent launchIntent(Context c) {
        return new Intent(c, AodActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }

    /** Preview from the app: the first tap simply closes it. */
    public static Intent previewIntent(Context c) {
        return new Intent(c, AodActivity.class).putExtra(EXTRA_PREVIEW, true);
    }

    private ActivityAodBinding b;
    private AodViewModel vm;
    private NotifIconAdapter iconAdapter;
    private final BurnInShifter shifter = new BurnInShifter();
    private PointF lastOffset = new PointF(0f, 0f);

    private AodSettings settings;
    private boolean lowBattery = false;
    private boolean preview;
    private String lastMinuteKey = "";
    private AnimatorSet floatAnim;
    private PowerManager.WakeLock wakeLock;

    // ───────────────────────── Receivers ─────────────────────────

    /** ACTION_TIME_TICK fires once per minute: the heartbeat for clock + burn-in shift. */
    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onMinuteTick();
        }
    };

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishAndRemoveTask();
        }
    };

    // ───────────────────────── Lifecycle ─────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAodBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(AodViewModel.class);
        preview = getIntent().getBooleanExtra(EXTRA_PREVIEW, false);

        showing = true;
        AodLauncher.cancel(this);

        // Show over the lock screen and light the panel (also declared in the manifest).
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        hideSystemBars();
        acquireWakeLock();

        iconAdapter = new NotifIconAdapter(getPackageManager());
        b.rvNotifIcons.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        b.rvNotifIcons.setAdapter(iconAdapter);
        b.rvNotifIcons.setItemAnimator(null);     // no motion: AMOLED + battery

        // Whenever the block changes size (e.g. a notification appears), re-clamp it
        // so it can never end up outside the screen.
        b.contentGroup.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> {
            if ((r - l) != (or - ol) || (bt - t) != (ob - ot)) shiftContent(false);
        });

        b.aodRoot.setOnClickListener(v -> onTapped());

        observe();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, tickReceiver,
                new IntentFilter(Intent.ACTION_TIME_TICK), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, finishReceiver,
                new IntentFilter(ScreenStateReceiver.ACTION_FINISH_AOD), ContextCompat.RECEIVER_NOT_EXPORTED);
        updateClock();            // catch up if a minute passed while stopped
        vm.refreshWeather();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(tickReceiver);
        unregisterReceiver(finishReceiver);
        stopFloating();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        showing = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    // ───────────────────────── Observing state ─────────────────────────

    private void observe() {
        vm.settings().observe(this, s -> {
            settings = s;
            applySettings(s);
        });
        vm.battery().observe(this, this::renderBattery);
        vm.lowBattery().observe(this, low -> {
            lowBattery = low;
            b.neonEdge.setPaused(low);
            if (low) stopFloating(); else startFloating();
        });
        vm.notifications().observe(this, this::renderNotifications);
        vm.music().observe(this, this::renderMusic);
        vm.weather().observe(this, w -> {
            boolean show = settings != null && settings.showWeather && w != null;
            b.tvWeather.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) b.tvWeather.setText(w.city + "  " + w.displayText());
        });
    }

    /** Applies every user preference that changes how the AOD looks. */
    private void applySettings(AodSettings s) {
        // Brightness: a tiny window value, never 0 (0 can turn some panels fully off).
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = Math.max(0.01f, s.brightnessPercent / 100f);
        getWindow().setAttributes(lp);

        // Clock colour + size.
        for (android.widget.TextView tv : new android.widget.TextView[]{b.tvHour, b.tvColon, b.tvMinute}) {
            tv.setTextColor(s.clockColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, s.clockSizeSp);
        }
        b.neonEdge.setNeonColor(s.clockColor);
        applyClockStyle(s.clockStyle);

        // Custom text and logo.
        boolean hasText = !s.customText.trim().isEmpty();
        b.tvCustomText.setVisibility(hasText ? View.VISIBLE : View.GONE);
        b.tvCustomText.setText(s.customText);

        if (s.logoPath != null) {
            b.ivLogo.setImageBitmap(android.graphics.BitmapFactory.decodeFile(s.logoPath));
            b.ivLogo.setVisibility(View.VISIBLE);
        } else {
            b.ivLogo.setVisibility(View.GONE);
        }

        b.batteryBlock.setVisibility(s.showBattery ? View.VISIBLE : View.GONE);
        b.rvNotifIcons.setVisibility(s.showNotifications ? View.VISIBLE : View.GONE);
        if (!s.showNotifications) b.tvNotifCount.setVisibility(View.GONE);
        if (!s.showWeather) b.tvWeather.setVisibility(View.GONE);

        updateClock();
        vm.refreshWeather();
        if (vm.battery().getValue() != null) renderBattery(vm.battery().getValue());
        startFloating();
    }

    // ───────────────────────── Clock ─────────────────────────

    /** FLOATING/BOLD stay horizontal; STACKED puts the minutes under the hours. */
    private void applyClockStyle(ClockStyle style) {
        boolean stacked = style == ClockStyle.STACKED;
        b.clockContainer.setOrientation(stacked ? android.widget.LinearLayout.VERTICAL
                : android.widget.LinearLayout.HORIZONTAL);
        b.tvColon.setVisibility(stacked ? View.GONE : View.VISIBLE);

        Typeface face = style == ClockStyle.BOLD
                ? Typeface.create("sans-serif-medium", Typeface.NORMAL)
                : Typeface.create("sans-serif-thin", Typeface.NORMAL);
        b.tvHour.setTypeface(face);
        b.tvColon.setTypeface(face);
        b.tvMinute.setTypeface(face);
    }

    private void updateClock() {
        Date now = new Date();
        boolean is24 = DateFormat.is24HourFormat(this);
        Locale loc = Locale.getDefault();
        String hour = new SimpleDateFormat(is24 ? "HH" : "h", loc).format(now);
        String minute = new SimpleDateFormat("mm", loc).format(now);
        String key = hour + ":" + minute;

        boolean changed = !key.equals(lastMinuteKey);
        lastMinuteKey = key;

        b.tvHour.setText(hour);
        b.tvMinute.setText(minute);
        b.tvDate.setText(new SimpleDateFormat(
                DateFormat.getBestDateTimePattern(loc, "EEEdMMM"), loc).format(now));

        // Soft fade of the digits when the time changes (skipped on low battery).
        if (changed && !lowBattery && b.clockContainer.getWidth() > 0) {
            b.clockContainer.setAlpha(0.2f);
            b.clockContainer.animate().alpha(1f).setDuration(700).start();
        }
    }

    // ───────────────────────── Minute tick ─────────────────────────

    private void onMinuteTick() {
        // Schedule window ended while the AOD was showing: hand the screen back.
        if (settings != null && !preview && !ScheduleUtil.isActiveNow(settings)) {
            finishAndRemoveTask();
            return;
        }
        updateClock();
        shiftContent(true);          // burn-in protection (see README)
        vm.refreshWeather();
        startFloating();             // one short burst of gentle floating per minute
    }

    // ───────────────────────── Burn-in protection ─────────────────────────

    /**
     * Moves the whole content block to a new random position.
     * The block is centred by the layout; we only add a translation, and the helper
     * guarantees the block keeps SAFE_MARGIN_DP away from every screen edge.
     */
    private void shiftContent(boolean animate) {
        int w = b.aodRoot.getWidth();
        int h = b.aodRoot.getHeight();
        int cw = b.contentGroup.getWidth();
        int ch = b.contentGroup.getHeight();
        if (w == 0 || h == 0 || cw == 0) {
            // Not laid out yet: try again right after the first layout pass.
            b.aodRoot.post(() -> shiftContent(false));
            return;
        }

        // The floating animation adds up to FLOAT_AMPLITUDE_DP of extra travel,
        // so reserve it inside the margin.
        int margin = dp(SAFE_MARGIN_DP) + dp(FLOAT_AMPLITUDE_DP);
        PointF next = shifter.nextOffset(w, h, cw, ch, margin, lastOffset);
        lastOffset = next;

        if (animate && !lowBattery) {
            b.contentGroup.animate()
                    .translationX(next.x).translationY(next.y)
                    .setDuration(SHIFT_ANIM_MS)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        } else {
            b.contentGroup.animate().cancel();
            b.contentGroup.setTranslationX(next.x);
            b.contentGroup.setTranslationY(next.y);
        }
    }

    // ───────────────────────── Floating clock animation ─────────────────────────

    /**
     * Funtouch-style gentle "breathing" of the clock: three slow up/down cycles,
     * then stillness until the next minute. Short bursts instead of a permanent
     * 60fps loop keep the display driver (and battery) idle most of the time.
     */
    private void startFloating() {
        stopFloating();
        if (lowBattery || settings == null || settings.clockStyle != ClockStyle.FLOATING) return;

        float amp = dp(FLOAT_AMPLITUDE_DP);
        ObjectAnimator up = ObjectAnimator.ofFloat(b.clockContainer, View.TRANSLATION_Y, 0f, -amp);
        up.setDuration(2500);
        up.setRepeatCount(5);                         // 6 half-cycles = 3 full cycles
        up.setRepeatMode(ObjectAnimator.REVERSE);
        up.setInterpolator(new AccelerateDecelerateInterpolator());

        floatAnim = new AnimatorSet();
        floatAnim.play(up);
        floatAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                b.clockContainer.setTranslationY(0f);
            }
        });
        floatAnim.start();
    }

    private void stopFloating() {
        if (floatAnim != null) {
            floatAnim.cancel();
            floatAnim = null;
        }
        b.clockContainer.setTranslationY(0f);
    }

    // ───────────────────────── Battery ─────────────────────────

    private void renderBattery(BatteryState s) {
        b.tvBattery.setText(s.percent + "%");
        b.ivBolt.setVisibility(s.charging ? View.VISIBLE : View.GONE);

        // Level bar: scale a full-width view from its left edge.
        b.batteryFill.setPivotX(0f);
        b.batteryFill.setScaleX(Math.max(0.02f, s.percent / 100f));
        int fill = s.charging ? ContextCompat.getColor(this, R.color.neon_lime)
                : s.isLow() ? ContextCompat.getColor(this, R.color.neon_red)
                : ContextCompat.getColor(this, R.color.aod_dim_text);
        b.batteryFill.setBackgroundColor(fill);

        // Neon edge only while charging and when enabled in settings.
        b.neonEdge.setActive(s.charging && settings != null && settings.neonWhileCharging);
    }

    // ───────────────────────── Notifications / music ─────────────────────────

    private void renderNotifications(List<NotificationItem> items) {
        if (settings == null || !settings.showNotifications || items == null || items.isEmpty()) {
            iconAdapter.submitList(null);
            b.tvNotifCount.setVisibility(View.GONE);
            b.rvNotifIcons.setVisibility(View.GONE);
            return;
        }
        int total = 0;
        for (NotificationItem i : items) total += i.count;

        b.rvNotifIcons.setVisibility(View.VISIBLE);
        iconAdapter.submitList(items.size() > 5 ? items.subList(0, 5) : items);
        b.tvNotifCount.setVisibility(View.VISIBLE);
        b.tvNotifCount.setText(total == 1 ? getString(R.string.notif_count_one)
                : getString(R.string.notif_count_many, total));
    }

    private void renderMusic(MusicInfo m) {
        boolean show = settings != null && settings.showMusic && m != null && !m.isEmpty();
        b.musicRow.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) b.tvMusic.setText(m.displayText());
    }

    // ───────────────────────── Tap to wake / fingerprint ─────────────────────────

    private void onTapped() {
        if (preview) {
            finish();
            return;
        }
        if (settings == null || !settings.tapToWake) return;

        restoreSystemBrightness();
        if (settings.fingerprintUnlock && canUseBiometrics()) {
            showBiometricPrompt();
        } else {
            finishAndRemoveTask();      // lock screen is revealed underneath
        }
    }

    private boolean canUseBiometrics() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                        // Ask the system to dismiss the lock screen; either way we close.
                        getSystemService(KeyguardManager.class).requestDismissKeyguard(
                                AodActivity.this, new KeyguardManager.KeyguardDismissCallback() {
                                    @Override public void onDismissSucceeded() { finishAndRemoveTask(); }
                                    @Override public void onDismissCancelled() { finishAndRemoveTask(); }
                                    @Override public void onDismissError() { finishAndRemoveTask(); }
                                });
                    }

                    @Override
                    public void onAuthenticationError(int code, CharSequence msg) {
                        finishAndRemoveTask();   // cancelled: fall back to the plain lock screen
                    }
                });

        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build());
    }

    private void restoreSystemBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
    }

    // ───────────────────────── Helpers ─────────────────────────

    /**
     * Partial wake lock: lets the CPU deliver the per-minute tick while the AOD is up.
     * It has a timeout so a crash can never drain the battery forever.
     */
    private void acquireWakeLock() {
        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CustomAOD:aod");
        wakeLock.acquire(10 * 60 * 60 * 1000L);     // 10 hours max
    }

    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), b.getRoot());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }
}
