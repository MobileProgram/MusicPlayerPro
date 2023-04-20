package com.mblhcmute.musicplayerpro;

public interface OnProgressUpdateListener {
    void onProgressUpdate(float currentTimeMs, float durationMs, long progress);

    void currentIndex(int index);
}


