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
        Button btnGenerateFinals = findViewById(R.id.btnGenerateFinals);
        Button btnDeleteEveryMatch = findViewById(R.id.btnDeleteEveryMatch);

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

        ListView listView = findViewById(R.id.listViewMatches);
        tournamentBracketAdminAdapter = new TournamentBracketAdminAdapter(tournament);
        listView.setAdapter(tournamentBracketAdminAdapter);
    }

    /** Rinfresca la schermata (se aperta) dopo un aggiornamento realtime dei dati. */
    public static void reloadMatches() {
        if (tournamentActivityManageMatches == null || tournamentActivityManageMatches.isDestroyed()) {
            return;
        }
        tournamentActivityManageMatches.runOnUiThread(() -> {
            try {
                tournamentActivityManageMatches.recreate();
            } catch (Exception e) {
                // Difensivo: se l'activity e' in transizione, recreate() puo' fallire.
                // Non e' un bug: la prossima onCreate ridisegnera' comunque i dati.
                android.util.Log.w("ManageMatches", "reloadMatches.recreate fallito", e);
            }
        });
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

    private void openPopUpFinals(String tournamentKey) {
        Intent intent = new Intent(tournamentActivityManageMatches.getApplicationContext(), ActivityPopUpGenerateFinals.class);
        intent.putExtra("tournament_key", tournamentKey);
        tournamentActivityManageMatches.startActivity(intent);
    }
}
