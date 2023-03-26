package com.mblhcmute.musicplayerpro;

import android.app.Application;

import timber.log.Timber;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // initialize timber in application class
        Timber.plant(new Timber.DebugTree());
    }
}
