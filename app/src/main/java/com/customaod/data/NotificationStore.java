package com.customaod.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;

/**
 * Process-wide holder for data pushed in by {@link com.customaod.service.AodNotificationListener}.
 * The listener is a system-bound service with no UI, so a singleton LiveData is the
 * simplest way to hand its data to the ViewModels.
 */
public final class NotificationStore {

    private static final NotificationStore INSTANCE = new NotificationStore();

    private final MutableLiveData<List<NotificationItem>> notifications =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<MusicInfo> music = new MutableLiveData<>(MusicInfo.NONE);

    private NotificationStore() { }

    public static NotificationStore get() {
        return INSTANCE;
    }

    public LiveData<List<NotificationItem>> notifications() {
        return notifications;
    }

    public LiveData<MusicInfo> music() {
        return music;
    }

    // postValue: the listener calls from binder threads.
    public void setNotifications(List<NotificationItem> items) {
        notifications.postValue(items);
    }

    public void setMusic(MusicInfo info) {
        music.postValue(info);
    }
}
