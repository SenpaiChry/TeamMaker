package com.teammaker.app.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import com.teammaker.app.R;

/**
 * Wrapper minimale su MediaPlayer per riprodurre un file da res/raw una volta
 * sola, liberando le risorse a fine playback (o su errore).
 */
public class Sound {

    private static final String TAG = "Sound";

    /** Riproduce il file indicato (es. R.raw.fatass). Silenzioso in caso di errore. */
    public static void play(Context context, int rawResId) {
        try {
            MediaPlayer mp = MediaPlayer.create(context.getApplicationContext(), rawResId);
            if (mp == null) {
                Log.w(TAG, "MediaPlayer.create ha restituito null per resId=" + rawResId);
                return;
            }
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.setOnErrorListener((player, what, extra) -> {
                Log.w(TAG, "Errore playback (" + what + "/" + extra + ")");
                player.release();
                return true;
            });
            mp.start();
        } catch (Exception e) {
            Log.e(TAG, "Errore play sound", e);
        }
    }
}
