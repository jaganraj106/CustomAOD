package com.customaod.data;

import androidx.annotation.Nullable;

/** Now-playing info taken from the active MediaSession. */
public final class MusicInfo {

    public static final MusicInfo NONE = new MusicInfo(null, null, false);

    @Nullable public final String title;
    @Nullable public final String artist;
    public final boolean playing;

    public MusicInfo(@Nullable String title, @Nullable String artist, boolean playing) {
        this.title = title;
        this.artist = artist;
        this.playing = playing;
    }

    public boolean isEmpty() {
        return !playing || title == null || title.isEmpty();
    }

    /** "Artist · Title" or just the title. */
    public String displayText() {
        if (title == null) return "";
        return (artist == null || artist.isEmpty()) ? title : artist + " · " + title;
    }
}
