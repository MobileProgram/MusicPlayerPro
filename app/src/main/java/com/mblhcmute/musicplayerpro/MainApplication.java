package com.mblhcmute.musicplayerpro;

import android.app.Application;
import android.app.NotificationChannel;
import android.os.Build;
import android.app.NotificationManager;
import android.os.Build;


import timber.log.Timber;

public class MainApplication extends Application {

    public static final String CHANNEL_ID = "MyMusicService";
    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        // initialize timber in application class
        Timber.plant(new Timber.DebugTree());
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My Music Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
