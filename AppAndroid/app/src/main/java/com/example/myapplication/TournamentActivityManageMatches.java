package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

public class TournamentActivityManageMatches extends AppCompatActivity {

    public static TournamentActivityManageMatches tournamentActivityManageMatches;
    public static TournamentBracketAdminAdapter tournamentBracketAdminAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_manage_matches);

        tournamentActivityManageMatches = this;

        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        Button btnNewMatch = findViewById(R.id.btnNewMatch);
        btnNewMatch.setOnClickListener(v -> openActivityNewMatch(tournamentKey));

        Button btnGenerateCalendar = findViewById(R.id.btnGenerateCalendar);
        Button btnDeleteEveryMatch = findViewById(R.id.btnDeleteEveryMatch);

        if (tournament.nBracket == 0) {
            btnGenerateCalendar.setVisibility(View.VISIBLE);
            btnDeleteEveryMatch.setVisibility(View.GONE);

            btnGenerateCalendar.setOnClickListener(v -> openPopUpBracket(tournamentKey));
        } else {
            btnGenerateCalendar.setVisibility(View.GONE);
            btnDeleteEveryMatch.setVisibility(View.VISIBLE);

            btnDeleteEveryMatch.setOnClickListener(v -> openPopUp(tournamentKey));
        }

        ListView listView = findViewById(R.id.listViewMatches);
        tournamentBracketAdminAdapter = new TournamentBracketAdminAdapter(tournament);
        listView.setAdapter(tournamentBracketAdminAdapter);
    }

    private void openPopUp(String tournamentKey) {
        Intent intent = new Intent(tournamentActivityManageMatches.getApplicationContext(), ActivityPopUp.class);
        intent.putExtra("tournament_key", tournamentKey);
        intent.putExtra("pop_up_type", "DELETE_EVERY_MATCH");
        tournamentActivityManageMatches.startActivity(intent);
    }

    private void openActivityNewMatch(String tournamentKey) {
        Intent intent = new Intent(tournamentActivityManageMatches.getApplicationContext(), TournamentActivityEditMatch.class);
        intent.putExtra("tournament_key", tournamentKey);
        intent.putExtra("position", -1);
        tournamentActivityManageMatches.startActivity(intent);
    }

    private void openPopUpBracket(String tournamentKey) {
        Intent intent = new Intent(tournamentActivityManageMatches.getApplicationContext(), ActivityPopUpGenerateBracket.class);
        intent.putExtra("tournament_key", tournamentKey);
        tournamentActivityManageMatches.startActivity(intent);
    }
}
