package com.teammaker.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.Model.Constants;
import com.teammaker.app.Utility.VerticalSpacingItemDecoration;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;

public class TournamentActivityManageTournaments extends AppCompatActivity {

    // WeakReference: se l'Activity viene distrutta il GC puo' liberarla.
    // Chi ne ha bisogno usa TournamentActivityManageTournaments.get() e controlla null.
    private static WeakReference<TournamentActivityManageTournaments> instance = new WeakReference<>(null);
    public static TournamentActivityManageTournaments get() { return instance.get(); }

    private TournamentAdapter tournamentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_manage_tournaments);

        instance = new WeakReference<>(this);

        RecyclerView listViewTournaments = findViewById(R.id.listViewTournaments);
        listViewTournaments.setLayoutManager(new LinearLayoutManager(this));
        listViewTournaments.addItemDecoration(new VerticalSpacingItemDecoration(this, 8));
        tournamentAdapter = new TournamentAdapter(this);
        listViewTournaments.setAdapter(tournamentAdapter);

        Button btnNewTournament = findViewById(R.id.btnNewTournament);
        btnNewTournament.setOnClickListener(v -> openTournamentActivityGenerate());

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());
    }

    /**
     * Riordina la lista dei tornei per data (desc) e notifica l'adapter se l'Activity
     * e' viva. Se e' morta (utente su altra schermata), no-op: al prossimo onCreate
     * la lista sara' ricostruita dai dati aggiornati di Constants.
     */
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

        TournamentActivityManageTournaments a = get();
        if (a == null || a.tournamentAdapter == null) return;
        a.tournamentAdapter.notifyDataSetChanged();
    }

    public void openTournamentActivityGenerate() {
        Intent intent = new Intent(this, ActivityGenerate.class);
        intent.putExtra("GENERATE_FOR", "TOURNAMENT");
        startActivity(intent);
    }
}
