package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.MatchUtility;
import com.example.myapplication.Utility.TimeUtility;
import com.example.myapplication.Utility.TournamentScheduleUtility;
import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;
import java.util.Arrays;

public class TournamentActivityEditMatch extends AppCompatActivity {

    ArrayList<String> stringTeams = new ArrayList<>();
    ArrayList<String> stringPhases = new ArrayList<>();

    private LinearLayout llSets;
    private TextView txtSetCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_edit_match);

        // La modale si adatta al contenuto; lo scroll interno ha un tetto (MaxHeightScrollView)
        // così con tanti set scrolla e i tasti restano visibili, senza vuoti con pochi set.
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        MaxHeightScrollView scrollContent = findViewById(R.id.scrollContent);
        float density = getResources().getDisplayMetrics().density;
        int chromeHeight = (int) (210 * density); // spazio per titolo + tasti + margini
        scrollContent.setMaxHeight(getResources().getDisplayMetrics().heightPixels - chromeHeight);

        int position = (int) getIntent().getExtras().get("position");
        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        ArrayList<String> subtitleTeams = new ArrayList<>();
        stringTeams.add("TO DO");
        subtitleTeams.add("");
        int teamNumber = 1;
        for (Team team : tournament.teams) {
            // Chiuso: "TEAM n"; nella tendina anche i giocatori (sottotitolo)
            stringTeams.add(getString(R.string.team).trim() + " " + teamNumber);
            subtitleTeams.add(team.toStringNameAndSurname());
            teamNumber++;
        }

        stringPhases = new ArrayList<>(Arrays.asList(" --- ",
                getString(R.string.finalString),
                getString(R.string.finalina),
                getString(R.string.semifinal),
                getString(R.string.quarter)
        ));

        if (tournament.nBracket == 1) {
            stringPhases.add(getString(R.string.bracketSpace));
        } else if (tournament.nBracket > 1) {
            for (int i = 0; i < tournament.nBracket; i ++) {
                stringPhases.add(TournamentScheduleUtility.getBracketLabel(i));
            }
        }

        TextView txtTitle = findViewById(R.id.txtTitle);
        EditText txtDay = findViewById(R.id.txtDay);
        EditText txtTime = findViewById(R.id.txtTime);

        llSets = findViewById(R.id.llSets);
        txtSetCount = findViewById(R.id.txtSetCount);
        Button btnAddSet = findViewById(R.id.btnAddSet);
        btnAddSet.setOnClickListener(v -> addSetRow(-1, -1));

        Spinner spinnerTeam1 = findViewById(R.id.spinnerTeam1);
        Spinner spinnerTeam2 = findViewById(R.id.spinnerTeam2);
        TournamentSpinnerTeamAdapter adapterTeam1 = new TournamentSpinnerTeamAdapter(this, stringTeams, subtitleTeams);
        TournamentSpinnerTeamAdapter adapterTeam2 = new TournamentSpinnerTeamAdapter(this, stringTeams, subtitleTeams);
        spinnerTeam1.setAdapter(adapterTeam1);
        spinnerTeam2.setAdapter(adapterTeam2);
        adapterTeam1.setSpinner(spinnerTeam1);
        adapterTeam2.setSpinner(spinnerTeam2);

        Spinner spinnerPhase = findViewById(R.id.spinnerPhase);
        TournamentSpinnerTeamAdapter adapterPhase = new TournamentSpinnerTeamAdapter(this, stringPhases);
        spinnerPhase.setAdapter(adapterPhase);
        adapterPhase.setSpinner(spinnerPhase);

        Button btnConfirm = findViewById(R.id.btnConfirm);

        if (position < 0) { // NEW
            txtTitle.setText(this.getText(R.string.new_match));
            findViewById(R.id.llResult).setVisibility(View.GONE);

            btnConfirm.setOnClickListener(view -> {
                if (TimeUtility.isValidTime(String.valueOf(txtTime.getText()))
                        && spinnerTeam1.getSelectedItemPosition() != spinnerTeam2.getSelectedItemPosition()
                        && spinnerPhase.getSelectedItemPosition() > 0) {
                    Match match = new Match(tournament.getTeamByIndex(spinnerTeam1.getSelectedItemPosition()),
                            tournament.getTeamByIndex(spinnerTeam2.getSelectedItemPosition()),
                            Integer.parseInt(String.valueOf(txtDay.getText())),
                            String.valueOf(txtTime.getText()),
                            0 , 0,
                            stringPhases.get(spinnerPhase.getSelectedItemPosition()));

                    MatchUtility.addNewMatch(tournamentKey, match);
                    finish();
                }
            });
        } else { // EDIT
            txtTitle.setText(this.getText(R.string.edit_match));
            Match existing = tournament.matches.get(position);
            txtDay.setText(String.valueOf(existing.day));
            txtTime.setText(existing.time);

            spinnerTeam1.setSelection(tournament.getNTeamByKey(existing.keyTeam1));
            spinnerTeam2.setSelection(tournament.getNTeamByKey(existing.keyTeam2));
            spinnerPhase.setSelection(getIndexByPhase(existing.type));

            // Pre-compila i set dai punteggi esistenti
            for (int[] set : existing.detail) {
                addSetRow(set[0], set[1]);
            }
            if (existing.detail.isEmpty()) {
                // Legacy senza dettaglio: mostra comunque i set già salvati
                txtSetCount.setText(existing.points1 + " - " + existing.points2);
            }

            btnConfirm.setOnClickListener(view -> {
                if (!TimeUtility.isValidTime(String.valueOf(txtTime.getText()))
                        || spinnerTeam1.getSelectedItemPosition() == spinnerTeam2.getSelectedItemPosition()
                        || spinnerPhase.getSelectedItemPosition() <= 0) {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Costruisce il detail dai set inseriti (righe vuote ignorate)
                ArrayList<int[]> detail = new ArrayList<>();
                for (int i = 0; i < llSets.getChildCount(); i++) {
                    View row = llSets.getChildAt(i);
                    String s1 = text(row, R.id.etSetPoints1);
                    String s2 = text(row, R.id.etSetPoints2);
                    if (s1.isEmpty() && s2.isEmpty()) continue;

                    int p1 = parseIntOrZero(s1);
                    int p2 = parseIntOrZero(s2);
                    if (p1 == p2) { // un set deve avere un vincitore
                        Toast.makeText(this, R.string.invalid_set, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    detail.add(new int[]{p1, p2});
                }

                int sets1, sets2;
                if (detail.isEmpty()) {
                    if (!existing.detail.isEmpty()) {
                        sets1 = 0; sets2 = 0; // aveva un dettaglio, ora azzerato dall'utente
                    } else {
                        // legacy senza detail: mantieni i set esistenti, non azzerarli
                        sets1 = existing.points1;
                        sets2 = existing.points2;
                    }
                } else {
                    sets1 = 0; sets2 = 0;
                    for (int[] set : detail) {
                        if (set[0] > set[1]) sets1++; else sets2++;
                    }
                }

                Match match = new Match(
                        tournament.getTeamByIndex(spinnerTeam1.getSelectedItemPosition()),
                        tournament.getTeamByIndex(spinnerTeam2.getSelectedItemPosition()),
                        Integer.parseInt(String.valueOf(txtDay.getText())),
                        String.valueOf(txtTime.getText()),
                        sets1, sets2,
                        stringPhases.get(spinnerPhase.getSelectedItemPosition()));
                match.detail = detail;
                match.key = existing.key;

                MatchUtility.editMatch(tournamentKey, match);
                finish();
            });
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> finish());
    }

    /** Aggiunge una riga-set (p1/p2 negativi = campo vuoto). */
    private void addSetRow(int p1, int p2) {
        View row = LayoutInflater.from(this).inflate(R.layout.layout_edit_set_row, llSets, false);
        EditText et1 = row.findViewById(R.id.etSetPoints1);
        EditText et2 = row.findViewById(R.id.etSetPoints2);
        if (p1 >= 0) et1.setText(String.valueOf(p1));
        if (p2 >= 0) et2.setText(String.valueOf(p2));

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateSetCount(); }
        };
        et1.addTextChangedListener(watcher);
        et2.addTextChangedListener(watcher);

        row.findViewById(R.id.btnRemoveSet).setOnClickListener(v -> {
            llSets.removeView(row);
            renumberSets();
            updateSetCount();
        });

        llSets.addView(row);
        renumberSets();
        updateSetCount();
    }

    /** Rinumera le etichette "SET n" dopo aggiunta/rimozione. */
    private void renumberSets() {
        for (int i = 0; i < llSets.getChildCount(); i++) {
            TextView label = llSets.getChildAt(i).findViewById(R.id.txtSetLabel);
            label.setText(getString(R.string.set_short) + " " + (i + 1));
        }
    }

    /** Aggiorna il conteggio set live (es. "2 - 1"). */
    private void updateSetCount() {
        int s1 = 0, s2 = 0;
        for (int i = 0; i < llSets.getChildCount(); i++) {
            View row = llSets.getChildAt(i);
            String a = text(row, R.id.etSetPoints1);
            String b = text(row, R.id.etSetPoints2);
            if (a.isEmpty() && b.isEmpty()) continue;
            int p1 = parseIntOrZero(a), p2 = parseIntOrZero(b);
            if (p1 > p2) s1++; else if (p2 > p1) s2++;
        }
        txtSetCount.setText(s1 + " - " + s2);
    }

    private String text(View row, int id) {
        return ((EditText) row.findViewById(id)).getText().toString().trim();
    }

    private int parseIntOrZero(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getIndexByPhase(String phase) {
        for (int i = 0; i < stringPhases.size(); i ++) {
            if (stringPhases.get(i).equals(phase)) {
                return i;
            }
        }

        return 0;
    }
}
