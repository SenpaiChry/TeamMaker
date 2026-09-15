package com.teammaker.app.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.data.repository.TournamentRepository;
import com.teammaker.app.ui.common.VerticalSpacingItemDecoration;

import java.lang.ref.WeakReference;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.ui.adapter.TournamentModifyTeamsAdapter;
import com.teammaker.app.ui.popup.EditTeamPopupActivity;
import com.teammaker.app.R;

public class ManageTeamsActivity extends AppCompatActivity {

    // WeakReference: se l'Activity viene distrutta il GC puo' liberarla.
    // Chi ne ha bisogno usa ManageTeamsActivity.get() e controlla null.
    private static WeakReference<ManageTeamsActivity> instance = new WeakReference<>(null);
    public static ManageTeamsActivity get() { return instance.get(); }

    private TournamentModifyTeamsAdapter tournamentModifyTeamsAdapter;
    static private final int nCharSurname = 5;

    private final Runnable onTeamsChanged = this::notifyDataChangeInternal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_modify_teams);

        instance = new WeakReference<>(this);

        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);

        Button btnNewTeam = findViewById(R.id.btnNewTeam);
        btnNewTeam.setOnClickListener(v -> openPopUpNewTeam(this, tournamentKey));

        RecyclerView listView = findViewById(R.id.listViewTeams);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.addItemDecoration(new VerticalSpacingItemDecoration(this, 8));
        tournamentModifyTeamsAdapter = new TournamentModifyTeamsAdapter(this, nCharSurname, tournament);
        listView.setAdapter(tournamentModifyTeamsAdapter);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        DataChangeBus.register(DataChangeBus.Event.TEAMS, onTeamsChanged);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataChangeBus.unregister(DataChangeBus.Event.TEAMS, onTeamsChanged);
    }

    private void notifyDataChangeInternal() {
        if (tournamentModifyTeamsAdapter != null) tournamentModifyTeamsAdapter.notifyDataSetChanged();
    }

    public static void openPopUpNewTeam(Context context, String tournamentKey) {
        Intent intent = new Intent(context.getApplicationContext(), EditTeamPopupActivity.class);
        intent.putExtra("tournament_key", tournamentKey);
        intent.putExtra("team_key", "null");
        intent.putExtra("nCharSurname", nCharSurname);
        context.startActivity(intent);
    }
}
