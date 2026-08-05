package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.TopToast.TopToast;
import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Utility.Utility;

import java.util.HashMap;

public class ActivityEditPlayer extends AppCompatActivity {

    private Button btnGenderM;
    private Button btnGenderF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_edit_player);

        String key = (String) getIntent().getExtras().get("player_key");

        TextView txtTile = findViewById(R.id.title);
        EditText txtName = findViewById(R.id.txtName);
        EditText txtSurname = findViewById(R.id.txtSurname);
        EditText txtNickname = findViewById(R.id.txtNickname);

        btnGenderM = findViewById(R.id.btnGenderM);
        btnGenderF = findViewById(R.id.btnGenderF);

        Button btnConfirm = findViewById(R.id.btnConfirm);

        switch (key) {
            case "null": { // NEW
                Player player = new Player();

                txtTile.setText(R.string.new_player);

                btnGenderM.setOnClickListener(v -> {
                    player.gender = "M";
                    updateGenderButtons("M");
                });

                btnGenderF.setOnClickListener(v -> {
                    player.gender = "F";
                    updateGenderButtons("F");
                });

                ListView listStats = findViewById(R.id.listStats);
                StatsPlayerAdapter statsPlayerAdapter = new StatsPlayerAdapter(this, player.stats);
                listStats.setAdapter(statsPlayerAdapter);

                btnConfirm.setOnClickListener(view -> {
                    if (!String.valueOf(txtName.getText()).isEmpty() && player.gender != null && (player.gender.equals("F") || player.gender.equals("M"))) {
                        if (!Utility.isNetworkAvailable(this)) {
                            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
                            return;
                        }

                        btnConfirm.setEnabled(false); // disabilita il bottone

                        player.stats = statsPlayerAdapter.getStats();
                        Player playerTemp = new Player(
                                txtName.getText().toString(),
                                txtSurname.getText().toString(),
                                txtNickname.getText().toString(),
                                player.gender,
                                player.stats
                        );

                        PlayerUtility.addPlayer(playerTemp, success -> {
                            runOnUiThread(() -> {
                                if (success) {
                                    TopToast.show(this,
                                            "Salvataggio riuscito",
                                            "Il giocatore è stato salvato correttamente",
                                            TopToast.MessageType.SUCCESS);

                                    new android.os.Handler().postDelayed(this::finish, 1500); // chiude l’activity
                                } else {
                                    Toast.makeText(this, R.string.error_while_saving_data, Toast.LENGTH_SHORT).show();
                                    btnConfirm.setEnabled(true); // riabilita il bottone
                                }
                            });
                        });
                    } else {
                        Toast.makeText(this, R.string.missing_data, Toast.LENGTH_SHORT).show();
                    }
                });

                break;
            }
            default: { // EDIT
                Player player = PlayerUtility.getPlayerByKey(getIntent().getExtras().get("player_key").toString());

                txtTile.setText(R.string.edit_player);
                txtName.setText(player.name);
                txtSurname.setText(player.surname);
                txtNickname.setText(player.nickname);

                updateGenderButtons(player.gender);

                btnGenderM.setOnClickListener(v -> {
                    player.gender = "M";
                    updateGenderButtons("M");
                });

                btnGenderF.setOnClickListener(v -> {
                    player.gender = "F";
                    updateGenderButtons("F");
                });

                ListView listStats = findViewById(R.id.listStats);
                StatsPlayerAdapter statsPlayerAdapter = new StatsPlayerAdapter(this, (HashMap<String, Object>) player.stats.clone());
                listStats.setAdapter(statsPlayerAdapter);

                btnConfirm.setOnClickListener(view -> {
                    Player playerTemp = new Player(
                            player.key, txtName.getText().toString(), txtSurname.getText().toString(),
                            txtNickname.getText().toString(), player.gender, true, statsPlayerAdapter.getStats());

                    PlayerUtility.addEditPlayer(playerTemp, player.key);
                    finish();
                });

                break;
            }
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> finish());
    }

    /** Evidenzia il tasto del sesso selezionato: M azzurro, F rosa, l'altro neutro. */
    private void updateGenderButtons(String gender) {
        boolean isM = "M".equals(gender);
        boolean isF = "F".equals(gender);

        btnGenderM.setBackground(ContextCompat.getDrawable(this,
                isM ? R.drawable.bg_gender_selected_m : R.drawable.bg_gender_unselected));
        btnGenderF.setBackground(ContextCompat.getDrawable(this,
                isF ? R.drawable.bg_gender_selected_f : R.drawable.bg_gender_unselected));

        // Testo scuro sul tasto acceso (contrasto), chiaro su quello neutro
        btnGenderM.setTextColor(ContextCompat.getColor(this,
                isM ? R.color.scorecard_marquee_text : R.color.list_text_primary));
        btnGenderF.setTextColor(ContextCompat.getColor(this,
                isF ? R.color.scorecard_marquee_text : R.color.list_text_primary));
    }
}