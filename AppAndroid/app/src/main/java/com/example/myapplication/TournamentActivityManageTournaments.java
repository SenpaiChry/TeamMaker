package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Constants;

import java.util.Collections;
import java.util.Comparator;

public class TournamentActivityManageTournaments extends AppCompatActivity {

    static TournamentActivityManageTournaments tournamentActivityManageTournaments;
    static private TournamentAdapter tournamentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_manage_tournaments);

        tournamentActivityManageTournaments = this;

        ListView listViewTournaments = findViewById(R.id.listViewTournaments);
        tournamentAdapter = new TournamentAdapter(this);
        listViewTournaments.setAdapter(tournamentAdapter);

        Button btnNewTournament = findViewById(R.id.btnNewTournament);
        btnNewTournament.setOnClickListener(v -> openTournamentActivityGenerate());

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());
    }

    public static void reloadTournaments() {
        Collections.sort(Constants.tournaments, new Comparator<Tournament>() {
            @Override
            public int compare(Tournament t1, Tournament t2) {
                if (t1.date == null && t2.date == null) return 0;
                if (t1.date == null) return 1;
                if (t2.date == null) return -1;
                return t2.date.getTime().compareTo(t1.date.getTime()); // descending
            }
        });

        tournamentAdapter.notifyDataSetChanged();
    }

    public void openTournamentActivityGenerate() {
        Intent intent = new Intent(tournamentActivityManageTournaments.getApplicationContext(), ActivityGenerate.class);
        intent.putExtra("GENERATE_FOR", "TOURNAMENT");
        tournamentActivityManageTournaments.startActivity(intent);
    }
}


