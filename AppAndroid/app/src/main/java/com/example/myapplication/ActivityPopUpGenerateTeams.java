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

        btnNPlayer2.setOnClickListener(v -> selectPlayerCount(2));
        btnNPlayer3.setOnClickListener(v -> selectPlayerCount(3));
        btnNPlayer4.setOnClickListener(v -> selectPlayerCount(4));
        btnNPlayer5.setOnClickListener(v -> selectPlayerCount(5));

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