package com.mblhcmute.musicplayerpro.interfaces;

public interface SongChangeListener {
    void playMusicAt(int position);
    void nextSong();
    void previousSong();
    void playPauseSong();
}
