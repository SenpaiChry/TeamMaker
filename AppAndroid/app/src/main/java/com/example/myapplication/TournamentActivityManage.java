package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class TournamentActivityManage extends AppCompatActivity {

    static TournamentActivityManage tournamentActivityManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_manage);

        tournamentActivityManage = this;

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        LinearLayout btnManageTournament = findViewById(R.id.btnManageTournament);
        btnManageTournament.setOnClickListener(v -> openTournamentActivityManageTournaments());

        LinearLayout btnManagePlayers = findViewById(R.id.btnManagePlayers);
        btnManagePlayers.setOnClickListener(v -> openTournamentActivityManagePlayers());
    }

    public void openTournamentActivityManageTournaments() {
        Intent intent = new Intent(tournamentActivityManage.getApplicationContext(), TournamentActivityManageTournaments.class);
        tournamentActivityManage.startActivity(intent);
    }

    public void openTournamentActivityManagePlayers() {
        Intent intent = new Intent(tournamentActivityManage.getApplicationContext(), ActivityAdmin.class);
        tournamentActivityManage.startActivity(intent);
    }
}

