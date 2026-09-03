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

            boolean swapped = getBoolValue(snapshot, "swapped");

            String[] names = { getStringValue(snapshot, "team1_name"), getStringValue(snapshot, "team2_name") };
            String[] players = { getStringValue(snapshot, "team1_players"), getStringValue(snapshot, "team2_players") };
            int[] points = { getIntValue(snapshot, "points1"), getIntValue(snapshot, "points2") };
            int[] sets = { getIntValue(snapshot, "sets1"), getIntValue(snapshot, "sets2") };
            int[] colors = { ContextCompat.getColor(this, R.color.scorecard_team_a),
                    ContextCompat.getColor(this, R.color.scorecard_team_b) };

            // Lato fisico 1 (sopra) = squadra A, oppure B dopo il cambio campo
            int side1Team = swapped ? 1 : 0;
            int side2Team = swapped ? 0 : 1;

            fillSide(side1Team, names, players, points, sets, colors,
                    txtTeam1Name, txtTeam1Players, txtPoints1, txtSets1, panelPoints1);
            fillSide(side2Team, names, players, points, sets, colors,
                    txtTeam2Name, txtTeam2Players, txtPoints2, txtSets2, panelPoints2);
        });

        findViewById(R.id.btnGoBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveMatchUtility.stopListening();
    }

    /** Riempie un lato fisico con i dati della squadra indicata (0=A, 1=B): il colore la segue. */
    private void fillSide(int team, String[] names, String[] players, int[] points, int[] sets, int[] colors,
                          TextView name, TextView playersTv, TextView pointsTv, TextView setsTv, View panel) {
        name.setText(names[team]);
        playersTv.setText(players[team]);
        pointsTv.setText(String.valueOf(points[team]));
        setsTv.setText(String.valueOf(sets[team]));

        int color = colors[team];
        name.setTextColor(color);
        pointsTv.setTextColor(color);
        setsTv.setTextColor(color);
        applyPanelColor(panel, color);
    }

    private boolean getBoolValue(DataSnapshot s, String key) {
        Boolean v = s.child(key).getValue(Boolean.class);
        return v != null && v;
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