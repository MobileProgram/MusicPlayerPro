package com.mblhcmute.musicplayerpro.ui.fragments.musics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mblhcmute.musicplayerpro.R;
import com.mblhcmute.musicplayerpro.interfaces.SongChangeListener;
import com.mblhcmute.musicplayerpro.models.Music;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.io.IOException;
import java.util.List;

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
        Music currentSong = list.get(position);
        byte[] image = new byte[0];
        try {
            image = MusicUtils.getMusicImage(currentSong.getMusicFile().toString(), context);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(image != null){
            Glide.with(context).load(image).into(holder.musicImg);
        }else{
            String imagePath = currentSong.getMusicFile().toString().replace(".mp3", ".jpg");
            if (imagePath != null) {
                Glide.with(context).load(imagePath).into(holder.musicImg);
            }else {
                Glide.with(context).load(R.drawable.ic_baseline_music_note_24).into(holder.musicImg);
            }
        }

        if (currentSong.isPlaying()) {
            playingPosition = position;
            holder.rootLayout.setBackgroundResource(R.drawable.round_back_blue_10);
        } else {
            holder.rootLayout.setBackgroundResource(R.drawable.round_back_10);
        }
        String generateDuration = MusicUtils.formatDuration(currentSong.getDuration());
        holder.title.setText(currentSong.getTitle());
        holder.artist.setText(currentSong.getArtist());
        holder.duration.setText(generateDuration);
        holder.rootLayout.setOnClickListener(view -> {
            list.get(playingPosition).setPlaying(false);
            changeSong(playingPosition, position);
            currentSong.setPlaying(true);
            songChangeListener.playMusicAt(position);
        });

    }

    public void changeSong(int fromSong, int toSong) {
        notifyItemChanged(fromSong);
        notifyItemChanged(toSong);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

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


}
