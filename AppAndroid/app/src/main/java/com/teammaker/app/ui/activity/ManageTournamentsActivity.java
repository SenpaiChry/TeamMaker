package com.teammaker.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.model.Constants;
import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.ui.common.VerticalSpacingItemDecoration;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.ui.adapter.TournamentAdapter;
import com.teammaker.app.R;

public class ManageTournamentsActivity extends AppCompatActivity {

    // WeakReference: se l'Activity viene distrutta il GC puo' liberarla.
    // Chi ne ha bisogno usa ManageTournamentsActivity.get() e controlla null.
    private static WeakReference<ManageTournamentsActivity> instance = new WeakReference<>(null);
    public static ManageTournamentsActivity get() { return instance.get(); }

    private TournamentAdapter tournamentAdapter;

    private final Runnable onTournamentsChanged = this::reloadTournamentsInternal;

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

    @Override
    protected void onStart() {
        super.onStart();
        DataChangeBus.register(DataChangeBus.Event.TOURNAMENTS, onTournamentsChanged);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataChangeBus.unregister(DataChangeBus.Event.TOURNAMENTS, onTournamentsChanged);
    }

    /** Riordina i tornei per data (desc) e notifica l'adapter se aperto. */
    private void reloadTournamentsInternal() {
        Collections.sort(Constants.tournaments, new Comparator<Tournament>() {
            @Override
            public int compare(Tournament t1, Tournament t2) {
                if (t1.date == null && t2.date == null) return 0;
                if (t1.date == null) return 1;
                if (t2.date == null) return -1;
                return t2.date.getTime().compareTo(t1.date.getTime()); // descending
            }
        });

        if (tournamentAdapter != null) tournamentAdapter.notifyDataSetChanged();
    }

    public void openTournamentActivityGenerate() {
        Intent intent = new Intent(this, GenerateActivity.class);
        intent.putExtra("GENERATE_FOR", "TOURNAMENT");
        startActivity(intent);
    }
}
