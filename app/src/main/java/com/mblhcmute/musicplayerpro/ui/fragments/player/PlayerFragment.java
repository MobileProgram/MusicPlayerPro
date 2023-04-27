package com.mblhcmute.musicplayerpro.ui.fragments.player;

import static com.mblhcmute.musicplayerpro.ui.fragments.musics.MusicsFragment.canUpdate;
import static com.mblhcmute.musicplayerpro.ui.fragments.musics.MusicsFragment.musics;
import static com.mblhcmute.musicplayerpro.ui.fragments.musics.MusicsFragment.myMusicService;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.Slider;
import com.mblhcmute.musicplayerpro.R;
import com.mblhcmute.musicplayerpro.databinding.FragmentPlayerBinding;
import com.mblhcmute.musicplayerpro.interfaces.OnProgressUpdateListener;
import com.mblhcmute.musicplayerpro.interfaces.SongChangeListener;
import com.mblhcmute.musicplayerpro.ui.fragments.musics.MusicsFragment;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.io.IOException;
import java.util.Objects;

public class PlayerFragment extends Fragment implements SongChangeListener, OnProgressUpdateListener{

    private FragmentPlayerBinding binding;
    private PlayerViewModel viewModel;
    private int currentSongIndex = myMusicService.getCurrentSongIndex();

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        binding.setVm(viewModel);
        binding.setLifecycleOwner(this);


        binding.playerSeekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                binding.playerSeekBar.setLabelFormatter(progress -> MusicUtils.formatDuration(myMusicService.getDuration() / 100 * progress));
                myMusicService.seekTo((long) (value * myMusicService.getDuration() / 100));
            }
        });

        binding.playerSeekBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                canUpdate = false;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                canUpdate = true;
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (myMusicService != null){
            myMusicService.setOnProgressUpdateListenerB(this);
            myMusicService.updatePlayPauseButton(myMusicService.isPlaying());
            setInformationBinding(myMusicService.getCurrentSongIndex());
        }
        viewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            switch (event) {
                case PlayPauseClick: {
                    playPauseSong();
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

    @Override
    public synchronized void playPauseSong() {
        myMusicService.playPauseSong();
    }

    @Override
    public void currentIndex(int index) {
        currentSongIndex = index;
        setInformationBinding(index);
    }

    private void setInformationBinding(int index) {
        if (binding == null) return;
        byte[] image = new byte[0];
        try {
            image = MusicUtils.getMusicImage(musics.get(index).getMusicFile().toString(), requireContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(image != null){
            Glide.with(requireContext()).asBitmap().load(image).into(binding.coverArt);
        }else{
            String imagePath = musics.get(index).getMusicFile().toString().replace(".mp3", ".jpg");
            if (imagePath.contains("firebase")) {
                Glide.with(requireContext()).load(imagePath).into(binding.coverArt);
            }else {
                Glide.with(requireContext()).load(R.mipmap.ic_music).into(binding.coverArt);
            }
        }
        binding.songName.setText(musics.get(index).getTitle());
        binding.songSinger.setText(musics.get(index).getArtist());
    }

    @Override
    public void updatePlayPauseButton(boolean isPlaying) {
        viewModel.setIsPlaying(isPlaying);
    }

    @Override
    public synchronized void previousSong() {
        myMusicService.previousSong();
    }

    @Override
    public synchronized void nextSong() {
        myMusicService.nextSong();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public synchronized void playMusicAt(int position) {
        currentSongIndex = position;
        myMusicService.playMusicAt(position);
    }

    @Override
    public void onProgressUpdate(float currentTimeMs, float durationMs, long progress){
        if (myMusicService == null || binding == null) return;
        binding.currentTime.setText(MusicUtils.formatDuration(currentTimeMs));
        binding.endTime.setText(MusicUtils.formatDuration(durationMs));
        binding.playerSeekBar.setValue((int) progress);
    }
}