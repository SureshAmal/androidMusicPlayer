package com.example.musicplayer;

import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongAdapter
    extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    private final List<SongItem> songList;
    private final OnSongClickListener listener;

    public SongAdapter(List<SongItem> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(
        @NonNull ViewGroup parent,
        int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
            R.layout.item_song,
            parent,
            false
        );
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SongItem song = songList.get(position);
        holder.bind(song, position);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgAlbumArt;
        private TextView tvTitle;
        private TextView tvArtist;
        private TextView tvDuration;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAlbumArt = itemView.findViewById(R.id.imgAlbumArt);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }

        public void bind(SongItem song, int position) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
            tvDuration.setText(song.getFormattedDuration());

            // Load album art
            loadAlbumArt(song);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(position);
                }
            });

            // Add ripple effect
            itemView.setClickable(true);
            itemView.setFocusable(true);
        }

        private void loadAlbumArt(SongItem song) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(itemView.getContext(), song.getUri());
                byte[] albumArt = retriever.getEmbeddedPicture();

                if (albumArt != null) {
                    imgAlbumArt.setImageBitmap(
                        BitmapFactory.decodeByteArray(
                            albumArt,
                            0,
                            albumArt.length
                        )
                    );
                } else {
                    imgAlbumArt.setImageResource(
                        R.drawable.ic_music_placeholder
                    );
                }

                retriever.release();
            } catch (Exception e) {
                // If we can't load album art, use placeholder
                imgAlbumArt.setImageResource(R.drawable.ic_music_placeholder);
            }
        }
    }
}
