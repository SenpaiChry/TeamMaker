package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.myapplication.Model.Constants;
import com.example.myapplication.Utility.PlayerExportUtility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class ActivityAdmin extends AppCompatActivity {

    private static ArrayList<Player> playersToSee;
    public static ActivityAdmin activityAdmin;
    public static PlayerAdminAdapter playerAdminAdapter;
    ListView listViewAdmin;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        activityAdmin = this;
        listViewAdmin = findViewById(R.id.listViewAdmin);
        playersToSee = new ArrayList<>(Constants.players);
        sortActiveFirst(playersToSee);
        playerAdminAdapter = new PlayerAdminAdapter(listViewAdmin.getContext(), playersToSee);
        listViewAdmin.setAdapter(playerAdminAdapter);

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        Button btnAddNewPlayer = findViewById(R.id.btnAddNewPlayer);
        btnAddNewPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(activityAdmin.getApplicationContext(), ActivityEditPlayer.class);
            intent.putExtra("player_key", "null");
            activityAdmin.startActivity(intent);
        });

        Button btnExport = findViewById(R.id.btnExportPlayers);
        btnExport.setOnClickListener(v -> exportPlayers());

        EditText txtSearch = findViewById(R.id.txtSearch);
        txtSearch.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (txtSearch.getRight() - txtSearch.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    txtSearch.setText("");
                    return true;
                }
            }
            return false;
        });

        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                playersToSee.clear();

                for (Player player : Constants.players) {
                    if (player.containsString(s.toString())) {
                        playersToSee.add(player);
                    }
                }

                sortActiveFirst(playersToSee);
                playerAdminAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * Genera l'xlsx con la lista giocatori e apre lo share sheet Android per
     * salvarlo/spedirlo dove si vuole (Drive, Downloads, email, WhatsApp...).
     */
    private void exportPlayers() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            File exportDir = new File(getCacheDir(), "exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File file = new File(exportDir, "teammaker_giocatori_" + timestamp + ".xlsx");

            PlayerExportUtility.exportToXlsx(file);

            // FileProvider condivide con altre app in modo sicuro
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", file);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_players_subject));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(share, getString(R.string.export)));
        } catch (Exception e) {
            Log.e("ActivityAdmin", "Export fallito", e);
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public static void reloadPlayers() {
        playersToSee.clear();
        playersToSee.addAll(Constants.players);
        sortActiveFirst(playersToSee);
        playerAdminAdapter.notifyDataSetChanged();
    }

    /**
     * Attivi prima (ordinati per voto), archiviati in fondo (ordinati per voto).
     */
    private static void sortActiveFirst(ArrayList<Player> list) {
        Collections.sort(list, (p1, p2) -> {
            if (p1.isActive != p2.isActive) {
                return p1.isActive ? -1 : 1;
            }
            return p1.compareTo(p2);
        });
    }
}