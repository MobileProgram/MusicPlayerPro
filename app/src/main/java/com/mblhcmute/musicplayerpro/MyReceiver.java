package com.mblhcmute.musicplayerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int actionMusic = intent.getIntExtra("acton_music", 0);
        Intent intentService = new Intent(context, MyMusicService.class);
        intentService.putExtra("acton_music_service", actionMusic);
        context.startService(intentService);
    }
}