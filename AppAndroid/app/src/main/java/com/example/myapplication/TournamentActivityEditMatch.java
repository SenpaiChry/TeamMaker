package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.MatchUtility;
import com.example.myapplication.Utility.TimeUtility;
import com.example.myapplication.Utility.TournamentScheduleUtility;
import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

import java.util.ArrayList;
import java.util.Arrays;

public class TournamentActivityEditMatch extends AppCompatActivity {

    ArrayList<String> stringTeams = new ArrayList<>();
    ArrayList<String> stringPhases = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_edit_match);

        int position = (int) getIntent().getExtras().get("position");
        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        stringTeams.add("TO DO");
        for (Team team : tournament.teams) {
            stringTeams.add(team.toStringNameAndSurname());
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
        EditText txtPoints1 = findViewById(R.id.txtPoints1);
        EditText txtPoints2 = findViewById(R.id.txtPoints2);

        TournamentSpinnerTeamAdapter adapterTeam = new TournamentSpinnerTeamAdapter(this, stringTeams);
        Spinner spinnerTeam1 = findViewById(R.id.spinnerTeam1);
        Spinner spinnerTeam2 = findViewById(R.id.spinnerTeam2);
        spinnerTeam1.setAdapter(adapterTeam);
        spinnerTeam2.setAdapter(adapterTeam);

        TournamentSpinnerTeamAdapter adapterPhase = new TournamentSpinnerTeamAdapter(this, stringPhases);
        Spinner spinnerPhase = findViewById(R.id.spinnerPhase);
        spinnerPhase.setAdapter(adapterPhase);

        Button btnConfirm = findViewById(R.id.btnConfirm);

        if (position < 0) { // NEW
            txtTitle.setText(this.getText(R.string.new_match));
            findViewById(R.id.llPoints1).setVisibility(View.GONE);
            findViewById(R.id.llPoints2).setVisibility(View.GONE);

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
            txtDay.setText(String.valueOf(tournament.matches.get(position).day));
            txtTime.setText(tournament.matches.get(position).time);
            txtPoints1.setText(String.valueOf(tournament.matches.get(position).points1));
            txtPoints2.setText(String.valueOf(tournament.matches.get(position).points2));

            spinnerTeam1.setSelection(tournament.getNTeamByKey(tournament.matches.get(position).keyTeam1));
            spinnerTeam2.setSelection(tournament.getNTeamByKey(tournament.matches.get(position).keyTeam2));

            spinnerPhase.setSelection(getIndexByPhase(tournament.matches.get(position).type));

            btnConfirm.setOnClickListener(view -> {
                if (TimeUtility.isValidTime(String.valueOf(txtTime.getText()))
                        && spinnerTeam1.getSelectedItemPosition() != spinnerTeam2.getSelectedItemPosition()
                        && Integer.parseInt(String.valueOf(txtPoints1.getText())) >= 0
                        && Integer.parseInt(String.valueOf(txtPoints2.getText())) >= 0
                        && spinnerPhase.getSelectedItemPosition() > 0) {

                    Match match = new Match(
                            tournament.getTeamByIndex(spinnerTeam1.getSelectedItemPosition()),
                            tournament.getTeamByIndex(spinnerTeam2.getSelectedItemPosition()),
                            Integer.parseInt(String.valueOf(txtDay.getText())),
                            String.valueOf(txtTime.getText()),
                            Integer.parseInt(String.valueOf(txtPoints1.getText())),
                            Integer.parseInt(String.valueOf(txtPoints2.getText())),
                            stringPhases.get(spinnerPhase.getSelectedItemPosition()));

                    match.key = tournament.matches.get(position).key;
                    MatchUtility.editMatch(tournamentKey, match);
                    finish();
                } else {
                    Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                }
            });
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> finish());
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
