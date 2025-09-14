package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;

public class MusicService extends Service {

    public static final String ACTION_INIT = "com.example.musicplayer.action.INIT";
    public static final String ACTION_PLAY = "com.example.musicplayer.action.PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayer.action.PAUSE";
    public static final String ACTION_TOGGLE = "com.example.musicplayer.action.TOGGLE";
    public static final String ACTION_NEXT = "com.example.musicplayer.action.NEXT";
    public static final String ACTION_PREV = "com.example.musicplayer.action.PREV";
    public static final String ACTION_SEEK = "com.example.musicplayer.action.SEEK";
    public static final String ACTION_STOP = "com.example.musicplayer.action.STOP";

    private static final int NOTIF_ID = 1001;
    private static final String CHANNEL_ID = "music_playback";

    private ArrayList<Uri> playlist = new ArrayList<>();
    private int index = 0;
    private MediaPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_INIT.equals(action)) {
                ArrayList<Uri> list = intent.getParcelableArrayListExtra("playlist");
                int idx = intent.getIntExtra("index", 0);
                if (list != null && !list.isEmpty()) {
                    playlist = list;
                    playIndex(idx);
                }
            } else if (ACTION_PLAY.equals(action)) {
                startPlay();
            } else if (ACTION_PAUSE.equals(action)) {
                pausePlay();
            } else if (ACTION_TOGGLE.equals(action)) {
                if (player != null && player.isPlaying()) pausePlay(); else startPlay();
            } else if (ACTION_NEXT.equals(action)) {
                playIndex(index + 1);
            } else if (ACTION_PREV.equals(action)) {
                playIndex(index - 1);
            } else if (ACTION_SEEK.equals(action)) {
                int position = intent.getIntExtra("position", 0);
                seekTo(position);
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void playIndex(int idx) {
        if (playlist == null || playlist.isEmpty()) return;
        if (idx < 0) idx = playlist.size() - 1;
        if (idx >= playlist.size()) idx = 0;
        index = idx;

        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }

        Uri u = playlist.get(index);
        player = MediaPlayer.create(this, u);
        if (player == null) return;
        player.setOnCompletionListener(mp -> playIndex(index + 1));

        startForeground(NOTIF_ID, buildNotification(u));
        startPlay();
    }

    private void startPlay() {
        if (player == null) return;
        try { player.start(); } catch (Exception ignored) {}
        Notification n = buildNotification(playlist.isEmpty() ? null : playlist.get(index));
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, n);
    }

    private void pausePlay() {
        if (player == null) return;
        try { player.pause(); } catch (Exception ignored) {}
        Notification n = buildNotification(playlist.isEmpty() ? null : playlist.get(index));
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, n);
    }

    private void seekTo(int position) {
        if (player == null) return;
        try { player.seekTo(position); } catch (Exception ignored) {}
    }

    private Notification buildNotification(@Nullable Uri uri) {
        String title = "Playing";
        String artist = "";
        if (uri != null) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(this, uri);
                String t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (t != null) title = t;
                if (a != null) artist = a;
            } catch (Exception ignored) {} finally { try { mmr.release(); } catch (Exception ignored) {} }
        }

        Intent open = new Intent(this, PlayerActivity.class);
        open.putParcelableArrayListExtra("playlist", playlist);
        open.putExtra("index", index);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        PendingIntent pPrev = PendingIntent.getService(this, 1, new Intent(this, MusicService.class).setAction(ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        PendingIntent pToggle = PendingIntent.getService(this, 2, new Intent(this, MusicService.class).setAction(ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        PendingIntent pNext = PendingIntent.getService(this, 3, new Intent(this, MusicService.class).setAction(ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(pi)
                .setOngoing(player != null && player.isPlaying())
                .addAction(android.R.drawable.ic_media_previous, "Prev", pPrev)
                .addAction(player != null && player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, player != null && player.isPlaying() ? "Pause" : "Play", pToggle)
                .addAction(android.R.drawable.ic_media_next, "Next", pNext)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title + " - " + artist));

        return b.build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        if (player != null) {
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        super.onDestroy();
    }
}


