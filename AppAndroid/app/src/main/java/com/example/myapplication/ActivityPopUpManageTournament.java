package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.TournamentUtility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class ActivityPopUpManageTournament extends AppCompatActivity {

    private static Button btnActivate;
    private static Button btnDeactivate;
    private static Tournament tournament;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_manage_tournament);

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    WindowManager.LayoutParams.WRAP_CONTENT);
            getWindow().setGravity(Gravity.CENTER);
        }

        String tournamentKey = getIntent().getStringExtra("tournament_key");
        tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        EditText txtName = findViewById(R.id.txtName);
        if (tournament.name != null) txtName.setText(tournament.name);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        EditText txtDate = findViewById(R.id.txtDate);

        if (tournament.date != null) {
            txtDate.setText(sdf.format(tournament.date.getTime()));
        }

        txtDate.setOnClickListener(v -> {
            Calendar date = tournament.date != null ? tournament.date : Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(ActivityPopUpManageTournament.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        txtDate.setText(selectedDate);
                    }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));

            datePickerDialog.show();
        });

        Button btnManageMatches = findViewById(R.id.btnManageMatches);
        btnManageMatches.setOnClickListener(v -> openActivityManageMatches(tournamentKey));

        Button btnManageTeams = findViewById(R.id.btnManageTeams);
        btnManageTeams.setOnClickListener(v -> openActivityManageTeams(tournamentKey));

        btnDeactivate = findViewById(R.id.btnDeactivate);
        btnDeactivate.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityPopUp.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("pop_up_type", PopUpType.DEACTIVATE_TOURNAMENT);
            startActivity(intent);
        });

        btnActivate = findViewById(R.id.btnActivate);
        btnActivate.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityPopUp.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("pop_up_type", PopUpType.ACTIVATE_TOURNAMENT);
            startActivity(intent);
        });

        if (tournament.isValid) {
            btnActivate.setVisibility(View.GONE);
            btnDeactivate.setVisibility(View.VISIBLE);
        } else {
            btnDeactivate.setVisibility(View.GONE);
            btnActivate.setVisibility(View.VISIBLE);
        }

        Button btnCopyTeams = findViewById(R.id.btnCopyTeams);
        btnCopyTeams.setOnClickListener(v -> {
            StringBuilder textToCopy = new StringBuilder();

            for (Team team : tournament.teams) {
                textToCopy.append(getString(R.string.team)).append(" ").append(tournament.getNTeamByKey(team.key));
                if (tournament.nBracket > 1) {
                    textToCopy.append(" (").append(getString(R.string.bracket)).append(" ").append(team.bracket).append(")");
                }
                textToCopy.append(": ").append(team.toStringNameAndSurname()).append("\n");
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Testo copiato", textToCopy.toString()));
            Toast.makeText(this, "Testo copiato negli appunti", Toast.LENGTH_SHORT).show();
        });

        Button btnCopyMatches = findViewById(R.id.btnCopyMatches);
        btnCopyMatches.setOnClickListener(v -> {
            ArrayList<Match> matches = new ArrayList<>(tournament.matches);

            Collections.sort(matches, (m1, m2) -> {
                if (m1.day != m2.day) return Integer.compare(m1.day, m2.day);
                return m1.time.compareTo(m2.time);
            });

            StringBuilder textToCopy = new StringBuilder();
            for (Match match : matches) {
                textToCopy.append(getString(R.string.match)).append(" ").append(tournament.getNMatchByKey(match.key))
                        .append(" - ").append(match.time)
                        .append(" (").append(match.type.trim()).append("): ")
                        .append(getString(R.string.team)).append(" ").append(tournament.getNTeamByKey(match.keyTeam1))
                        .append(" VS ").append(getString(R.string.team)).append(" ").append(tournament.getNTeamByKey(match.keyTeam2))
                        .append("\n");
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Testo copiato", textToCopy.toString()));
            Toast.makeText(this, "Testo copiato negli appunti", Toast.LENGTH_SHORT).show();
        });

        Button btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> {
            String name = txtName.getText().toString();
            String dateStr = txtDate.getText().toString();

            if (!name.isEmpty() && !dateStr.isEmpty()) {
                Calendar newDate = Calendar.getInstance();
                try {
                    newDate.setTime(sdf.parse(dateStr));
                } catch (Exception e) {
                    Log.d("FATAL catch", e.toString());
                    return;
                }
                TournamentUtility.updateNameAndDateTournament(tournamentKey, name, newDate);
            } else {
                Toast.makeText(this, "MISSING DATA", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    private void openActivityManageTeams(String tournamentKey) {
        Intent intent = new Intent(this, TournamentActivityManageTeams.class);
        intent.putExtra("tournament_key", tournamentKey);
        startActivity(intent);
    }

    private void openActivityManageMatches(String tournamentKey) {
        Intent intent = new Intent(this, TournamentActivityManageMatches.class);
        intent.putExtra("tournament_key", tournamentKey);
        startActivity(intent);
    }

    static void updateButtons() {
        if (tournament.isValid) {
            btnDeactivate.setVisibility(View.VISIBLE);
            btnActivate.setVisibility(View.GONE);
        } else {
            btnDeactivate.setVisibility(View.GONE);
            btnActivate.setVisibility(View.VISIBLE);
        }
    }
}