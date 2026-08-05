package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.LiveMatchUtility;
import com.google.firebase.database.DataSnapshot;

/**
 * Schermata di sola lettura che mostra la partita in corso in tempo reale.
 * Non ha tasti per modificare il punteggio: si aggiorna automaticamente
 * quando il segnapunti scrive su Firebase.
 */
public class ActivityLiveMatch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_match);

        TextView txtTeam1Name = findViewById(R.id.txtTeam1Name);
        TextView txtTeam2Name = findViewById(R.id.txtTeam2Name);
        TextView txtTeam1Players = findViewById(R.id.txtTeam1Players);
        TextView txtTeam2Players = findViewById(R.id.txtTeam2Players);
        TextView txtPoints1 = findViewById(R.id.txtPoints1);
        TextView txtPoints2 = findViewById(R.id.txtPoints2);
        TextView txtSets1 = findViewById(R.id.txtSets1);
        TextView txtSets2 = findViewById(R.id.txtSets2);
        View panelPoints1 = findViewById(R.id.panelPoints1);
        View panelPoints2 = findViewById(R.id.panelPoints2);

        // Pulsazione del badge LIVE
        TextView liveIndicator = findViewById(R.id.txtLiveIndicator);
        ObjectAnimator pulse = ObjectAnimator.ofFloat(liveIndicator, "alpha", 1f, 0.3f);
        pulse.setDuration(800);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();

        // Colora i pannelli
        applyPanelColor(panelPoints1, ContextCompat.getColor(this, R.color.scorecard_team_a));
        applyPanelColor(panelPoints2, ContextCompat.getColor(this, R.color.scorecard_team_b));

        // Ascolta il nodo live in tempo reale
        LiveMatchUtility.startListening((active, snapshot) -> {
            if (!active) {
                txtTeam1Name.setText(R.string.no_live_match);
                txtTeam2Name.setText("");
                txtTeam1Players.setText("");
                txtTeam2Players.setText("");
                txtPoints1.setText("-");
                txtPoints2.setText("-");
                txtSets1.setText("-");
                txtSets2.setText("-");
                liveIndicator.setVisibility(View.GONE);
                return;
            }

            liveIndicator.setVisibility(View.VISIBLE);

            txtTeam1Name.setText(getStringValue(snapshot, "team1_name"));
            txtTeam2Name.setText(getStringValue(snapshot, "team2_name"));
            txtTeam1Players.setText(getStringValue(snapshot, "team1_players"));
            txtTeam2Players.setText(getStringValue(snapshot, "team2_players"));
            txtPoints1.setText(String.valueOf(getIntValue(snapshot, "points1")));
            txtPoints2.setText(String.valueOf(getIntValue(snapshot, "points2")));
            txtSets1.setText(String.valueOf(getIntValue(snapshot, "sets1")));
            txtSets2.setText(String.valueOf(getIntValue(snapshot, "sets2")));
        });

        findViewById(R.id.btnGoBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveMatchUtility.stopListening();
    }

    private void applyPanelColor(View panel, int color) {
        if (panel != null && panel.getBackground() instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) panel.getBackground().mutate();
            shape.setColor((color & 0x00FFFFFF) | 0x14000000);
            shape.setStroke(dpToPx(2), (color & 0x00FFFFFF) | 0x55000000);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String getStringValue(DataSnapshot s, String key) {
        return s.child(key).getValue(String.class) != null
                ? s.child(key).getValue(String.class) : "";
    }

    private int getIntValue(DataSnapshot s, String key) {
        return s.child(key).getValue(Integer.class) != null
                ? s.child(key).getValue(Integer.class) : 0;
    }
}