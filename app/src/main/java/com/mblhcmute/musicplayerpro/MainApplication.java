package com.mblhcmute.musicplayerpro;

import android.app.Application;
import android.app.NotificationChannel;
import android.os.Build;
import android.app.NotificationManager;
import android.os.Build;


import timber.log.Timber;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // initialize timber in application class
        Timber.plant(new Timber.DebugTree());
    }
}
