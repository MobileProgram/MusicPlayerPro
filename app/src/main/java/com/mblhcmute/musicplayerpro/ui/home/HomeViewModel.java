package com.mblhcmute.musicplayerpro.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<HomeScreenState> uiEvent = new MutableLiveData<>();

    public MutableLiveData<HomeScreenState> getUiEvent() {
        return uiEvent;
    }

    public MutableLiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public void onClick(HomeScreenState state) {
        uiEvent.setValue(state);
    }
}