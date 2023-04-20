package com.mblhcmute.musicplayerpro.ui.musics;

import static android.content.Context.BIND_AUTO_CREATE;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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

import com.mblhcmute.musicplayerpro.MainActivity;
import com.mblhcmute.musicplayerpro.Music;
import com.mblhcmute.musicplayerpro.MusicAdapter;
import com.mblhcmute.musicplayerpro.MyMusicService;
import com.mblhcmute.musicplayerpro.OnProgressUpdateListener;
import com.mblhcmute.musicplayerpro.R;
import com.mblhcmute.musicplayerpro.SongChangeListener;
import com.mblhcmute.musicplayerpro.databinding.FragmentMusicsBinding;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.util.ArrayList;
import java.util.List;

public class MusicsFragment extends Fragment implements SongChangeListener, ServiceConnection, OnProgressUpdateListener {

    public static final List<Music> musics = new ArrayList<>();
    private RecyclerView playerRecycler;
    private int currentSongIndex = 0;
    private MusicAdapter musicAdapter;
    private MusicsViewModel viewModel;
    private FragmentMusicsBinding binding;
    MyMusicService myMusicService;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MusicsViewModel.class);

        binding = FragmentMusicsBinding.inflate(inflater, container, false);
        binding.setVm(viewModel);
        binding.setLifecycleOwner(this);
        playerRecycler = binding.musicRecyclerView;
        playerRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            getMusicFiles();
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
            else getMusicFiles();
        }


        binding.playerSeekBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                binding.playerSeekBar.setLabelFormatter(progress -> MusicUtils.formatDuration(myMusicService.getDuration() / 100 * progress));
                myMusicService.seekTo((long) (value * myMusicService.getDuration() / 100));
            }
        });

        return binding.getRoot();
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
    public void onResume() {

        Intent intent = new Intent(requireContext(), MyMusicService.class);
        if (myMusicService == null) {
            requireContext().bindService(intent, this, BIND_AUTO_CREATE);
            requireContext().startService(intent);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
//        myMusicService.unbindService(this);
        super.onPause();
    }

    @Override
    public synchronized void playPauseSong() {
        myMusicService.playPauseSong();
    }

    @Override
    public void currentIndex(int index) {
        currentSongIndex = index;
        playerRecycler.scrollToPosition(currentSongIndex);
        musicAdapter.updateList(musics);
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
    }

    @Override
    public synchronized void playMusicAt(int position) {
        currentSongIndex = position;
        myMusicService.playMusicAt(position);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MyMusicService.MyBinder myBinder = (MyMusicService.MyBinder) iBinder;
        myMusicService = myBinder.getService();
        myMusicService.setOnProgressUpdateListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        myMusicService = null;
    }

    @Override
    public void onProgressUpdate(float currentTimeMs, float durationMs, long progress) {
        binding.currentTime.setText(MusicUtils.formatDuration(currentTimeMs));
        binding.endTime.setText(MusicUtils.formatDuration(durationMs));
        binding.playerSeekBar.setValue((int) progress);
    }
}