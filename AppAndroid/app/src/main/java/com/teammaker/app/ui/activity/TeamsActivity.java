package com.teammaker.app.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.model.Constants;
import com.teammaker.app.domain.TeamGenerator;
import com.teammaker.app.ui.common.VerticalSpacingItemDecoration;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.ui.adapter.PlayerTeamsAdapter;
import com.teammaker.app.ui.popup.SaveNewTournamentPopupActivity;
import com.teammaker.app.R;

public class TeamsActivity extends AppCompatActivity {

    // WeakReference: se l'Activity viene distrutta il GC puo' liberarla.
    // Chi ne ha bisogno usa TeamsActivity.get() e controlla null.
    private static WeakReference<TeamsActivity> instance = new WeakReference<>(null);
    public static TeamsActivity get() { return instance.get(); }

    private TextView txtMaxDifference;
    private TextView txtN;
    private ProgressBar progressBar;
    private Button btnReroll;
    private PlayerTeamsAdapter playerTeamsAdapter;
    private int nPlayers;
    private int nAlgorithm;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams);

        instance = new WeakReference<>(this);

        nPlayers = getIntent().getExtras().getInt("nPlayers");
        nAlgorithm = getIntent().getExtras().getInt("nAlgorithm");
        String type = getIntent().getExtras().getString("TYPE");

        progressBar = findViewById(R.id.progressBar);
        txtMaxDifference = findViewById(R.id.txtMaxDifference);
        txtN = findViewById(R.id.txtN);
        Button btnSave = findViewById(R.id.btnSave);
        btnReroll = findViewById(R.id.btnReroll);

        RecyclerView listView = findViewById(R.id.listViewTeams);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.addItemDecoration(new VerticalSpacingItemDecoration(this, 8));
        playerTeamsAdapter = new PlayerTeamsAdapter(type);
        listView.setAdapter(playerTeamsAdapter);

        if (!"TOURNAMENT".equals(type)) {
            txtMaxDifference.setVisibility(View.GONE);
            txtN.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);
        }
        if (nAlgorithm == 2) {
            btnReroll.setVisibility(View.INVISIBLE);
        }

        refreshUI();

        btnReroll.setOnClickListener(view -> reroll());

        btnSave.setOnClickListener(view -> {
            Intent intent = new Intent(this, SaveNewTournamentPopupActivity.class);
            startActivity(intent);
        });

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(view -> finish());
    }

    private void reroll() {
        // Fotografa le squadre PRIMA della rigenerazione
        Set<Set<String>> previousTeams = snapshotTeams();

        progressBar.setVisibility(View.VISIBLE);
        btnReroll.setEnabled(false);
        Toast.makeText(this, R.string.generating, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Constants.inputMaxDifference = 0;
                TeamGenerator.makeTeams(nPlayers, nAlgorithm, Constants.playersSelected);

                // Se le squadre sono identiche a prima, forza uno scambio casuale
                if (snapshotTeams().equals(previousTeams) && Constants.teams.size() >= 2) {
                    forceRandomSwap();
                    Log.d("TeamsActivity", "Squadre identiche -> forzato swap casuale");
                }
            } catch (Exception e) {
                Log.e("TeamsActivity", "Errore durante la generazione", e);
            }

            runOnUiThread(() -> {
                refreshUI();
                playerTeamsAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                btnReroll.setEnabled(true);
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void refreshUI() {
        float diff = calcMaxDiff();
        int cycle = Constants.nCycle;

        Log.d("TeamsActivity", "refreshUI -> diff=" + diff + " cycle=" + cycle);

        txtMaxDifference.setText(getString(R.string.max_diff) + " " + diff);
        txtN.setText(getString(R.string.cycle_n) + " " + cycle);
    }

    /**
     * Crea una fotografia delle squadre: Set di Set di chiavi giocatore.
     * Indipendente dall'ordine delle squadre e dei giocatori.
     */
    private static Set<Set<String>> snapshotTeams() {
        Set<Set<String>> snapshot = new HashSet<>();
        for (Team team : Constants.teams) {
            Set<String> keys = new HashSet<>();
            for (Player p : team.players) {
                keys.add(p.key);
            }
            snapshot.add(keys);
        }
        return snapshot;
    }

    /**
     * Scambia due giocatori dello STESSO genere tra due squadre diverse: il
     * risultato cambia rispetto al precedente, ma i conteggi maschi/femmine
     * restano invariati (niente squadre tipo 2F vs 2M). Se non trova una coppia
     * stesso-genere da scambiare, non forza nulla (meglio invariato che sbilanciato).
     */
    private static void forceRandomSwap() {
        if (Constants.teams.size() < 2) return;
        Random rng = new Random();

        for (int attempt = 0; attempt < 100; attempt++) {
            int t1idx = rng.nextInt(Constants.teams.size());
            int t2idx = rng.nextInt(Constants.teams.size());
            if (t1idx == t2idx) continue;

            Team team1 = Constants.teams.get(t1idx);
            Team team2 = Constants.teams.get(t2idx);
            if (team1.players.isEmpty() || team2.players.isEmpty()) continue;

            int p1 = rng.nextInt(team1.players.size());
            int p2 = rng.nextInt(team2.players.size());

            Player a = team1.players.get(p1);
            Player b = team2.players.get(p2);
            if (a.gender == null || !a.gender.equalsIgnoreCase(b.gender)) continue; // solo stesso genere

            team1.players.set(p1, b);
            team2.players.set(p2, a);
            return;
        }
    }

    private float calcMaxDiff() {
        if (Constants.teams == null || Constants.teams.isEmpty()) return 0;

        float minValue = Float.MAX_VALUE, maxValue = -Float.MAX_VALUE;
        for (Team team : Constants.teams) {
            float vote = team.getTotalVote();
            if (vote < minValue) minValue = vote;
            if (vote > maxValue) maxValue = vote;
        }
        return maxValue - minValue;
    }
}