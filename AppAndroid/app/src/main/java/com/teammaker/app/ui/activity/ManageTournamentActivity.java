package com.teammaker.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.teammaker.app.R;

public class ManageTournamentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_manage);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        LinearLayout btnManageTournament = findViewById(R.id.btnManageTournament);
        btnManageTournament.setOnClickListener(v -> openTournamentActivityManageTournaments());

        LinearLayout btnManagePlayers = findViewById(R.id.btnManagePlayers);
        btnManagePlayers.setOnClickListener(v -> openTournamentActivityManagePlayers());

        LinearLayout btnManageStats = findViewById(R.id.btnManageStats);
        btnManageStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageStatsActivity.class);
            startActivity(intent);
        });
    }

    public void openTournamentActivityManageTournaments() {
        Intent intent = new Intent(this, ManageTournamentsActivity.class);
        startActivity(intent);
    }

    public void openTournamentActivityManagePlayers() {
        Intent intent = new Intent(this, AdminActivity.class);
        startActivity(intent);
    }
}
