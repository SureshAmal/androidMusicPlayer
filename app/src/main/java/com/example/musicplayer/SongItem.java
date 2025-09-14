package com.example.musicplayer;

import android.net.Uri;

public class SongItem {

    private String title;
    private String artist;
    private String album;
    private long duration;
    private Uri uri;
    private String path;

    public SongItem() {
        // Default constructor
    }

    public SongItem(
        Uri uri,
        String title,
        String artist,
        String album,
        long duration,
        String path
    ) {
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFormattedDuration() {
        if (duration <= 0) return "0:00";

        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public String toString() {
        return (
            "SongItem{" +
            "title='" +
            title +
            '\'' +
            ", artist='" +
            artist +
            '\'' +
            ", album='" +
            album +
            '\'' +
            ", duration=" +
            duration +
            ", uri=" +
            uri +
            ", path='" +
            path +
            '\'' +
            '}'
        );
    }
}
