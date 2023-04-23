package com.mblhcmute.musicplayerpro.ui.fragments.musics;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mblhcmute.musicplayerpro.utils.SingleLiveEvent;

public class MusicsViewModel extends ViewModel {
    MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final SingleLiveEvent<MusicsScreenState> uiEvent = new SingleLiveEvent<>();

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