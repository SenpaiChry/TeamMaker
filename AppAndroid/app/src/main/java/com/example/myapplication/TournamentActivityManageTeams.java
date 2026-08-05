package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

public class TournamentActivityManageTeams extends AppCompatActivity {

    public static TournamentModifyTeamsAdapter tournamentModifyTeamsAdapter;
    static private final int nCharSurname = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_modify_teams);

        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        Button btnNewTeam = findViewById(R.id.btnNewTeam);
        btnNewTeam.setOnClickListener(v -> openPopUpNewTeam(this, tournamentKey));

        ListView listView = findViewById(R.id.listViewTeams);
        tournamentModifyTeamsAdapter = new TournamentModifyTeamsAdapter(this, nCharSurname, tournament);
        listView.setAdapter(tournamentModifyTeamsAdapter);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());
    }

    public static void notifyDataChange() {
        tournamentModifyTeamsAdapter.notifyDataSetChanged();
    }

    public static void openPopUpNewTeam(Context context, String tournamentKey) {
        Intent intent = new Intent(context.getApplicationContext(), TournamentActivityPopUpEditTeam.class);
        intent.putExtra("tournament_key", tournamentKey);
        intent.putExtra("team_key", "null");
        intent.putExtra("nCharSurname", nCharSurname);
        context.startActivity(intent);
    }
}

