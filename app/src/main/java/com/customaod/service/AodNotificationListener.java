package com.customaod.service;

import android.app.Notification;
import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.customaod.data.MusicInfo;
import com.customaod.data.NotificationItem;
import com.customaod.data.NotificationStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feeds two things to the AOD:
 *  1. the list of active, user-visible notifications (count + app icons);
 *  2. now-playing music info, read from active MediaSessions
 *     (this requires notification access, which the user grants for this service).
 */
public class AodNotificationListener extends NotificationListenerService {

    private MediaSessionManager sessionManager;
    private final List<MediaController> watched = new ArrayList<>();

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            controllers -> bindControllers(controllers);

    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            publishMusic();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            publishMusic();
        }
    };

    @Override
    public void onListenerConnected() {
        refreshNotifications();
        try {
            sessionManager = getSystemService(MediaSessionManager.class);
            ComponentName me = new ComponentName(this, AodNotificationListener.class);
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, me);
            bindControllers(sessionManager.getActiveSessions(me));
        } catch (SecurityException ignored) {
            // Access was revoked between callbacks.
        }
    }

    @Override
    public void onListenerDisconnected() {
        unbindControllers();
        if (sessionManager != null) sessionManager.removeOnActiveSessionsChangedListener(sessionsListener);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        refreshNotifications();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        refreshNotifications();
    }

    /** Groups notifications per app so the AOD shows one icon per app. */
    private void refreshNotifications() {
        StatusBarNotification[] active;
        try {
            active = getActiveNotifications();
        } catch (SecurityException e) {
            return;
        }
        Map<String, Integer> perApp = new LinkedHashMap<>();
        if (active != null) {
            for (StatusBarNotification sbn : active) {
                if (!isUserVisible(sbn)) continue;
                perApp.merge(sbn.getPackageName(), 1, Integer::sum);
            }
        }
        List<NotificationItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> e : perApp.entrySet()) {
            items.add(new NotificationItem(e.getKey(), e.getValue()));
        }
        NotificationStore.get().setNotifications(items);
    }

    /** Skips our own service notice, group summaries, and ongoing system noise. */
    private boolean isUserVisible(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals(getPackageName())) return false;
        Notification n = sbn.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return false;
        if (sbn.isOngoing() && n.category != null
                && !Notification.CATEGORY_MESSAGE.equals(n.category)) return false;
        return true;
    }

    // ───────────────────────── Music ─────────────────────────

    private void bindControllers(List<MediaController> controllers) {
        unbindControllers();
        if (controllers != null) {
            for (MediaController c : controllers) {
                c.registerCallback(controllerCallback);
                watched.add(c);
            }
        }
        publishMusic();
    }

    private void unbindControllers() {
        for (MediaController c : watched) c.unregisterCallback(controllerCallback);
        watched.clear();
    }

    /** Publishes the first controller that is actively playing. */
    private void publishMusic() {
        for (MediaController c : watched) {
            PlaybackState ps = c.getPlaybackState();
            MediaMetadata md = c.getMetadata();
            if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING && md != null) {
                NotificationStore.get().setMusic(new MusicInfo(
                        md.getString(MediaMetadata.METADATA_KEY_TITLE),
                        md.getString(MediaMetadata.METADATA_KEY_ARTIST),
                        true));
                return;
            }
        }
        NotificationStore.get().setMusic(MusicInfo.NONE);
    }
}
