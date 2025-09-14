package com.example.musicplayer;

import android.animation.ObjectAnimator;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView imgAlbum;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private Slider seekBar;
    private FloatingActionButton btnPlay;
    private MaterialButton btnNext, btnPrev;

    private MaterialCardView albumArtCard;

    private ArrayList<Uri> playlist;
    private int index = 0;
    private Handler handler = new Handler();
    private ObjectAnimator rotationAnim;

    private boolean isPlaying = false;
    private int currentPosition = 0;
    private int totalDuration = 0;
    private boolean isUserSeeking = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        toolbar = findViewById(R.id.toolbar);
        imgAlbum = findViewById(R.id.imgAlbum);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        seekBar = findViewById(R.id.seekBar);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);

        albumArtCard = findViewById(R.id.albumArtCard);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // No title, as per design
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        playlist = getIntent().getParcelableArrayListExtra("playlist");
        index = getIntent().getIntExtra("index", 0);

        rotationAnim = ObjectAnimator.ofFloat(
            imgAlbum,
            View.ROTATION,
            0f,
            360f
        );
        rotationAnim.setDuration(10000);
        rotationAnim.setRepeatCount(ObjectAnimator.INFINITE);
        rotationAnim.setRepeatMode(ObjectAnimator.RESTART);

        btnPlay.setOnClickListener(v -> togglePlay());
        btnNext.setOnClickListener(v -> playNext());
        btnPrev.setOnClickListener(v -> playPrev());

        seekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                tvCurrentTime.setText(formatDuration((long) value));
            }
        });

        // Initialize Slider valueFrom and valueTo
        seekBar.setValueFrom(0); // Minimum progress
        seekBar.setValueTo(100); // Default maximum, will be updated with song duration

        seekBar.addOnSliderTouchListener(
            new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {
                    isUserSeeking = true;
                    handler.removeCallbacks(tick);
                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider slider) {
                    isUserSeeking = false;
                    currentPosition = (int) slider.getValue();
                    // Send seek command to service
                    android.content.Intent i = new android.content.Intent(
                        PlayerActivity.this,
                        MusicService.class
                    ).setAction(MusicService.ACTION_SEEK);
                    i.putExtra("position", (int) slider.getValue());
                    if (
                        android.os.Build.VERSION.SDK_INT >= 26
                    ) startForegroundService(i);
                    else startService(i);
                    handler.post(tick);
                }
            }
        );

        // initialize service with playlist and start playback
        if (playlist != null && !playlist.isEmpty()) {
            playIndex(index);
        }
    }

    private void updateSongInfo(int idx) {
        if (
            playlist == null ||
            playlist.isEmpty() ||
            idx < 0 ||
            idx >= playlist.size()
        ) return;

        Uri uri = playlist.get(idx);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(this, uri);
            String title = mmr.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_TITLE
            );
            String artist = mmr.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ARTIST
            );
            String durationStr = mmr.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            );
            byte[] albumArt = mmr.getEmbeddedPicture();

            tvTitle.setText(title != null ? title : "Unknown Title");
            tvArtist.setText(artist != null ? artist : "Unknown Artist");

            if (durationStr != null) {
                long duration = Long.parseLong(durationStr);
                totalDuration = (int) duration;
                tvTotalTime.setText(formatDuration(duration));
                seekBar.setValueTo(totalDuration); // Set max for Slider
                seekBar.setValue(0); // Set progress for Slider
                currentPosition = 0;
                tvCurrentTime.setText("0:00");
                seekBar.setLabelFormatter(value ->
                    formatDuration((long) value)
                );
            }

            if (albumArt != null) {
                imgAlbum.setImageBitmap(
                    BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length)
                );
            } else {
                imgAlbum.setImageResource(R.drawable.ic_music_placeholder);
            }
        } catch (Exception e) {
            tvTitle.setText("Unknown Title");
            tvArtist.setText("Unknown Artist");

            imgAlbum.setImageResource(R.drawable.ic_music_placeholder);
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {}
        }
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void playIndex(int idx) {
        if (
            playlist == null ||
            playlist.isEmpty() ||
            idx < 0 ||
            idx >= playlist.size()
        ) return;

        // Update index and song info
        index = idx;
        updateSongInfo(idx);

        // Reset playback state for new song
        isPlaying = false;
        currentPosition = 0;
        seekBar.setValue(0); // Set progress for Slider
        tvCurrentTime.setText("0:00");

        // Send command to service
        android.content.Intent i = new android.content.Intent(
            this,
            MusicService.class
        ).setAction(MusicService.ACTION_INIT);
        i.putParcelableArrayListExtra("playlist", playlist);
        i.putExtra("index", idx);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);

        // Start playing the new song
        startPlay();
    }

    private void updateProgress() {
        // Only update progress if user is not seeking
        if (isPlaying && totalDuration > 0 && !isUserSeeking) {
            currentPosition += 1000; // Add 1 second
            if (currentPosition > totalDuration) {
                currentPosition = totalDuration;
            }
            seekBar.setValue(currentPosition); // Set progress for Slider
            tvCurrentTime.setText(formatDuration(currentPosition));
        }
    }

    private void startPlay() {
        isPlaying = true;
        btnPlay.setImageResource(R.drawable.ic_pause); // Use custom pause icon
        rotationAnim.start();
        handler.post(tick);
    }

    private void pausePlay() {
        isPlaying = false;
        btnPlay.setImageResource(R.drawable.ic_play); // Use custom play icon
        rotationAnim.pause();
        handler.removeCallbacks(tick);
    }

    private void togglePlay() {
        // Toggle UI state first for immediate feedback
        if (isPlaying) {
            pausePlay();
        } else {
            startPlay();
        }
        // Then send command to service
        android.content.Intent i = new android.content.Intent(
            this,
            MusicService.class
        ).setAction(MusicService.ACTION_TOGGLE);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    private void playNext() {
        if (playlist == null || playlist.isEmpty()) return;
        int next = index + 1;
        if (next >= playlist.size()) next = 0;
        index = next;
        playIndex(next);
    }

    private void playPrev() {
        if (playlist == null || playlist.isEmpty()) return;
        int prev = index - 1;
        if (prev < 0) prev = playlist.size() - 1;
        index = prev;
        playIndex(prev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // UI pause only; service continues if desired
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }
}
