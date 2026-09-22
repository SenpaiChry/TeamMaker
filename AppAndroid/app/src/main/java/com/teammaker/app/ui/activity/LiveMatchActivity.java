package com.teammaker.app.ui.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.teammaker.app.domain.LiveMatchTimer;
import com.google.firebase.database.DataSnapshot;
import com.teammaker.app.R;

import java.util.ArrayList;

public class LiveMatchActivity extends AppCompatActivity {

    private TextView txtPoints1, txtPoints2, txtSets1, txtSets2,
            txtTeam1, txtTeam2, txtTeam1Players, txtTeam2Players,
            badgeLive;
    private LinearLayout dotsTeam1, dotsTeam2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorecard);

        txtPoints1 = findViewById(R.id.txtPoints1);
        txtPoints2 = findViewById(R.id.txtPoints2);
        txtSets1 = findViewById(R.id.txtSets1);
        txtSets2 = findViewById(R.id.txtSets2);
        txtTeam1 = findViewById(R.id.txtTeam1);
        txtTeam2 = findViewById(R.id.txtTeam2);
        txtTeam1Players = findViewById(R.id.txtTeam1Players);
        txtTeam2Players = findViewById(R.id.txtTeam2Players);
        dotsTeam1 = findViewById(R.id.dotsTeam1);
        dotsTeam2 = findViewById(R.id.dotsTeam2);
        badgeLive = findViewById(R.id.badgeLive);

        hide(R.id.btnMinusPoint1, R.id.btnMinusPoint2,
                R.id.btnPlusSet1, R.id.btnMinusSet1,
                R.id.btnPlusSet2, R.id.btnMinusSet2,
                R.id.btnSwap, R.id.btnNextMatch);

        findViewById(R.id.cardTeam1).setClickable(false);
        findViewById(R.id.cardTeam2).setClickable(false);

        TextView txtStatusSubtitle = findViewById(R.id.txtStatusSubtitle);
        txtStatusSubtitle.setVisibility(View.GONE);

        ObjectAnimator pulse = ObjectAnimator.ofFloat(badgeLive, "alpha", 1f, 0.3f);
        pulse.setDuration(800);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();

        showNoMatch();

        LiveMatchTimer.startListening((active, snapshot) -> {
            if (!active) {
                showNoMatch();
                return;
            }
            showMatch(snapshot);
        });

        findViewById(R.id.btnGoBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveMatchTimer.stopListening();
    }

    private void showNoMatch() {
        txtTeam1.setText(R.string.no_live_match);
        txtTeam2.setText("");
        txtTeam1Players.setText("");
        txtTeam2Players.setText("");
        txtPoints1.setText("-");
        txtPoints2.setText("-");
        txtSets1.setText("-");
        txtSets2.setText("-");
        badgeLive.setVisibility(View.GONE);
        dotsTeam1.removeAllViews();
        dotsTeam2.removeAllViews();
    }

    private void showMatch(DataSnapshot snapshot) {
        badgeLive.setVisibility(View.VISIBLE);
        int currentSet = getIntValue(snapshot, "sets1") + getIntValue(snapshot, "sets2") + 1;
        badgeLive.setText("● LIVE · SET " + currentSet);

        boolean swapped = getBoolValue(snapshot, "swapped");

        String nameA = getStringValue(snapshot, "team1_name");
        String nameB = getStringValue(snapshot, "team2_name");
        String playersA = getStringValue(snapshot, "team1_players");
        String playersB = getStringValue(snapshot, "team2_players");
        int ptsA = getIntValue(snapshot, "points1");
        int ptsB = getIntValue(snapshot, "points2");
        int setsA = getIntValue(snapshot, "sets1");
        int setsB = getIntValue(snapshot, "sets2");
        ArrayList<Integer> winners = LiveMatchTimer.decodeSetWinners(
                getStringValue(snapshot, "set_winners"));
        if (winners.isEmpty() && (setsA + setsB) > 0) {
            for (int i = 0; i < setsA; i++) winners.add(1);
            for (int i = 0; i < setsB; i++) winners.add(2);
        }

        int side1Team = swapped ? 2 : 1;
        int side2Team = swapped ? 1 : 2;

        txtTeam1.setText(side1Team == 1 ? nameA : nameB);
        txtTeam2.setText(side2Team == 1 ? nameA : nameB);
        txtTeam1Players.setText(side1Team == 1 ? playersA : playersB);
        txtTeam2Players.setText(side2Team == 1 ? playersA : playersB);
        txtPoints1.setText(String.valueOf(side1Team == 1 ? ptsA : ptsB));
        txtPoints2.setText(String.valueOf(side2Team == 1 ? ptsA : ptsB));
        txtSets1.setText(String.valueOf(side1Team == 1 ? setsA : setsB));
        txtSets2.setText(String.valueOf(side2Team == 1 ? setsA : setsB));

        int colA = ContextCompat.getColor(this, R.color.scorecard_team_a);
        int colB = ContextCompat.getColor(this, R.color.scorecard_team_b);
        txtTeam1.setTextColor(side1Team == 1 ? colA : colB);
        txtSets1.setTextColor(side1Team == 1 ? colA : colB);
        txtTeam2.setTextColor(side2Team == 1 ? colA : colB);
        txtSets2.setTextColor(side2Team == 1 ? colA : colB);

        buildDots(dotsTeam1, side1Team, winners);
        buildDots(dotsTeam2, side2Team, winners);
    }

    private void buildDots(LinearLayout container, int forTeam, ArrayList<Integer> winners) {
        container.removeAllViews();
        int size = dpToPx(8);
        int margin = dpToPx(3);
        for (int i = 0; i < winners.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.leftMargin = margin;
            lp.rightMargin = margin;
            lp.gravity = Gravity.CENTER_VERTICAL;
            dot.setLayoutParams(lp);
            int winner = winners.get(i);
            dot.setBackgroundResource(winner == forTeam
                    ? (forTeam == 1 ? R.drawable.bg_dot_a : R.drawable.bg_dot_b)
                    : R.drawable.bg_dot_empty);
            container.addView(dot);
        }
    }

    private void hide(int... ids) {
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) v.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private boolean getBoolValue(DataSnapshot s, String key) {
        Boolean v = s.child(key).getValue(Boolean.class);
        return v != null && v;
    }

    private String getStringValue(DataSnapshot s, String key) {
        String v = s.child(key).getValue(String.class);
        return v != null ? v : "";
    }

    private int getIntValue(DataSnapshot s, String key) {
        Integer v = s.child(key).getValue(Integer.class);
        return v != null ? v : 0;
    }
}
