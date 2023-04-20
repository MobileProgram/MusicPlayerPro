package com.mblhcmute.musicplayerpro.utils;

import androidx.recyclerview.widget.DiffUtil;

import com.mblhcmute.musicplayerpro.Music;

import java.util.List;

public class MyDiffCallback extends DiffUtil.Callback {

    private List<Music> oldList;
    private List<Music> newList;

    public MyDiffCallback(List<Music> oldList, List<Music> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getMusicFile() == newList.get(newItemPosition).getMusicFile();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final Music oldItem = oldList.get(oldItemPosition);
        final Music newItem = newList.get(newItemPosition);
        return oldItem.equals(newItem);
    }
}
