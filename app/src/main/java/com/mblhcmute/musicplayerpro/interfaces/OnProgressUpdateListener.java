package com.mblhcmute.musicplayerpro.interfaces;

public interface OnProgressUpdateListener {
    void onProgressUpdate(float currentTimeMs, float durationMs, long progress);

    void currentIndex(int index);

    void updatePlayPauseButton(boolean isPlaying);
}


