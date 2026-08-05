package com.example.myapplication.TopToast;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.FrameLayout;

import com.example.myapplication.R;

public class TopToast {

    public enum MessageType {
        SUCCESS, DANGER, INFO, WARNING
    }

    public static void show(Activity activity, String title, String message, MessageType type) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.top_toast, null);

        TextView titleView = layout.findViewById(R.id.toast_title);
        TextView messageView = layout.findViewById(R.id.toast_message);
        View container = layout.findViewById(R.id.custom_toast_container);

        titleView.setText(title);
        messageView.setText(message);

        int bgColor;
        switch (type) {
            case SUCCESS:
                bgColor = Color.parseColor("#4CAF50");
                break;
            case DANGER:
                bgColor = Color.parseColor("#F44336");
                break;
            case INFO:
                bgColor = Color.parseColor("#878681");
                break;
            case WARNING:
                bgColor = Color.parseColor("#FFC107");
                break;
            default:
                bgColor = Color.DKGRAY;
        }

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(200f); // bordi curvi
        background.setColor(bgColor);
        container.setBackground(background);


        // Inserisce la vista nel layout principale
        FrameLayout rootLayout = activity.findViewById(android.R.id.content);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP;

        int marginPx = (int) (30 * activity.getResources().getDisplayMetrics().density); // 30dp
        params.setMargins(marginPx, marginPx, marginPx, 0);
        rootLayout.addView(layout, params);

        // Rimuove dopo 2,5 secondi
        new Handler().postDelayed(() -> {
            rootLayout.removeView(layout);
        }, 2500);
    }
}
