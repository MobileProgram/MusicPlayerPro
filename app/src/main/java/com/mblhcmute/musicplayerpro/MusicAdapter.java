package com.mblhcmute.musicplayerpro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
    private List<Music> list;
    private final Context context;
    private int playingPosition = 0;
    private final SongChangeListener songChangeListener;

    public MusicAdapter(List<Music> list, Context context, SongChangeListener listener) {
        this.list = list;
        this.context = context;
        this.songChangeListener = listener;
    }

    @NonNull
    @Override
    public MusicAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.music_adapter_layout, null));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Music list2 = list.get(position);

        byte[] image = new byte[0];
        try {
            image = getMusicImage(list2.getMusicFile().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(image != null){
                Glide.with(context).asBitmap().load(image).into(holder.musicImg);
            }else{
                Glide.with(context).load(R.drawable.ic_baseline_music_note_24).into(holder.musicImg);
            }

        if (list2.isPlaying()) {
            playingPosition = position;
            holder.rootLayout.setBackgroundResource(R.drawable.round_back_blue_10);
        } else {
            holder.rootLayout.setBackgroundResource(R.drawable.round_back_10);
        }
        String generateDuration = MusicUtils.formatDuration(list2.getDuration());
        holder.title.setText(list2.getTitle());
        holder.artist.setText(list2.getArtist());
        holder.duration.setText(generateDuration);
        holder.rootLayout.setOnClickListener(view -> {

            list.get(playingPosition).setPlaying(false);
            list2.setPlaying(true);

            songChangeListener.playMusicAt(position);

            notifyDataSetChanged();
        });

    }

    public void updateList(List<Music> list){
        this.list = list;
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        return list.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private final RelativeLayout rootLayout;
        private final TextView title;
        private final TextView artist;
        private final TextView duration;
        private final ImageView musicImg;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            rootLayout = itemView.findViewById(R.id.rootLayout);
            title = itemView.findViewById(R.id.musicTitle);
            artist = itemView.findViewById(R.id.musicArtist);
            duration = itemView.findViewById(R.id.musicDuration);
            musicImg = itemView.findViewById(R.id.musicImage);
        }
    }

    private byte[] getMusicImage(String uri) throws IOException {
        byte[] art = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri musicUri = Uri.parse(uri);
            if (musicUri != null) {
                FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
                mmr.setDataSource(context, musicUri);
                art = mmr.getEmbeddedPicture();
            }
        } else {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(uri);
            art = retriever.getEmbeddedPicture();
            retriever.release();
        }
        return art;
    }
}
