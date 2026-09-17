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

import com.teammaker.app.R;

/**
 * Con targetSdk 36 Android 15/16 forza l'edge-to-edge: il contenuto va sotto
 * status bar e nav bar se non lo diciamo esplicitamente. Registrato in
 * TeamMakerApplication, applica automaticamente a ogni Activity:
 *
 *   - padding top    = altezza status bar (dinamico: adatta a notch / dynamic island)
 *   - padding bottom = insets.bottom del sistema (gesture nav ~24dp, 3-button ~48dp)
 *   - background     = scorecard_bg_top, cosi' le aree di padding sono colorate
 *                      come il resto dell'app (prima lo faceva il tema con
 *                      android:statusBarColor / android:navigationBarColor, che
 *                      edge-to-edge ignora).
 *
 * Non serve toccare le 27 Activity a mano.
 */
public class SystemBarsPaddingCallback implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        // Gradiente top -> bottom (scorecard_bg_top -> scorecard_bg_bottom, gia' definito
        // in drawable/bg_scoreboard.xml): la fascia sotto la status bar prende il colore
        // top, quella sotto la nav bar il colore bottom, con transizione morbida in mezzo
        // (che di solito e' coperta dal layout dell'Activity).
        content.setBackgroundResource(R.drawable.bg_scoreboard);

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
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
