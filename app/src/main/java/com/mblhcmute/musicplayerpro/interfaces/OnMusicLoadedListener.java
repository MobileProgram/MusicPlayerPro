package com.mblhcmute.musicplayerpro.interfaces;

import com.mblhcmute.musicplayerpro.models.Music;

import java.util.List;

public interface OnMusicLoadedListener {
    void onMusicLoaded(List<Music> musicList);
    void onMusicLoadFailed(String message);
}
