package com.mblhcmute.musicplayerpro.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.mblhcmute.musicplayerpro.Music;
import com.mblhcmute.musicplayerpro.MusicAdapter;
import com.mblhcmute.musicplayerpro.MusicUtils;
import com.mblhcmute.musicplayerpro.SongChangeListener;
import com.mblhcmute.musicplayerpro.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class HomeFragment extends Fragment implements SongChangeListener {

    private final List<Music> musics = new ArrayList<>();
    private RecyclerView playerRecycler;
    private ExoPlayer player;
    private int currentSongIndex = 0;
    private MusicAdapter musicAdapter;
    HomeViewModel viewModel;
    private FragmentHomeBinding binding;
    Handler handler = new Handler();
    Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                Timber.d("Update progress: entry");
                float currentTimeMs = player.getCurrentPosition();
                float durationMs = player.getDuration();
                long progress = (long) (currentTimeMs / durationMs * 100);
                binding.currentTime.setText(MusicUtils.formatDuration(currentTimeMs));
                binding.endTime.setText(MusicUtils.formatDuration(durationMs));
                binding.playerSeekBar.setValue((int) progress);
                handler.postDelayed(this, 1000);
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        binding.setVm(viewModel);
        binding.setLifecycleOwner(this);
        playerRecycler = binding.musicRecyclerView;
        playerRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        player = new ExoPlayer.Builder(requireContext()).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                viewModel.isPlaying.postValue(isPlaying);
                updateProgress(isPlaying);
            }
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                   nextSong();
                }
            }
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) getMusicFiles();
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
            else getMusicFiles();
        }
        binding.playerSeekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                binding.playerSeekBar.setLabelFormatter(progress -> MusicUtils.formatDuration(player.getDuration() / 100 * progress));
                player.seekTo((long) (value * player.getDuration() / 100));
            }
        });
        return binding.getRoot();
    }

    private void updateProgress(boolean isPlaying) {
        if (!isPlaying) {
            handler.removeCallbacks(updateProgressTask);
            return;
        }

        handler.post(updateProgressTask);
    }

    private void getMusicFiles() {
        musics.addAll(MusicUtils.getMusicFiles(requireContext()));
        musicAdapter = new MusicAdapter(musics, getContext(), this);
        playerRecycler.setAdapter(musicAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            switch (event) {
                case PlayPauseClick: {
                    if (player.isPlaying()) player.pause();
                    else player.play();
                    break;
                }
                case NextClick: {
                    nextSong();
                    break;
                }
                case PreviousClick: {
                    previousSong();
                    break;
                }
            }
        });
    }

    private void previousSong() {
        int prevSongListPosition = currentSongIndex - 1;

        if (prevSongListPosition < 0) {
            prevSongListPosition = musics.size() - 1;
        }

        musics.get(currentSongIndex).setPlaying(false);
        musics.get(prevSongListPosition).setPlaying(true);

        playerRecycler.scrollToPosition(prevSongListPosition);

        musicAdapter.updateList(musics);

        playMusicAt(prevSongListPosition);
    }

    private void nextSong() {
        int nextSongIndex = currentSongIndex + 1;
        if (nextSongIndex >= musics.size()) nextSongIndex = 0;
        musics.get(currentSongIndex).setPlaying(false);
        musics.get(nextSongIndex).setPlaying(true);
        playerRecycler.scrollToPosition(nextSongIndex);
        musicAdapter.updateList(musics);
        playMusicAt(nextSongIndex);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) getMusicFiles();
        else Toast.makeText(this.getContext(), "Permission declined by user", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressTask);
        updateProgressTask = null;
        handler = null;
    }

    @Override
    public synchronized void playMusicAt(int position) {
        currentSongIndex = position;
        MediaItem mediaItem = MediaItem.fromUri(musics.get(position).getMusicFile());
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
}