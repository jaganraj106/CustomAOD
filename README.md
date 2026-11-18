# CustomAOD: Always-On Display for AMOLED (Java, MVVM, Material 3)

A minimal, Funtouch-OS-inspired Always-On Display: floating thin clock, battery bar under it,
neon edge light while charging, pure-black background, per-minute burn-in shifting.

## Project structure

```
CustomAOD/
├── build.gradle / settings.gradle / gradle.properties
└── app/
    ├── build.gradle                       (dependencies, ViewBinding on)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/customaod/
        │   ├── data/        Model: SettingsRepository (SharedPreferences), AodSettings,
        │   │                BatteryLiveData, NotificationStore, WeatherRepository, ...
        │   ├── viewmodel/   AodViewModel, MainViewModel, SettingsViewModel
        │   ├── ui/          MainActivity, SettingsActivity, AodActivity, adapter/, view/NeonEdgeView
        │   ├── service/     AodService (foreground), AodNotificationListener
        │   ├── receiver/    ScreenStateReceiver, BootReceiver
        │   └── util/        BurnInShifter, AodLauncher, PermissionUtil, ScheduleUtil, ImageUtils
        └── res/             layouts, themes (Material 3, AMOLED), drawables, anims, strings
```

MVVM flow: UI input -> ViewModel -> `SettingsRepository` (SharedPreferences) -> LiveData -> UI.
RecyclerViews: clock styles, colour swatches, notification icons.

## Setup (step by step)

1. Install **Android Studio** (Hedgehog or newer) with Android SDK 34 and JDK 17.
2. **File > Open** the `CustomAOD` folder. Let Gradle sync (Studio creates the Gradle wrapper).
3. Connect an AMOLED phone (USB debugging on) and press **Run**.
4. In the app, tick off the **Setup checklist**:
   - **Notification access**: enable CustomAOD in the list (notification count + music info).
   - **Post notifications** (Android 13+).
   - **Full-screen intents** (Android 14+): required to show the AOD while the screen is off.
   - **Unrestricted battery**: pick *Not optimized* so the service is not killed.
5. Turn on **Enable Always-On Display**, then tap **Customize** to style it.
6. Use **Preview AOD** to see it immediately (tap to close).
7. Press the power button. The AOD appears. Tap it to wake (optionally with fingerprint).
   Press the power button while the AOD is showing to switch the display fully off.

## How burn-in protection works

OLED pixels age with use; static bright shapes leave ghost images. CustomAOD defends in layers:

1. **Whole-block shifting every 60 s.** `AodActivity` listens to `ACTION_TIME_TICK`
   (fires once per minute) and calls `BurnInShifter.nextOffset(...)`. Every element
   (date, clock, battery, logo, text, icons, weather, music) sits in ONE container,
   so they all move together and keep their layout.
2. **Random but meaningful moves.** The new offset is random within the free space, and
   must be at least ~35% of that space away from the previous one. This prevents
   "random" picks that land on the same pixels.
3. **Never off-screen.** The offset range is
   `(screen - content) / 2 - margin` per axis, clamped to >= 0. The block is centred by the
   layout and only translated, so with a 24 dp margin (+6 dp for the floating animation) it
   can never cross an edge, even if content is taller than the screen (range becomes 0).
   A layout listener re-clamps instantly when content size changes (e.g. a new notification).
4. **Smooth motion.** The move animates over 1.5 s (fast-out-slow-in), so it looks like
   Funtouch's drift, not a jump.
5. **Sparse, dim content.** Thin fonts, 60% white secondary text, grayscale notification
   icons, black background, extremely low window brightness.
6. **Moving light.** The neon edge is a comet travelling around the outline, never static.

## Power optimization

- Window brightness forced to 1-20% (default 2%).
- Pure `#000000` background: AMOLED pixels are off.
- Below **15% battery**: floating animation, fades, shift animation and neon edge are all
  stopped (the shift becomes an instant jump so burn-in protection still works).
- Animations run in short bursts (3 gentle cycles per minute), not a constant loop.
- Battery receiver is registered only while the AOD is visible (LiveData `onActive/onInactive`).
- Weather is cached for 30 minutes; partial wake lock has a 10 h timeout.

## Bonus features

- **Clock styles**: Floating, Stacked, Bold.
- **Weather**: Open-Meteo (no API key); set a city in Customize.
- **Music info**: from active media sessions via the notification listener.
- **Tap to wake** and **fingerprint unlock** (BiometricPrompt then keyguard dismiss).
- **Scheduling**: start/stop time window, may cross midnight (e.g. 22:00 to 07:00).

## Permissions

| Permission | Why |
|---|---|
| Notification listener (special access) | Notification count, music info |
| Battery status | Sticky `ACTION_BATTERY_CHANGED`, needs no manifest permission |
| WAKE_LOCK | Keep the tick running while AOD is shown |
| FOREGROUND_SERVICE (+ SPECIAL_USE) | Keep the screen on/off receiver alive |
| POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT | Service notice and launching AOD while the screen is off |
| USE_BIOMETRIC, INTERNET, RECEIVE_BOOT_COMPLETED | Fingerprint, weather, restart after reboot |

## Honest limitations

A third-party app cannot use the phone's hardware low-power AOD mode (that belongs to the OEM's
system UI). CustomAOD draws a very dim black activity over the lock screen, so the panel runs at
its normal refresh and the CPU stays partly awake. Expect more battery drain than a native AOD.
Some OEMs (Vivo, Xiaomi, Oppo) need extra "auto-start" / "background activity" toggles in system
settings. The code has not been compiled in this environment (no Android SDK available), so
expect to fix small build issues on first sync.

## Getting an installable APK

**Android Studio:** open the folder, let it sync, then *Build > Build Bundle(s) / APK(s) > Build APK(s)*.
The file is `app/build/outputs/apk/debug/app-debug.apk` (debug builds are auto-signed and installable).

**GitHub (no local setup):** push this folder to a new GitHub repo. The included workflow
`.github/workflows/build-apk.yml` builds it; download `CustomAOD-debug-apk` from the run's Artifacts.

Install: copy the APK to the phone, open it, and allow "Install unknown apps" for your file manager.
