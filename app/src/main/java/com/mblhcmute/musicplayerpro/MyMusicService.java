package com.mblhcmute.musicplayerpro;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyMusicService extends Service {
    public MyMusicService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}