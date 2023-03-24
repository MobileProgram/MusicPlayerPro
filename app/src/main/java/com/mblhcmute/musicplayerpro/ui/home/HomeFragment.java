package com.mblhcmute.musicplayerpro.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mblhcmute.musicplayerpro.MusicAdapter;
import com.mblhcmute.musicplayerpro.MusicList;
import com.mblhcmute.musicplayerpro.R;
import com.mblhcmute.musicplayerpro.SongChangeListener;
import com.mblhcmute.musicplayerpro.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment implements SongChangeListener {

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView playerRecycler;
    private MediaPlayer mediaPlayer;
    private TextView endTime,startTime;
    private boolean isPlaying = false;
    private SeekBar playerSeekBar;
    private ImageView playPauseImage;
    private CardView playPausedCard;
    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        View decodeView = requireActivity().getWindow().getDecorView();

        int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decodeView.setSystemUiVisibility(options);

        final LinearLayout btnSearch = binding.btnSearch;
        final LinearLayout btnMenu = binding.btnMenu;
        playerRecycler = binding.musicRecyclerView;
        playPausedCard = binding.playPauseCard;
        playPauseImage = binding.playPauseImg;
        final ImageView btnNext = binding.btnNext;
        final ImageView btnPrevious = binding.btnPrevious;

        playerSeekBar = binding.playerSeekBar;
        startTime = binding.startTime;
        endTime = binding.endTime;

        playerRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mediaPlayer = new MediaPlayer();

        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            getMusicFiles();
        }else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},11);
            }else{
                getMusicFiles();
            }
        }

        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int nextSongListPosition = currentSongListPosition+1;

                if(nextSongListPosition>=musicLists.size()){
                    nextSongListPosition=0;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(nextSongListPosition).setPlaying(true);

                playerRecycler.scrollToPosition(nextSongListPosition);

                musicAdapter.updateList(musicLists);

                onChanged(nextSongListPosition);
            }
        });

        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int prevSongListPosition = currentSongListPosition-1;

                if(prevSongListPosition<0){
                    prevSongListPosition=musicLists.size()-1;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(prevSongListPosition).setPlaying(true);

                playerRecycler.scrollToPosition(prevSongListPosition);

                musicAdapter.updateList(musicLists);

                onChanged(prevSongListPosition);
            }
        });

        playPausedCard.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if(isPlaying){
                    isPlaying=false;

                    mediaPlayer.pause();
                    playPauseImage.setImageResource(R.drawable.ic_play);
                }else{
                    isPlaying=true;
                    mediaPlayer.start();
                    playPauseImage.setImageResource(R.drawable.ic_pause);
                }
            }
        });
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    if(isPlaying){
                        mediaPlayer.seekTo(i);
                    }else{
                        mediaPlayer.seekTo(0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
//        final TextView textView = binding.textHome;
//        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return binding.getRoot();
    }

    @SuppressLint("Range")
    private void getMusicFiles() {
        ContentResolver resolver = requireContext().getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        @SuppressLint("Recycle") Cursor cursor =resolver.query(uri,null,MediaStore.Audio.Media.DATA+" LIKE?",
                new String[]{"%.mp3%"},null);
        if (cursor == null){
            Toast.makeText(this.getContext(),"Something went wrong!",Toast.LENGTH_SHORT).show();
        }else if (!cursor.moveToNext()){
            Toast.makeText(this.getContext(),"No Music Found!",Toast.LENGTH_SHORT).show();
        }else{
            do {
                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));;
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cursorId);
                String getDuration = "00:00";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(getMusicFileName,getArtistName,getDuration,false,musicFileUri);
                musicLists.add(musicList);
            }while (cursor.moveToNext());

            musicAdapter= new MusicAdapter(musicLists,getContext(),this);
            playerRecycler.setAdapter(musicAdapter);

        }
        assert cursor != null;
        cursor.close();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();
        } else {
            Toast.makeText(this.getContext(), "Permission declined by user", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onChanged(int position) {
        currentSongListPosition = position;
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(getContext(),musicLists.get(position).getMusicFile());
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Unable to play track!", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                final int getTotalDuration = mp.getDuration();

                String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration),
                        TimeUnit.MILLISECONDS.toSeconds(getTotalDuration)-
                        TimeUnit.MINUTES.toSeconds((TimeUnit.MILLISECONDS.toMinutes(getTotalDuration))));
                 endTime.setText(generateDuration);
                 isPlaying = true;

                mp.start();

                playerSeekBar.setMax(getTotalDuration);

                playPauseImage.setImageResource(R.drawable.ic_pause);
            }
        });

        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();
                        String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration),
                                TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration)-
                                        TimeUnit.MINUTES.toSeconds((TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration))));
                        playerSeekBar.setProgress(getCurrentDuration);
                        startTime.setText(generateDuration);

                    }
                });

            }
        },1000,1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();

                timer.purge();
                timer.cancel();

                isPlaying   = false;

                playPauseImage.setImageResource(R.drawable.ic_play);

                playerSeekBar.setProgress(0);
            }
        });
    }
}