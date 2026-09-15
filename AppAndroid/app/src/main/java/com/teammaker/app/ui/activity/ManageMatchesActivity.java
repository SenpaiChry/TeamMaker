package com.teammaker.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.teammaker.app.ui.adapter.TournamentBracketAdminAdapter;
import com.teammaker.app.ui.popup.ConfirmPopupActivity;
import com.teammaker.app.ui.popup.GenerateBracketPopupActivity;
import com.teammaker.app.ui.popup.GenerateFinalsPopupActivity;
import com.teammaker.app.R;

public class ManageMatchesActivity extends AppCompatActivity {

    // WeakReference: se l'Activity viene distrutta il GC puo' liberarla.
    // Chi ne ha bisogno usa ManageMatchesActivity.get() e controlla null.
    private static WeakReference<ManageMatchesActivity> instance = new WeakReference<>(null);
    public static ManageMatchesActivity get() { return instance.get(); }

    private TournamentBracketAdminAdapter tournamentBracketAdminAdapter;

    private final Runnable onMatchesChanged = this::onMatchesChangedInternal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_manage_matches);

        instance = new WeakReference<>(this);

        String tournamentKey = getIntent().getExtras().getString("tournament_key");
        Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        Button btnNewMatch = findViewById(R.id.btnNewMatch);
        btnNewMatch.setOnClickListener(v -> openActivityNewMatch(tournamentKey));

        Button btnGenerateCalendar = findViewById(R.id.btnGenerateCalendar);
        Button btnGenerateFinals = findViewById(R.id.btnGenerateFinals);
        Button btnDeleteEveryMatch = findViewById(R.id.btnDeleteEveryMatch);

        // Torneo bloccato: nascondo TUTTE le azioni sulle partite (nuova, genera,
        // cancella tutte). L'adapter, a sua volta, nasconde ✎/🗑/▶ sulle righe.
        if (tournament.locked) {
            btnNewMatch.setVisibility(View.GONE);
            btnGenerateCalendar.setVisibility(View.GONE);
            btnGenerateFinals.setVisibility(View.GONE);
            btnDeleteEveryMatch.setVisibility(View.GONE);
        } else {
            // Se non ci sono partite -> mostro GENERA CALENDARIO. Se ce ne sono
            // (a prescindere dall'origine: generate o aggiunte a mano) -> mostro
            // GENERA FINALI e CANCELLA TUTTE.
            boolean hasMatches = !tournament.matches.isEmpty();
            if (!hasMatches) {
                btnGenerateCalendar.setVisibility(View.VISIBLE);
                btnGenerateFinals.setVisibility(View.GONE);
                btnDeleteEveryMatch.setVisibility(View.GONE);

                btnGenerateCalendar.setOnClickListener(v -> openPopUpBracket(tournamentKey));
            } else {
                btnGenerateCalendar.setVisibility(View.GONE);
                btnGenerateFinals.setVisibility(View.VISIBLE);
                btnDeleteEveryMatch.setVisibility(View.VISIBLE);

                btnGenerateFinals.setOnClickListener(v -> openPopUpFinals(tournamentKey));
                btnDeleteEveryMatch.setOnClickListener(v -> openPopUp(tournamentKey));
            }
        }

        RecyclerView listView = findViewById(R.id.listViewMatches);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.addItemDecoration(new VerticalSpacingItemDecoration(this, 8));
        tournamentBracketAdminAdapter = new TournamentBracketAdminAdapter(tournament);
        listView.setAdapter(tournamentBracketAdminAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DataChangeBus.register(DataChangeBus.Event.MATCHES, onMatchesChanged);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataChangeBus.unregister(DataChangeBus.Event.MATCHES, onMatchesChanged);
    }

    /**
     * Rinfresca la schermata dopo un cambio dei dati. Usa recreate() perche' i
     * bottoni in cima cambiano in base allo stato (GENERA CALENDARIO vs GENERA
     * FINALI + CANCELLA TUTTE), non basta notifyDataSetChanged sull'adapter.
     */
    private void onMatchesChangedInternal() {
        if (isDestroyed()) return;
        try {
            recreate();
        } catch (Exception e) {
            android.util.Log.w("ManageMatches", "recreate fallito", e);
        }
    }

    private void openPopUp(String tournamentKey) {
        Intent intent = new Intent(this, ConfirmPopupActivity.class);
        intent.putExtra("tournament_key", tournamentKey);
        intent.putExtra("pop_up_type", "DELETE_EVERY_MATCH");
        startActivity(intent);
    }

    private void openActivityNewMatch(String tournamentKey) {
        Intent intent = new Intent(this, EditMatchActivity.class);
        intent.putExtra("tournament_key", tournamentKey);
        intent.putExtra("position", -1);
        startActivity(intent);
    }

    private void openPopUpBracket(String tournamentKey) {
        Intent intent = new Intent(this, GenerateBracketPopupActivity.class);
        intent.putExtra("tournament_key", tournamentKey);
        startActivity(intent);
    }

    private void openPopUpFinals(String tournamentKey) {
        Intent intent = new Intent(this, GenerateFinalsPopupActivity.class);
        intent.putExtra("tournament_key", tournamentKey);
        startActivity(intent);
    }
}
