package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.LiveMatchUtility;
import com.example.myapplication.Utility.MatchUtility;
import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;

public class ActivityScorecard extends AppCompatActivity {

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

    // Riferimenti alle view fisiche (lato 1 = sx/sopra, lato 2 = dx/sotto)
    private TextView txtPoints1, txtPoints2, txtSets1, txtSets2, txtTeam1, txtTeam2;
    private LinearLayout llTeam1, llTeam2, llTeam1Player34, llTeam2Player34;
    private View panelPoints1, panelPoints2;

    private final int[] side1PlayerIds = {R.id.txtTeam1Player1Name, R.id.txtTeam1Player2Name,
            R.id.txtTeam1Player3Name, R.id.txtTeam1Player4Name, R.id.txtTeam1Player5Name};
    private final int[] side2PlayerIds = {R.id.txtTeam2Player1Name, R.id.txtTeam2Player2Name,
            R.id.txtTeam2Player3Name, R.id.txtTeam2Player4Name, R.id.txtTeam2Player5Name};

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
        llTeam1 = findViewById(R.id.llTeam1);
        llTeam2 = findViewById(R.id.llTeam2);
        llTeam1Player34 = findViewById(R.id.llTeam1Player34);
        llTeam2Player34 = findViewById(R.id.llTeam2Player34);
        panelPoints1 = findViewById(R.id.panelPoints1);
        panelPoints2 = findViewById(R.id.panelPoints2);

        // ---- Costruzione dati squadre ----
        if (getIntent().getExtras() != null) {
            position = getIntent().getIntExtra("position", 0);
            String tournamentKey = getIntent().getExtras().getString("tournament_key", "null");
            Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
            isTournament = true;

            String key1 = tournament.matches.get(position).keyTeam1;
            String key2 = tournament.matches.get(position).keyTeam2;
            Log.d("FATAL", "key " + tournamentKey + " position " + position + " k1 " + key1 + " k2 " + key2);

            titleA = buildTitle(tournament, key1, R.string.team_1);
            titleB = buildTitle(tournament, key2, R.string.team_2);
            buildPlayers(key1, playersA);
            buildPlayers(key2, playersB);
        } else {
            titleA = getString(R.string.team_1);
            titleB = getString(R.string.team_2);
        }

        // ---- Tasti punti: click = +1, tenuto premuto = ripete finche' non rilasci ----
        View.OnClickListener plus1 = v -> changePoints(1, +1);
        View.OnClickListener minus1 = v -> changePoints(1, -1);
        View.OnClickListener plus2 = v -> changePoints(2, +1);
        View.OnClickListener minus2 = v -> changePoints(2, -1);

        findViewById(R.id.btnPlusPoint1).setOnTouchListener(new RepeatListener(plus1));
        findViewById(R.id.btnMinusPoint1).setOnTouchListener(new RepeatListener(minus1));
        findViewById(R.id.btnPlusPoint2).setOnTouchListener(new RepeatListener(plus2));
        findViewById(R.id.btnMinusPoint2).setOnTouchListener(new RepeatListener(minus2));

        // ---- Tap direttamente sul numero = +1 ----
        txtPoints1.setOnClickListener(v -> changePoints(1, +1));
        txtPoints2.setOnClickListener(v -> changePoints(2, +1));

        // ---- Set ----
        findViewById(R.id.btnPlusSet1).setOnClickListener(v -> changeSet(1, +1));
        findViewById(R.id.btnMinusSet1).setOnClickListener(v -> changeSet(1, -1));
        findViewById(R.id.btnPlusSet2).setOnClickListener(v -> changeSet(2, +1));
        findViewById(R.id.btnMinusSet2).setOnClickListener(v -> changeSet(2, -1));

        // ---- Swap dei due team (sx/dx e sopra/sotto) ----
        findViewById(R.id.btnSwap).setOnClickListener(v -> {
            swapped = !swapped;
            renderAll();
            if (isTournament) writeLiveScore();
        });

        findViewById(R.id.btnGoBack).setOnClickListener(v -> finish());

        // ---- Tasto rotazione ----
        findViewById(R.id.btnRotate).setOnClickListener(v -> {
            int orientation = getResources().getConfiguration().orientation;
            setRequestedOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE
                    ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        });

        // ---- Prossima partita / marquee ----
        Button btnNextMatch = findViewById(R.id.btnNextMatch);
        TextView txtNextMatchMarquee = findViewById(R.id.txtNextMatchMarquee);

        if (isTournament) {
            String tournamentKey = getIntent().getExtras().getString("tournament_key", "null");
            Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

            boolean isLastMatch = position >= tournament.matches.size() - 1;

            if (isLastMatch) {
                // Ultima partita: salva e torna alla gestione
                btnNextMatch.setText(R.string.save);
                btnNextMatch.setVisibility(View.VISIBLE);
                btnNextMatch.setOnClickListener(v -> saveAndOpenActivity(tournament.key));
                txtNextMatchMarquee.setVisibility(View.GONE);
            } else {
                // Ci sono altre partite: mostra "prossima partita" e il marquee
                btnNextMatch.setText(R.string.next_match);
                btnNextMatch.setVisibility(View.VISIBLE);
                btnNextMatch.setOnClickListener(v -> saveAndOpenActivity(tournament.key));
                txtNextMatchMarquee.setText(tournament.matches.get(position + 1).toStringForNextMatch(this));
                txtNextMatchMarquee.setSelected(true);
            }
        } else {
            // Segnapunti base senza squadre: niente tasto e niente marquee
            btnNextMatch.setVisibility(View.GONE);
            txtNextMatchMarquee.setVisibility(View.GONE);
        }

        renderAll();

        // Se e' una partita del torneo, inizia a trasmettere il punteggio live
        if (isTournament) {
            LiveMatchUtility.registerOnDisconnect(); // pulizia automatica in caso di crash/kill
            writeLiveScore(); // scrivi subito
            liveHandler.postDelayed(liveWriter, LIVE_INTERVAL_MS); // poi ogni 30s
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveHandler.removeCallbacks(liveWriter);
        if (isTournament) {
            LiveMatchUtility.clearLiveMatch();
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

    /** Scrive il punteggio attuale su Firebase per la visualizzazione live. */
    private void writeLiveScore() {
        if (!isTournament || getIntent().getExtras() == null) return;
        String tournamentKey = getIntent().getExtras().getString("tournament_key", "");
        LiveMatchUtility.writeLiveMatch(
                tournamentKey, position,
                titleA, titleB,
                String.join(", ", playersA),
                String.join(", ", playersB),
                points1, points2, sets1, sets2, swapped
        );
    }

    // Quale squadra logica (1=A, 2=B) e' mostrata su un lato fisico (1=sx/sopra, 2=dx/sotto)
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
        if (isTournament) writeLiveScore();
    }

    private void changeSet(int side, int delta) {
        int team = teamOnSide(side);
        if (delta > 0) {
            // Set vinto: registra i punti del set corrente (se giocato) e riparte da 0
            if (points1 > 0 || points2 > 0) {
                detail.add(new int[]{points1, points2});
            }
            if (team == 1) sets1++; else sets2++;
            points1 = points2 = 0;
        } else {
            // Annulla l'ultimo set: ripristina i punti registrati
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
        if (isTournament) writeLiveScore();
    }

    // ---------------- Rendering ----------------

    private void renderAll() {
        renderPoints();
        renderSets();
        renderTitlesAndPlayers();
    }

    private void renderPoints() {
        txtPoints1.setText(String.valueOf(teamOnSide(1) == 1 ? points1 : points2));
        txtPoints2.setText(String.valueOf(teamOnSide(2) == 1 ? points1 : points2));
    }

    private void renderSets() {
        txtSets1.setText(String.valueOf(teamOnSide(1) == 1 ? sets1 : sets2));
        txtSets2.setText(String.valueOf(teamOnSide(2) == 1 ? sets1 : sets2));
    }

    private void renderTitlesAndPlayers() {
        fillSide(1, teamOnSide(1));
        fillSide(2, teamOnSide(2));
    }

    private void fillSide(int side, int team) {
        TextView title = side == 1 ? txtTeam1 : txtTeam2;
        LinearLayout llTeam = side == 1 ? llTeam1 : llTeam2;
        LinearLayout ll34 = side == 1 ? llTeam1Player34 : llTeam2Player34;
        int[] ids = side == 1 ? side1PlayerIds : side2PlayerIds;
        ArrayList<String> players = team == 1 ? playersA : playersB;

        title.setText(team == 1 ? titleA : titleB);

        // Il colore segue la squadra, non il lato: dopo lo swap si sposta con lei
        applySideColors(side, team);

        if (players.isEmpty()) {
            llTeam.setVisibility(View.GONE);
            return;
        }

        llTeam.setVisibility(View.VISIBLE);
        // reset: 2 giocatori = solo prima riga; nascondo il resto
        ll34.setVisibility(View.GONE);
        findViewById(ids[1]).setVisibility(View.GONE);
        findViewById(ids[4]).setVisibility(View.GONE);

        for (int i = 0; i < players.size() && i < ids.length; i++) {
            TextView playerView = findViewById(ids[i]);
            playerView.setText(players.get(i));
            playerView.setVisibility(View.VISIBLE);
            if (i == 2) ll34.setVisibility(View.VISIBLE);
            if (i == 3) findViewById(ids[3]).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Colora titolo, punteggio, set e pannello del lato indicato con il colore della squadra
     * che vi e' attualmente mostrata.
     */
    private void applySideColors(int side, int team) {
        int color = ContextCompat.getColor(this,
                team == 1 ? R.color.scorecard_team_a : R.color.scorecard_team_b);

        TextView title = side == 1 ? txtTeam1 : txtTeam2;
        TextView points = side == 1 ? txtPoints1 : txtPoints2;
        TextView sets = side == 1 ? txtSets1 : txtSets2;
        View panel = side == 1 ? panelPoints1 : panelPoints2;

        title.setTextColor(color);
        points.setTextColor(color);
        sets.setTextColor(color);

        if (panel != null && panel.getBackground() instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) panel.getBackground().mutate();
            shape.setColor(withAlpha(color, 0x14));                        // riempimento velato
            shape.setStroke(dpToPx(2), withAlpha(color, 0x55));            // bordo piu' marcato
        }
    }

    private int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
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
        ArrayList<Player> players = TournamentTeamUtility.getTeamByKey(keyTeam).players;
        for (Player p : players) out.add(p.getNameAndSurname(20));
    }

    private void saveAndOpenActivity(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        // Finalizza l'eventuale set in corso non ancora confermato (es. partita a set unico)
        if (points1 > 0 || points2 > 0) {
            detail.add(new int[]{points1, points2});
            if (points1 > points2) sets1++;
            else if (points2 > points1) sets2++;
            points1 = points2 = 0;
        }

        // Nota: points1/sets1 sono SEMPRE del team keyTeam1, points2/sets2 di keyTeam2,
        // indipendentemente dallo swap visivo -> il salvataggio resta corretto.
        // points1/points2 salvati = set vinti (headline); detail = punti dei set.
        Match match = new Match(tournament.matches.get(position).keyTeam1, tournament.matches.get(position).keyTeam2,
                tournament.matches.get(position).day, tournament.matches.get(position).time, sets1, sets2, tournament.matches.get(position).type);
        match.detail = new ArrayList<>(detail);

        match.key = tournament.matches.get(position).key;
        MatchUtility.editMatch(tournamentKey, match);

        Class<?> nextActivity = (position < tournament.matches.size() - 1) ?
                ActivityNextMatch.class : TournamentActivityManageMatches.class;

        Intent intent = new Intent(getApplicationContext(), nextActivity);
        if (nextActivity == ActivityNextMatch.class) {
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
        private static final int INITIAL_DELAY = 400; // ms prima di iniziare a ripetere
        private static final int REPEAT_INTERVAL = 80; // ms tra una ripetizione e l'altra

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
                    action.onClick(v);                     // primo incremento immediato
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