package com.mblhcmute.musicplayerpro;

import static com.mblhcmute.musicplayerpro.ui.musics.MusicsFragment.musics;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.mblhcmute.musicplayerpro.ui.musics.MusicsFragment;
import com.mblhcmute.musicplayerpro.ui.musics.MusicsViewModel;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MyMusicService extends Service implements SongChangeListener, OnProgressUpdateListener {

    ViewModelProvider viewModelProvider;
    MusicsViewModel myViewModel;
    IBinder mBinder = new MyBinder();
    ExoPlayer player;
    private int currentSongIndex = 0;
    List<Music> listMusic = new ArrayList<>();
    Handler handler = new Handler();
    Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                Timber.d("Update progress: entry");
                float currentTimeMs = player.getCurrentPosition();
                float durationMs = player.getDuration();
                long progress = (long) (currentTimeMs / durationMs * 100);
                onProgressUpdate(currentTimeMs, durationMs, progress);
                handler.postDelayed(this, 1000);
            }
        }
    };

    public MyMusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listMusic = musics;

        createPlayer();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Bind", "Method");
        return mBinder;
    }



    @Override
    public void playMusicAt(int position) {
        currentSongIndex = position;
        MediaItem mediaItem = MediaItem.fromUri(musics.get(position).getMusicFile());
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        currentIndex(position);
    }

    @Override
    public void nextSong() {
        int nextSongIndex = currentSongIndex + 1;
        if (nextSongIndex >= listMusic.size()) nextSongIndex = 0;
        listMusic.get(currentSongIndex).setPlaying(false);
        listMusic.get(nextSongIndex).setPlaying(true);
        playMusicAt(nextSongIndex);
    }

    @Override
    public void previousSong() {
        int prevSongListPosition = currentSongIndex - 1;

        if (prevSongListPosition < 0) {
            prevSongListPosition = listMusic.size() - 1;
        }

        listMusic.get(currentSongIndex).setPlaying(false);
        listMusic.get(prevSongListPosition).setPlaying(true);

        playMusicAt(prevSongListPosition);
    }

    @Override
    public void playPauseSong() {
        if (player.isPlaying()) player.pause();
        else player.play();
    }

    @Override
    public void currentIndex(int index) {
        if (listener!=null){
            listener.currentIndex(index);
        }
    }

    @Override
    public void onProgressUpdate(float currentTimeMs, float durationMs, long progress) {
        if (listener != null) {
            listener.onProgressUpdate(currentTimeMs, durationMs, progress);
        }
    }

    private OnProgressUpdateListener listener;

    public void setOnProgressUpdateListener(OnProgressUpdateListener listener) {
        this.listener = listener;
        viewModelProvider = new ViewModelProvider((ViewModelStoreOwner) listener);
        myViewModel= viewModelProvider.get(MusicsViewModel.class);
    }


    public class MyBinder extends Binder {
        public MyMusicService getService() {
            return MyMusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return super.onStartCommand(intent, flags, startId);
    }

    public void release() {
        player.release();
    }

    public Boolean isPlaying() {
        return player.isPlaying();
    }

    public float getDuration() {
        return (float) player.getDuration();
    }

    public float getCurrentPosition() {
        return (float) player.getCurrentPosition();
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    void createPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {

                myViewModel.setIsPlaying(isPlaying());
                updateProgress(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    nextSong();
                }
            }
        });
    }

    public void updateProgress(boolean isPlaying) {
        if (!isPlaying) {
            handler.removeCallbacks(updateProgressTask);
            return;
        }

        handler.post(updateProgressTask);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateProgressTask);
        updateProgressTask = null;
        handler = null;
    }
}