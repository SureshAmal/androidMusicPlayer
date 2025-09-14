package com.example.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity
    extends AppCompatActivity
    implements SongAdapter.OnSongClickListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_DIRECTORY_REQUEST = 101;

    // UI Components
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private FloatingActionButton fabSelectFolder;
    private LinearLayout emptyStateLayout;

    // Data
    private ArrayList<SongItem> songList;
    private ArrayList<Uri> playlist;
    private int currentPlayingIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupFabs();
        updateWelcomeMessage();

        // Check permissions and load songs
        if (checkPermissions()) {
            loadDefaultSongs();
        } else {
            requestPermissions();
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        fabSelectFolder = findViewById(R.id.fabSelectFolder);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        songList = new ArrayList<>();
        playlist = new ArrayList<>();
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(songList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(songAdapter);
    }

    private void setupFabs() {
        fabSelectFolder.setOnClickListener(v -> {
            if (checkPermissions()) {
                openDirectoryPicker();
            } else {
                requestPermissions();
            }
        });
    }

    private void updateWelcomeMessage() {
        // Welcome message is handled by the static layout
        // This method can be used for dynamic updates in the future
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) ==
                PackageManager.PERMISSION_GRANTED
            );
        } else {
            return (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) ==
                PackageManager.PERMISSION_GRANTED
            );
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            ) {
                showPermissionRationale();
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.READ_MEDIA_AUDIO },
                    PERMISSION_REQUEST_CODE
                );
            }
        } else {
            if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showPermissionRationale();
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    private void showPermissionRationale() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage(
                "This app needs access to your music files to play them. Please grant the permission to continue."
            )
            .setPositiveButton("Grant", (dialog, which) -> requestPermissions())
            .setNegativeButton("Cancel", (dialog, which) ->
                Toast.makeText(
                    this,
                    "Permission denied. Cannot access music files.",
                    Toast.LENGTH_SHORT
                ).show()
            )
            .show();
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        );

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                loadDefaultSongs();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(permissions[0])) {
                        showSettingsDialog();
                    } else {
                        Toast.makeText(
                            this,
                            "Permission denied. Cannot access music files.",
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                }
            }
        }
    }

    private void showSettingsDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage(
                "You have permanently denied the permission. Please go to Settings and grant the permission manually."
            )
            .setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                );
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (dialog, which) ->
                Toast.makeText(
                    this,
                    "Permission is required to use this app.",
                    Toast.LENGTH_SHORT
                ).show()
            )
            .show();
    }

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_DIRECTORY_REQUEST);
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (
            requestCode == PICK_DIRECTORY_REQUEST &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                loadSongsFromDirectory(treeUri);
            }
        }
    }

    private void loadDefaultSongs() {
        songList.clear();
        playlist.clear();

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (
            Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )
        ) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    );
                    String title = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                        )
                    );
                    String artist = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                        )
                    );
                    String album = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ALBUM
                        )
                    );
                    long duration = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DURATION
                        )
                    );
                    String path = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DATA
                        )
                    );

                    Uri uri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id)
                    );

                    SongItem song = new SongItem();
                    song.setTitle(title != null ? title : "Unknown Title");
                    song.setArtist(artist != null ? artist : "Unknown Artist");
                    song.setAlbum(album != null ? album : "Unknown Album");
                    song.setDuration(duration);
                    song.setUri(uri);
                    song.setPath(path);

                    songList.add(song);
                    playlist.add(uri);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error loading music files: " + e.getMessage());
        }

        songAdapter.notifyDataSetChanged();
        updateUIState();
    }

    private void loadSongsFromDirectory(Uri treeUri) {
        Toast.makeText(
            this,
            "Custom folder selection is not yet fully implemented. Loading default music library instead.",
            Toast.LENGTH_LONG
        ).show();
        loadDefaultSongs();
    }

    private void updateUIState() {
        if (songList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            Toast.makeText(
                this,
                "Loaded " + songList.size() + " songs",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSongClick(int position) {
        if (position >= 0 && position < playlist.size()) {
            currentPlayingIndex = position;

            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putParcelableArrayListExtra("playlist", playlist);
            intent.putExtra("index", position);
            startActivity(intent);
        }
    }

    private void playNext() {
        if (
            currentPlayingIndex >= 0 &&
            currentPlayingIndex < songList.size() - 1
        ) {
            currentPlayingIndex++;

            // TODO: Communicate with MusicService to play next
        }
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("About Music Player")
            .setMessage(
                "A beautiful music player app\n\nVersion 1.0\nBuilt with Material Design 3"
            )
            .setPositiveButton("OK", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions() && songList.isEmpty()) {
            loadDefaultSongs();
        }
    }
}
