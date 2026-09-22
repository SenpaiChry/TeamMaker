package com.teammaker.app.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.teammaker.app.domain.LiveMatchTimer;
import com.teammaker.app.domain.MatchPhases;
import com.teammaker.app.data.repository.MatchRepository;
import com.teammaker.app.data.repository.TeamRepository;
import com.teammaker.app.data.repository.TournamentRepository;

import java.util.ArrayList;
import com.teammaker.app.data.model.Match;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.R;

public class ScorecardActivity extends AppCompatActivity {

    // Dati logici: indice 1 = team A (keyTeam1), indice 2 = team B (keyTeam2).
    // Restano SEMPRE legati alla squadra, non al lato: lo swap cambia solo la visualizzazione,
    // quindi il salvataggio del risultato resta corretto.
    private int position = 0, points1 = 0, points2 = 0, sets1 = 0, sets2 = 0;
    private boolean isTournament = false;
    private boolean swapped = false;

    // Punti dei set completati, in ordine {puntiTeam1, puntiTeam2}. Costruisce il "detail" della partita.
    private final ArrayList<int[]> detail = new ArrayList<>();

    // Timer per scrivere il punteggio live su Firebase ogni 30 secondi
    private final android.os.Handler liveHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final int LIVE_INTERVAL_MS = 30_000;
    private final Runnable liveWriter = new Runnable() {
        @Override
        public void run() {
            writeLiveScore();
            liveHandler.postDelayed(this, LIVE_INTERVAL_MS);
        }
    };

    private String titleA = "", titleB = "";
    private final ArrayList<String> playersA = new ArrayList<>();
    private final ArrayList<String> playersB = new ArrayList<>();

    // View (lato 1 = card in alto, lato 2 = card in basso)
    private TextView txtPoints1, txtPoints2, txtSets1, txtSets2,
            txtTeam1, txtTeam2, txtTeam1Players, txtTeam2Players,
            badgeLive, txtStatusSubtitle;
    private LinearLayout dotsTeam1, dotsTeam2;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorecard);

        if (savedInstanceState != null) {
            points1 = savedInstanceState.getInt("points1", 0);
            points2 = savedInstanceState.getInt("points2", 0);
            sets1 = savedInstanceState.getInt("sets1", 0);
            sets2 = savedInstanceState.getInt("sets2", 0);
            swapped = savedInstanceState.getBoolean("swapped", false);

            @SuppressWarnings("unchecked")
            ArrayList<int[]> savedDetail = (ArrayList<int[]>) savedInstanceState.getSerializable("detail");
            if (savedDetail != null) {
                detail.clear();
                detail.addAll(savedDetail);
            }
        }

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
        txtStatusSubtitle = findViewById(R.id.txtStatusSubtitle);

        // ---- Costruzione dati squadre ----
        String matchType = "";
        String matchTime = "";
        if (getIntent().getExtras() != null && getIntent().hasExtra("tournament_key")) {
            position = getIntent().getIntExtra("position", 0);
            String tournamentKey = getIntent().getExtras().getString("tournament_key", "null");
            Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);
            if (tournament != null && position < tournament.matches.size()) {
                isTournament = true;
                Match match = tournament.matches.get(position);
                matchType = match.type;
                matchTime = match.time;

                String key1 = match.keyTeam1;
                String key2 = match.keyTeam2;

                titleA = buildTitle(tournament, key1, R.string.team_1);
                titleB = buildTitle(tournament, key2, R.string.team_2);
                buildPlayers(key1, playersA);
                buildPlayers(key2, playersB);
            }
        }
        if (!isTournament) {
            titleA = getString(R.string.team_1);
            titleB = getString(R.string.team_2);
        }

        // ---- Tasti "-" ripetibili (tenendo premuto continua a scalare) ----
        View.OnClickListener minus1 = v -> changePoints(1, -1);
        View.OnClickListener minus2 = v -> changePoints(2, -1);
        findViewById(R.id.btnMinusPoint1).setOnTouchListener(new RepeatListener(minus1));
        findViewById(R.id.btnMinusPoint2).setOnTouchListener(new RepeatListener(minus2));

        // ---- Tap sulla card = +1 (pattern classico segnapunti) ----
        findViewById(R.id.cardTeam1).setOnClickListener(v -> changePoints(1, +1));
        findViewById(R.id.cardTeam2).setOnClickListener(v -> changePoints(2, +1));

        // ---- Set ----
        findViewById(R.id.btnPlusSet1).setOnClickListener(v -> changeSet(1, +1));
        findViewById(R.id.btnMinusSet1).setOnClickListener(v -> changeSet(1, -1));
        findViewById(R.id.btnPlusSet2).setOnClickListener(v -> changeSet(2, +1));
        findViewById(R.id.btnMinusSet2).setOnClickListener(v -> changeSet(2, -1));

        // ---- Swap dei due team ----
        findViewById(R.id.btnSwap).setOnClickListener(v -> {
            swapped = !swapped;
            renderAll();
            if (isTournament) writeLiveScore();
        });

        findViewById(R.id.btnGoBack).setOnClickListener(v -> finish());

        // ---- Subtitle (sotto il badge LIVE) ----
        setupStatusSubtitle(matchType, matchTime);

        // ---- Bottone destro (Next Match / Save) ----
        setupNextMatchButton();

        renderAll();

        // Se e' una partita del torneo, inizia a trasmettere il punteggio live
        if (isTournament) {
            LiveMatchTimer.registerOnDisconnect();
            writeLiveScore();
            liveHandler.postDelayed(liveWriter, LIVE_INTERVAL_MS);
        }
    }

    private void setupStatusSubtitle(String matchType, String matchTime) {
        if (!isTournament) {
            txtStatusSubtitle.setText(R.string.scorecard_base_status);
            return;
        }
        String phase = MatchPhases.label(this, matchType);
        String time = matchTime != null ? matchTime : "";
        String subtitle;
        if (phase.isEmpty()) {
            subtitle = time;
        } else if (time.isEmpty()) {
            subtitle = phase;
        } else {
            subtitle = phase + " · " + time;
        }
        txtStatusSubtitle.setText(subtitle);
    }

    private void setupNextMatchButton() {
        Button btnNextMatch = findViewById(R.id.btnNextMatch);
        if (!isTournament) {
            btnNextMatch.setVisibility(View.GONE);
            return;
        }
        String tournamentKey = getIntent().getExtras().getString("tournament_key", "null");
        Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);
        if (tournament == null) {
            btnNextMatch.setVisibility(View.GONE);
            return;
        }
        boolean isLastMatch = position >= tournament.matches.size() - 1;
        btnNextMatch.setText(isLastMatch ? R.string.save : R.string.next_match);
        btnNextMatch.setVisibility(View.VISIBLE);
        btnNextMatch.setOnClickListener(v -> saveAndOpenActivity(tournament.key));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveHandler.removeCallbacks(liveWriter);
        if (isTournament) {
            LiveMatchTimer.clearLiveMatch();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("points1", points1);
        outState.putInt("points2", points2);
        outState.putInt("sets1", sets1);
        outState.putInt("sets2", sets2);
        outState.putBoolean("swapped", swapped);
        outState.putSerializable("detail", detail);
    }

    private void writeLiveScore() {
        if (!isTournament || getIntent().getExtras() == null) return;
        String tournamentKey = getIntent().getExtras().getString("tournament_key", "");
        LiveMatchTimer.writeLiveMatch(
                tournamentKey, position,
                titleA, titleB,
                String.join(", ", playersA),
                String.join(", ", playersB),
                points1, points2, sets1, sets2, swapped
        );
    }

    // Quale squadra logica (1=A, 2=B) e' mostrata su un lato fisico (1=sopra, 2=sotto)
    private int teamOnSide(int side) {
        if (side == 1) return swapped ? 2 : 1;
        return swapped ? 1 : 2;
    }

    private void changePoints(int side, int delta) {
        int team = teamOnSide(side);
        if (team == 1) {
            if (delta < 0 && points1 == 0) return;
            points1 += delta;
        } else {
            if (delta < 0 && points2 == 0) return;
            points2 += delta;
        }
        renderPoints();
        renderBadgeLive();
        if (isTournament) writeLiveScore();
    }

    private void changeSet(int side, int delta) {
        int team = teamOnSide(side);
        if (delta > 0) {
            if (points1 > 0 || points2 > 0) {
                detail.add(new int[]{points1, points2});
            }
            if (team == 1) sets1++; else sets2++;
            points1 = points2 = 0;
        } else {
            if (team == 1) {
                if (sets1 == 0) return;
                sets1--;
            } else {
                if (sets2 == 0) return;
                sets2--;
            }
            if (!detail.isEmpty()) {
                int[] last = detail.remove(detail.size() - 1);
                points1 = last[0];
                points2 = last[1];
            }
        }
        renderPoints();
        renderSets();
        renderBadgeLive();
        renderDots();
        if (isTournament) writeLiveScore();
    }

    // ---------------- Rendering ----------------

    private void renderAll() {
        renderPoints();
        renderSets();
        renderTeams();
        renderBadgeLive();
        renderDots();
    }

    private void renderPoints() {
        txtPoints1.setText(String.valueOf(teamOnSide(1) == 1 ? points1 : points2));
        txtPoints2.setText(String.valueOf(teamOnSide(2) == 1 ? points1 : points2));
    }

    private void renderSets() {
        txtSets1.setText(String.valueOf(teamOnSide(1) == 1 ? sets1 : sets2));
        txtSets2.setText(String.valueOf(teamOnSide(2) == 1 ? sets1 : sets2));
    }

    private void renderTeams() {
        int side1Team = teamOnSide(1);
        int side2Team = teamOnSide(2);

        txtTeam1.setText(side1Team == 1 ? titleA : titleB);
        txtTeam2.setText(side2Team == 1 ? titleA : titleB);
        txtTeam1Players.setText(joinPlayers(side1Team == 1 ? playersA : playersB));
        txtTeam2Players.setText(joinPlayers(side2Team == 1 ? playersA : playersB));

        // Il colore segue la squadra, non il lato: dopo lo swap si sposta con lei
        int colA = ContextCompat.getColor(this, R.color.scorecard_team_a);
        int colB = ContextCompat.getColor(this, R.color.scorecard_team_b);
        txtTeam1.setTextColor(side1Team == 1 ? colA : colB);
        txtSets1.setTextColor(side1Team == 1 ? colA : colB);
        txtTeam2.setTextColor(side2Team == 1 ? colA : colB);
        txtSets2.setTextColor(side2Team == 1 ? colA : colB);
    }

    /** Badge "● LIVE · SET N" con N = set corrente (sets1 + sets2 + 1). */
    private void renderBadgeLive() {
        int currentSet = sets1 + sets2 + 1;
        badgeLive.setText("● LIVE · SET " + currentSet);
    }

    /**
     * Pallini indicatori dei set vinti in cima ad ogni card. Numero totale di dot
     * dinamico: max(3, max(sets1, sets2) * 2 - 1) — cresce se qualcuno arriva a 3.
     */
    private void renderDots() {
        int totalDots = Math.max(3, Math.max(sets1, sets2) * 2 - 1);

        int side1Team = teamOnSide(1);
        int side2Team = teamOnSide(2);
        int wins1 = side1Team == 1 ? sets1 : sets2;
        int wins2 = side2Team == 1 ? sets1 : sets2;

        buildDots(dotsTeam1, totalDots, wins1, side1Team == 1 ? R.drawable.bg_dot_a : R.drawable.bg_dot_b);
        buildDots(dotsTeam2, totalDots, wins2, side2Team == 1 ? R.drawable.bg_dot_a : R.drawable.bg_dot_b);
    }

    private void buildDots(LinearLayout container, int total, int filled, int filledDrawable) {
        container.removeAllViews();
        int size = dpToPx(8);
        int margin = dpToPx(3);
        for (int i = 0; i < total; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.leftMargin = margin;
            lp.rightMargin = margin;
            lp.gravity = Gravity.CENTER_VERTICAL;
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i < filled ? filledDrawable : R.drawable.bg_dot_empty);
            container.addView(dot);
        }
    }

    private String joinPlayers(ArrayList<String> players) {
        if (players == null || players.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(" · ");
            sb.append(players.get(i));
        }
        return sb.toString();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ---------------- Helper dati ----------------

    private String buildTitle(Tournament tournament, String keyTeam, int defaultRes) {
        if (keyTeam == null || keyTeam.equals("TO DO")) return getString(defaultRes);
        return tournament.toStringNTeamByKey(keyTeam);
    }

    private void buildPlayers(String keyTeam, ArrayList<String> out) {
        out.clear();
        if (keyTeam == null || keyTeam.equals("TO DO")) return;
        ArrayList<Player> players = TeamRepository.getTeamByKey(keyTeam).players;
        for (Player p : players) out.add(p.getNameAndSurname(20));
    }

    private void saveAndOpenActivity(String tournamentKey) {
        Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);

        if (points1 > 0 || points2 > 0) {
            detail.add(new int[]{points1, points2});
            if (points1 > points2) sets1++;
            else if (points2 > points1) sets2++;
            points1 = points2 = 0;
        }

        Match match = new Match(tournament.matches.get(position).keyTeam1, tournament.matches.get(position).keyTeam2,
                tournament.matches.get(position).day, tournament.matches.get(position).time, sets1, sets2, tournament.matches.get(position).type);
        match.detail = new ArrayList<>(detail);

        match.key = tournament.matches.get(position).key;
        MatchRepository.editMatch(tournamentKey, match);

        Class<?> nextActivity = (position < tournament.matches.size() - 1) ?
                NextMatchActivity.class : ManageMatchesActivity.class;

        Intent intent = new Intent(getApplicationContext(), nextActivity);
        if (nextActivity == NextMatchActivity.class) {
            intent.putExtra("position", position);
        }
        intent.putExtra("tournament_key", tournamentKey);
        startActivity(intent);
        finish();
    }

    /**
     * Listener che esegue l'azione una volta al tocco e poi la ripete a intervalli
     * regolari finche' il tasto resta premuto (rilasciando si ferma).
     */
    private static class RepeatListener implements View.OnTouchListener {
        private static final int INITIAL_DELAY = 400;
        private static final int REPEAT_INTERVAL = 80;

        private final Handler handler = new Handler(Looper.getMainLooper());
        private final View.OnClickListener action;
        private View downView;

        private final Runnable repeater = new Runnable() {
            @Override
            public void run() {
                if (downView != null) {
                    handler.postDelayed(this, REPEAT_INTERVAL);
                    action.onClick(downView);
                }
            }
        };

        RepeatListener(View.OnClickListener action) {
            this.action = action;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downView = v;
                    v.setPressed(true);
                    action.onClick(v);
                    handler.removeCallbacks(repeater);
                    handler.postDelayed(repeater, INITIAL_DELAY);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(repeater);
                    v.setPressed(false);
                    downView = null;
                    return true;
            }
            return false;
        }
    }
}
