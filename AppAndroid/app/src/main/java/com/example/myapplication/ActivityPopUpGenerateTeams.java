package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Model.Constants;
import com.example.myapplication.Utility.TeamGeneratorUtility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityPopUpGenerateTeams extends AppCompatActivity {

    private int btnSelectedPlayer = 0;
    private Button[] playerButtons;
    private ExecutorService executor;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_generate_teams);

        // Centra la modale al 90% della larghezza schermo
        if (getWindow() != null) {
            getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        String type = getIntent().getExtras().getString("GENERATE_FOR");

        ProgressBar progressBar1 = findViewById(R.id.progressBar1);

        Button btnNPlayer2 = findViewById(R.id.btnNPlayer2);
        Button btnNPlayer3 = findViewById(R.id.btnNPlayer3);
        Button btnNPlayer4 = findViewById(R.id.btnNPlayer4);
        Button btnNPlayer5 = findViewById(R.id.btnNPlayer5);
        playerButtons = new Button[]{btnNPlayer2, btnNPlayer3, btnNPlayer4, btnNPlayer5};

        // Abilita un numero solo se ci sono abbastanza giocatori (stessa soglia del confirm: k*2-1)
        int selectedCount = Constants.playersSelected.size();
        for (int i = 0; i < playerButtons.length; i++) {
            int k = i + 2;
            boolean enough = selectedCount >= k * 2 - 1;
            playerButtons[i].setEnabled(enough);
            playerButtons[i].setAlpha(enough ? 1f : 0.35f);
            if (enough) {
                playerButtons[i].setOnClickListener(v -> selectPlayerCount(k));
            }
        }

        Button btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(view -> {
            if (btnSelectedPlayer > 0 && Constants.playersSelected.size() >= btnSelectedPlayer * 2 - 1) {

                // Precisione massima: differenza zero
                Constants.inputMaxDifference = 0;

                executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());

                Toast.makeText(ActivityGenerate.activityGenerate, R.string.generating, Toast.LENGTH_SHORT).show();

                progressBar1.setVisibility(View.VISIBLE);

                LinearLayout rootLayout = findViewById(R.id.rootLayout);
                setEnabledAllChildren(rootLayout, false);

                executor.execute(() -> {
                    boolean success = TeamGeneratorUtility.makeTeams(btnSelectedPlayer, 5, Constants.playersSelected);

                    handler.post(() -> {
                        if (success) {
                            Intent intent = new Intent(ActivityGenerate.activityGenerate.getApplicationContext(), ActivityTeams.class);
                            intent.putExtra("TYPE", type);
                            intent.putExtra("nPlayers", btnSelectedPlayer);
                            intent.putExtra("nAlgorithm", 5);
                            ActivityGenerate.activityGenerate.startActivity(intent);

                            finish();
                        } else {
                            // Generazione fallita: non aprire Teams (mostrerebbe dati vecchi),
                            // riabilita la popup e avvisa l'utente.
                            progressBar1.setVisibility(View.GONE);
                            setEnabledAllChildren(rootLayout, true);
                            Toast.makeText(ActivityGenerate.activityGenerate, R.string.need_more_time, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /** Seleziona il numero di giocatori per squadra e aggiorna lo stato dei tasti. */
    private void selectPlayerCount(int count) {
        btnSelectedPlayer = count;
        for (int i = 0; i < playerButtons.length; i++) {
            boolean selected = (i + 2) == count;
            playerButtons[i].setBackground(ContextCompat.getDrawable(this,
                    selected ? R.drawable.button_main_selected : R.drawable.bg_gender_unselected));
        }
        updateSplitPreview(count);
    }

    /** Mostra come verrebbero divise le squadre, es. "2 DA 3 E 2 DA 2". */
    @SuppressLint("SetTextI18n")
    private void updateSplitPreview(int playersPerTeam) {
        TextView txtSplitPreview = findViewById(R.id.txtSplitPreview);
        int n = Constants.playersSelected.size();
        int nTeams = (int) Math.ceil((float) n / playersPerTeam);
        if (nTeams <= 0) {
            txtSplitPreview.setVisibility(View.GONE);
            return;
        }

        int base = n / nTeams;      // dimensione minima delle squadre
        int rem = n % nTeams;       // quante squadre hanno una persona in più
        String from = getString(R.string.of_size);

        String text;
        if (rem == 0) {
            text = nTeams + " " + getString(R.string.teams) + " " + from + " " + base;
        } else {
            text = rem + " " + from + " " + (base + 1)
                    + " " + getString(R.string.and_word) + " "
                    + (nTeams - rem) + " " + from + " " + base;
        }

        txtSplitPreview.setText(text);
        txtSplitPreview.setVisibility(View.VISIBLE);
    }

    private void setEnabledAllChildren(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setEnabledAllChildren(group.getChildAt(i), enabled);
            }
        }
    }
}