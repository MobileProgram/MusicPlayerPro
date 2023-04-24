package com.mblhcmute.musicplayerpro.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mblhcmute.musicplayerpro.models.Music;
import com.mblhcmute.musicplayerpro.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class MusicUtils {
    public static String formatDuration(Object duration) {
        try {
            float msTime = Float.parseFloat(String.valueOf(duration));
            Timber.d("FormatTime init: " + msTime);
            return String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) msTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) msTime) - TimeUnit.MINUTES.toSeconds((TimeUnit.MILLISECONDS.toMinutes((long) msTime))));
        } catch (Exception ex) {
            Timber.e("Error: " + ex);
            return String.valueOf(duration);
        }
    }

    public static byte[] getMusicImage(String uri, Context context) throws IOException {
        byte[] art = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context,Uri.parse(uri));
            art = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return art;
    }

    @SuppressLint("Range")
    public static List<Music> getMusicFiles(@NonNull Context context) {
        final List<Music> musics = new ArrayList<>();

        ContentResolver resolver = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DURATION
        };

        String selection = MediaStore.Audio.AudioColumns.IS_MUSIC + " != 0";

        @SuppressLint("Recycle") Cursor cursor = resolver.query(uri, projection, selection, null, null);

        if (cursor == null) {
            Toast.makeText(context, "Something went wrong!", Toast.LENGTH_SHORT).show();
        } else if (!cursor.moveToNext()) {
            Toast.makeText(context, "No Music Found!", Toast.LENGTH_SHORT).show();
        } else {
            do {
                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);
                String getDuration = "00:00";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final Music music = new Music(getMusicFileName, getArtistName, getDuration, false, musicFileUri);
                musics.add(music);
            } while (cursor.moveToNext());
        }
        assert cursor != null;
        cursor.close();
        return musics;
    }

    @BindingAdapter("setPlayImage")
    public static void setPlayImage(FloatingActionButton view, Boolean isPlaying) {
        int imageRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        view.setImageResource(imageRes);
    }
}
