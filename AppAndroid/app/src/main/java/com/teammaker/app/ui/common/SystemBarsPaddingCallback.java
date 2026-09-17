package com.teammaker.app.ui.common;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Con targetSdk 36 Android 15/16 forza l'edge-to-edge: il contenuto va sotto
 * status bar e nav bar se non lo diciamo esplicitamente. Registrato in
 * TeamMakerApplication, applica automaticamente a ogni Activity un padding
 * top pari all'altezza della status bar (variabile per notch/dynamic island)
 * e un padding bottom fisso "minimo" — cosi' non tocchiamo le 27 Activity a mano.
 */
public class SystemBarsPaddingCallback implements Application.ActivityLifecycleCallbacks {

    /** Padding sotto in dp: appena visibile per staccare i contenuti dalla nav bar. */
    private static final int BOTTOM_PADDING_DP = 4;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        final int bottomPaddingPx = (int) (BOTTOM_PADDING_DP
                * activity.getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // top = insets.top pieni; bottom = 4dp fisso, ignorando la nav bar reale.
            v.setPadding(bars.left, bars.top, bars.right, bottomPaddingPx);
            return insets;
        });
    }

    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityResumed(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
