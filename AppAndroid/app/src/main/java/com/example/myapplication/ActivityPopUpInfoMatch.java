package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;

public class ActivityPopUpInfoMatch extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_info_match);

        if (getWindow() != null) {
            getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92), WindowManager.LayoutParams.WRAP_CONTENT);
        }

        if (getIntent().getExtras() != null) {

            Match match = TournamentUtility.getActiveTournament()
                    .getMatchByKey(getIntent().getExtras().get("match_key").toString());

            TextView txtTeam1 = findViewById(R.id.txtTeam1);
            TextView txtTeam2 = findViewById(R.id.txtTeam2);

            TextView txtDay = findViewById(R.id.txtDay);
            TextView txtType = findViewById(R.id.txtType);
            TextView txtTime = findViewById(R.id.txtTime);
            TextView txtFinalPoints1 = findViewById(R.id.txtFinalPoints1);
            TextView txtFinalPoints2 = findViewById(R.id.txtFinalPoints2);
            TextView txtPoints1 = findViewById(R.id.txtPoints1);
            TextView txtPoints2 = findViewById(R.id.txtPoints2);

            txtDay.setText(getString(R.string.day) + " " + match.day);
            txtType.setText(match.type);
            txtTime.setText(match.time);
            txtFinalPoints1.setText(String.valueOf(match.points1));
            txtFinalPoints2.setText(String.valueOf(match.points2));

            TextView txtSetDetail = findViewById(R.id.txtSetDetail);
            String setDetail = match.detailString();
            if (setDetail.isEmpty()) {
                txtSetDetail.setVisibility(View.GONE);
            } else {
                txtSetDetail.setText(setDetail);
                txtSetDetail.setVisibility(View.VISIBLE);
            }

            txtPoints1.setVisibility(View.GONE);
            txtPoints2.setVisibility(View.GONE);

            txtTeam1.setText(TournamentUtility.getActiveTournament().toStringNTeamByKey(match.keyTeam1));
            txtTeam2.setText(TournamentUtility.getActiveTournament().toStringNTeamByKey(match.keyTeam2));

            int[] team1PlayerIds = {R.id.txtTeam1Player1, R.id.txtTeam1Player2,
                R.id.txtTeam1Player3, R.id.txtTeam1Player4, R.id.txtTeam1Player5};
            int[] team2PlayerIds = {R.id.txtTeam2Player1, R.id.txtTeam2Player2,
                R.id.txtTeam2Player3, R.id.txtTeam2Player4, R.id.txtTeam2Player5};

            fillPlayers(TournamentTeamUtility.getTeamByKey(match.keyTeam1), team1PlayerIds);
            fillPlayers(TournamentTeamUtility.getTeamByKey(match.keyTeam2), team2PlayerIds);
        }

        Button btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(view -> finish());
    }

    private void fillPlayers(Team team, int[] playerIds) {
        final int FULL = 99;

        if (team == null || team.players == null || team.players.isEmpty()) {
            TextView first = findViewById(playerIds[0]);
            first.setText(R.string.no_players);
            for (int i = 1; i < playerIds.length; i++) {
                findViewById(playerIds[i]).setVisibility(View.GONE);
            }
            return;
        }

        ArrayList<Player> players = team.players;
        for (int i = 0; i < playerIds.length; i++) {
            TextView view = findViewById(playerIds[i]);
            if (i < players.size()) {
                view.setVisibility(View.VISIBLE);
                view.setText(players.get(i).getNameAndSurname(FULL));
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }
}