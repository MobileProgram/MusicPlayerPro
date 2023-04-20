package com.mblhcmute.musicplayerpro;

import static com.mblhcmute.musicplayerpro.MainApplication.CHANNEL_ID;
import static com.mblhcmute.musicplayerpro.MainApplication.NOTIFICATION_ID;
import static com.mblhcmute.musicplayerpro.ui.fragments.musics.MusicsFragment.musics;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.mblhcmute.musicplayerpro.interfaces.OnProgressUpdateListener;
import com.mblhcmute.musicplayerpro.interfaces.SongChangeListener;
import com.mblhcmute.musicplayerpro.models.Music;
import com.mblhcmute.musicplayerpro.ui.activity.MainActivity;
import com.mblhcmute.musicplayerpro.ui.fragments.musics.MusicsViewModel;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MyMusicService extends Service implements SongChangeListener, OnProgressUpdateListener {

    public static final String ACTION_PLAYPAUSE_MUSIC = "ACTION_PLAYPAUSE_MUSIC";
    public static final String ACTION_STOP_MUSIC = "ACTION_STOP_MUSIC";
    public static final String ACTION_NEXT_MUSIC = "ACTION_NEXT_MUSIC";
    public static final String ACTION_PREVIOUS_MUSIC = "ACTION_PREVIOUS_MUSIC";
    NotificationCompat.Builder notification;
    RemoteViews remoteViews;

    boolean onNoti = false;
    ViewModelProvider viewModelProvider;
    MusicsViewModel myViewModel;
    IBinder mBinder = new MyBinder();
    ExoPlayer player;
    private int currentSongIndex = 0;
    List<Music> listMusic = new ArrayList<>(musics);
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
        createPlayer();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Bind", "Method");
        return mBinder;
    }

    public List<Music> getListMusic() {
        return listMusic;
    }

    @Override
    public void playMusicAt(int position) {
        currentSongIndex = position;
        MediaItem mediaItem = MediaItem.fromUri(listMusic.get(position).getMusicFile());
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        currentIndex(position);
    }

    public void updateNotification() {
        try {
            getOrUpdateNoti(listMusic.get(currentSongIndex));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (listener != null) {
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
        myViewModel = viewModelProvider.get(MusicsViewModel.class);
    }


    public class MyBinder extends Binder {
        public MyMusicService getService() {
            return MyMusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action == null) action = "";
            switch (action) {
                case ACTION_PLAYPAUSE_MUSIC:
                    playPauseSong();
                    break;
                case ACTION_STOP_MUSIC:
                    player.pause();
                    NotificationManager notificationManager = (NotificationManager) getSystemService(getApplication().NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_ID);
                    stopForeground(true);
                    onNoti = false;
                    break;
                case ACTION_NEXT_MUSIC:
                    nextSong();
                    break;
                case ACTION_PREVIOUS_MUSIC:
                    previousSong();
                    break;
                default:
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void getOrUpdateNoti(Music music) throws IOException {

        Intent intent1 = new Intent(this, MainActivity.class);
        PendingIntent openAppIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteViews = getOrUpdateRemoteView(music);

        if (notification == null)
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setContentIntent(openAppIntent)
                .setSound(null)
                .setCustomContentView(remoteViews);
        startForeground(NOTIFICATION_ID, notification.build());
        onNoti = true;
    }

    @NonNull
    private RemoteViews getOrUpdateRemoteView(Music music) throws IOException {

        Bitmap bitmap;
        byte[] image = MusicUtils.getMusicImage(music.getMusicFile().toString(), getApplicationContext());
        if (image != null) bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        else bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_baseline_music_note_24);

        if (remoteViews != null) {
            remoteViews.setImageViewResource(R.id.img_play_or_pause, isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        }else{
            remoteViews = new RemoteViews(getPackageName(), R.layout.layout_custom_notification);
            Intent intent = new Intent(this, MyMusicService.class);

            intent.setAction(ACTION_PLAYPAUSE_MUSIC);
            PendingIntent playPauseIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            intent.setAction(ACTION_STOP_MUSIC);
            PendingIntent clearIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            intent.setAction(ACTION_NEXT_MUSIC);
            PendingIntent nextIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            intent.setAction(ACTION_PREVIOUS_MUSIC);
            PendingIntent previousIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.ic_pause);

            remoteViews.setOnClickPendingIntent(R.id.img_play_or_pause, playPauseIntent);
            remoteViews.setOnClickPendingIntent(R.id.img_clear, clearIntent);
            remoteViews.setOnClickPendingIntent(R.id.img_next, nextIntent);
            remoteViews.setOnClickPendingIntent(R.id.img_previous, previousIntent);
        }

        remoteViews.setTextViewText(R.id.tv_title_song, music.getTitle());
        remoteViews.setTextViewText(R.id.tv_single_song, music.getArtist());
        remoteViews.setImageViewBitmap(R.id.img_song, bitmap);

        return remoteViews;
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
                updateNotification();
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
        stopForeground(true);
        stopSelf();
        handler.removeCallbacks(updateProgressTask);
        updateProgressTask = null;
        handler = null;
    }
}