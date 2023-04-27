package com.mblhcmute.musicplayerpro.ui.fragments.musics;

import static android.content.Context.BIND_AUTO_CREATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.slider.Slider;
import com.mblhcmute.musicplayerpro.MyMusicService;
import com.mblhcmute.musicplayerpro.databinding.FragmentMusicsBinding;
import com.mblhcmute.musicplayerpro.interfaces.OnMusicLoadedListener;
import com.mblhcmute.musicplayerpro.interfaces.OnProgressUpdateListener;
import com.mblhcmute.musicplayerpro.interfaces.SongChangeListener;
import com.mblhcmute.musicplayerpro.models.Music;
import com.mblhcmute.musicplayerpro.ui.fragments.player.PlayerFragment;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MusicsFragment extends Fragment implements SongChangeListener, ServiceConnection, OnProgressUpdateListener {

    public static boolean canUpdate = true;
    public static final List<Music> musics = new ArrayList<>();
    private RecyclerView playerRecycler;
    private int currentSongIndex = 0;
    private MusicAdapter musicAdapter;
    private MusicsViewModel viewModel;
    private FragmentMusicsBinding binding;
    public static MyMusicService myMusicService;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MusicsViewModel.class);

        binding = FragmentMusicsBinding.inflate(inflater, container, false);
        binding.setVm(viewModel);
        binding.setLifecycleOwner(this);
        playerRecycler = binding.musicRecyclerView;
        playerRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

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

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getMusicFiles();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        return binding.getRoot();
    }


    private void getMusicFiles() {
        //Nếu chưa có service thì load nhạc
        if (myMusicService == null || musics.size() == 0) {
            musics.clear();
            //LOAD LOCAL
            musics.addAll(MusicUtils.getMusicFiles(requireContext()));
            //LOAD FIREBASE
            MusicUtils.getMusicFilesFromFirebase(requireContext(), new OnMusicLoadedListener() {
                @Override
                public void onMusicLoaded(List<Music> musicList) {
                    musics.addAll(musicList);
                }

                @Override
                public void onMusicLoadFailed(String message) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }
        //SET ADAPTER
        musicAdapter = new MusicAdapter(musics, getContext(), this);
        playerRecycler.setAdapter(musicAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            getMusicFiles();
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 11);
            else getMusicFiles();
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
        int lastSongIndex = currentSongIndex;
        currentSongIndex = index;
        musicAdapter.changeSong(lastSongIndex, currentSongIndex);
        playerRecycler.scrollToPosition(currentSongIndex);
    }

    public MusicAdapter getMusicAdapter() {
        return musicAdapter;
    }

    public void setMusicAdapter(MusicAdapter musicAdapter) {
        this.musicAdapter = musicAdapter;
    }

    public RecyclerView getPlayerRecycler() {
        return playerRecycler;
    }

    public void setPlayerRecycler(RecyclerView playerRecycler) {
        this.playerRecycler = playerRecycler;
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

    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                if (isGranted.containsValue(false)) {
                    Toast.makeText(getContext(), "Permission declined by user", Toast.LENGTH_SHORT).show();
                } else {
                    getMusicFiles();
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 11) {
            Map<String, Boolean> isGranted = new HashMap<>();
            for (int i = 0; i < permissions.length; i++) {
                isGranted.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            requestPermissionLauncher.launch(permissions);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        myMusicService = null;
//        Intent intent = new Intent(getActivity(), MyMusicService.class);
//        requireActivity().stopService(intent);
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
        myMusicService.setOnProgressUpdateListenerA(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        myMusicService = null;
    }

    @Override
    public void onProgressUpdate(float currentTimeMs, float durationMs, long progress) {
        if (myMusicService == null || binding == null) return;
        binding.currentTime.setText(MusicUtils.formatDuration(currentTimeMs));
        binding.endTime.setText(MusicUtils.formatDuration(durationMs));
        binding.playerSeekBar.setValue((int) progress);
    }
}