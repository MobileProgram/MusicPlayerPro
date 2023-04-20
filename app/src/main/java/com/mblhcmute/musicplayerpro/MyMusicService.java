package com.mblhcmute.musicplayerpro;

import static com.mblhcmute.musicplayerpro.ui.musics.MusicsFragment.musics;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.mblhcmute.musicplayerpro.ui.musics.MusicsFragment;
import com.mblhcmute.musicplayerpro.ui.musics.MusicsViewModel;
import com.mblhcmute.musicplayerpro.utils.MusicUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MyMusicService extends Service implements SongChangeListener, OnProgressUpdateListener {

    boolean onNoti = false;
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

    private static final String CHANNEL_ID = "MyMusicService";
    private static final int NOTIFICATION_ID = 1;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My Music Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
    public void playMusicAt(int position){
        currentSongIndex = position;
        MediaItem mediaItem = MediaItem.fromUri(musics.get(position).getMusicFile());
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        currentIndex(position);
        try {
            if (!onNoti){
                sendNotification(musics.get(currentSongIndex));
            }
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
        try {
            if (!onNoti){
                sendNotification(musics.get(currentSongIndex));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        createNotificationChannel();

        if(player != null){
            try {
                sendNotification(musics.get(currentSongIndex));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (intent != null) {
            String action = intent.getAction();
            if("ACTION_PLAYPAUSE_MUSIC".equals(action)){
                playPauseSong();
            }
            if ("ACTION_STOP_MUSIC".equals(action)) {
                // Dừng chơi nhạc
                player.pause();
                // Gỡ bỏ notification
//                NotificationManager notificationManager = (NotificationManager) getSystemService(getApplication().NOTIFICATION_SERVICE);
//                notificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
                onNoti = false;
            }
            if ("ACTION_NEXT_MUSIC".equals(action)) {
                nextSong();
            }
            if("ACTION_PREVIOUS_MUSIC".equals(action)){
                previousSong();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void sendNotification(Music music) throws IOException {

        byte[] image = MusicUtils.getMusicImage(music.getMusicFile().toString(), getApplicationContext());
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

        Intent intent1 = new Intent(this, MainActivity.class);
        PendingIntent openAppIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = new Intent(this, MyMusicService.class);
        intent2.setAction("ACTION_PLAYPAUSE_MUSIC");
        PendingIntent playPauseIntent = PendingIntent.getService(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        intent2.setAction("ACTION_STOP_MUSIC");
        PendingIntent clearIntent = PendingIntent.getService(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        intent2.setAction("ACTION_NEXT_MUSIC");
        PendingIntent nextIntent = PendingIntent.getService(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        intent2.setAction("ACTION_PREVIOUS_MUSIC");
        PendingIntent previousIntent = PendingIntent.getService(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.layout_custom_notification);
        remoteViews.setTextViewText(R.id.tv_title_song, music.getTitle());
        remoteViews.setTextViewText(R.id.tv_single_song, music.getArtist());
        remoteViews.setImageViewBitmap(R.id.img_song, bitmap);

        remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.ic_pause);


        if (isPlaying() && onNoti){
            remoteViews.setOnClickPendingIntent(R.id.img_play_or_pause, playPauseIntent);
            remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.ic_play);
        }
        else{
            remoteViews.setOnClickPendingIntent(R.id.img_play_or_pause, playPauseIntent);
            remoteViews.setImageViewResource(R.id.img_play_or_pause, R.drawable.ic_pause);
        }

        remoteViews.setOnClickPendingIntent(R.id.img_clear, clearIntent);
        remoteViews.setOnClickPendingIntent(R.id.img_next, nextIntent);
        remoteViews.setOnClickPendingIntent(R.id.img_previous, previousIntent);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(openAppIntent)
                .setSound(null)
                .setCustomContentView(remoteViews);


        startForeground(NOTIFICATION_ID, notification.build());
        onNoti = true;
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
        stopForeground(true);
        stopSelf();
        handler.removeCallbacks(updateProgressTask);
        updateProgressTask = null;
        handler = null;
    }
}