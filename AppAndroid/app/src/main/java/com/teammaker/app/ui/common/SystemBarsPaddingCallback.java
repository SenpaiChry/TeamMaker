package com.teammaker.app.ui.common;

import android.app.Activity;
import android.app.Application;
import android.content.res.TypedArray;
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
 * TeamMakerApplication, applica automaticamente a ogni Activity full-screen:
 *
 *   - padding top    = altezza status bar (dinamico: adatta a notch / dynamic island)
 *   - padding bottom = insets.bottom del sistema (gesture nav ~24dp, 3-button ~48dp)
 *
 * Il colore delle aree di padding lo mette il tema (windowBackground =
 * bg_scoreboard), cosi' c'e' UN solo gradient continuo dalla status bar
 * alla nav bar, senza stacchi.
 *
 * Non serve toccare le 27 Activity a mano.
 */
public class SystemBarsPaddingCallback implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // I popup usano @style/NoTitleDialog con windowIsFloating=true: dialog
        // centrati con background arrotondato proprio (bg_modal_dark). Nessun
        // padding di sistema da applicare. Skip.
        if (isFloatingWindow(activity)) return;

        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private static boolean isFloatingWindow(Activity activity) {
        TypedArray ta = activity.getTheme()
                .obtainStyledAttributes(new int[]{android.R.attr.windowIsFloating});
        try {
            return ta.getBoolean(0, false);
        } finally {
            ta.recycle();
        }
    }

    @Override public void onActivityStarted(@NonNull Activity activity) { }
    @Override public void onActivityResumed(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivityStopped(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
