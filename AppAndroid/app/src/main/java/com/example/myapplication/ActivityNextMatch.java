package com.example.myapplication;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;

public class ActivityNextMatch extends AppCompatActivity {

    private int position;
    private String tournamentKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_match);

        position = getIntent().getIntExtra("position", -1) + 1;
        tournamentKey = getIntent().getStringExtra("tournament_key");

        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        if (tournament == null || position < 0 || position >= tournament.matches.size()) {
            finish();
            return;
        }

        Match match = tournament.matches.get(position);

        // Tipo
        TextView txtType = findViewById(R.id.txtType);
        if (match.type != null && !match.type.isEmpty()) {
            txtType.setText(match.type);
            txtType.setVisibility(View.VISIBLE);
        } else {
            txtType.setVisibility(View.GONE);
        }

        // Giorno e ora
        TextView txtDay = findViewById(R.id.txtDay);
        txtDay.setText(getString(R.string.day) + " " + match.day);

        TextView txtTime = findViewById(R.id.txtTime);
        txtTime.setText(match.time);

        // Nomi squadre
        TextView txtTeam1 = findViewById(R.id.txtTeam1);
        TextView txtTeam2 = findViewById(R.id.txtTeam2);
        txtTeam1.setText(tournament.toStringNTeamByKey(match.keyTeam1));
        txtTeam2.setText(tournament.toStringNTeamByKey(match.keyTeam2));

        // Giocatori
        int[] team1Ids = {R.id.txtTeam1Player1, R.id.txtTeam1Player2, R.id.txtTeam1Player3, R.id.txtTeam1Player4, R.id.txtTeam1Player5};
        int[] team2Ids = {R.id.txtTeam2Player1, R.id.txtTeam2Player2, R.id.txtTeam2Player3, R.id.txtTeam2Player4, R.id.txtTeam2Player5};
        fillPlayers(tournament.getTeamEntityByKey(match.keyTeam1), team1Ids);
        fillPlayers(tournament.getTeamEntityByKey(match.keyTeam2), team2Ids);

        // GIOCA
        Button btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityScorecard.class);
            intent.putExtra("position", position);
            intent.putExtra("tournament_key", tournamentKey);
            startActivity(intent);
            finish();
        });

        // Ruota schermo
        findViewById(R.id.btnRotate).setOnClickListener(v -> {
            int o = getResources().getConfiguration().orientation;
            setRequestedOrientation(o == Configuration.ORIENTATION_LANDSCAPE
                    ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        });

        // Indietro
        findViewById(R.id.btnGoBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("position", position);
        outState.putString("tournament_key", tournamentKey);
    }

    private void fillPlayers(Team team, int[] playerIds) {
        if (team == null || team.players == null) {
            for (int id : playerIds) findViewById(id).setVisibility(View.GONE);
            return;
        }
        ArrayList<Player> players = team.players;
        for (int i = 0; i < playerIds.length; i++) {
            TextView tv = findViewById(playerIds[i]);
            if (i < players.size()) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(players.get(i).getNameAndSurname(99));
            } else {
                tv.setVisibility(View.GONE);
            }
        }
    }
}