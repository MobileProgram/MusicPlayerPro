package com.mblhcmute.musicplayerpro.ui.musics;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MusicsViewModel extends ViewModel {
    MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<MusicsScreenState> uiEvent = new MutableLiveData<>();

    public MutableLiveData<MusicsScreenState> getUiEvent() {
        return uiEvent;
    }

    public MutableLiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }
    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying.setValue(isPlaying);
    }
    public void onClick(MusicsScreenState state) {
        uiEvent.setValue(state);
    }
}