package com.teammaker.app.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.repository.PlayerRepository;
import com.teammaker.app.ui.common.VerticalSpacingItemDecoration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.ui.adapter.PlayerGenerateAdapter;
import com.teammaker.app.ui.popup.GenerateTeamsPopupActivity;
import com.teammaker.app.R;

public class GenerateActivity extends AppCompatActivity {

    // WeakReference: se l'Activity viene distrutta il GC puo' liberarla.
    // Chi ne ha bisogno usa GenerateActivity.get() e controlla null.
    private static WeakReference<GenerateActivity> instance = new WeakReference<>(null);
    public static GenerateActivity get() { return instance.get(); }

    // Selezione utente prima della generazione. La reference e' immutabile (final):
    // chi aggiunge/rimuove mutano la lista, il flusso di generazione la legge.
    private static final ArrayList<Player> selected = new ArrayList<>();
    public static ArrayList<Player> getSelected() { return selected; }

    private PlayerGenerateAdapter playerGenerateAdapter;
    private ArrayList<Player> playersToSee = new ArrayList<>();
    private TextView txtNSelected;
    private ImageView btnSelect;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint({"UseCompatLoadingForDrawables", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);

        String type = getIntent().getExtras().getString("GENERATE_FOR");

        selected.clear();
        instance = new WeakReference<>(this);
        btnSelect = findViewById(R.id.btnSelect);

        playersToSee = PlayerRepository.getPlayersActive(true);
        playersToSee.sort(
                Comparator.comparing((Player p) -> p.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p -> p.surname, String.CASE_INSENSITIVE_ORDER)
        );

        RecyclerView listViewGenerate = findViewById(R.id.listViewGenerate);
        listViewGenerate.setLayoutManager(new LinearLayoutManager(this));
        listViewGenerate.addItemDecoration(new VerticalSpacingItemDecoration(this, 8));
        playerGenerateAdapter = new PlayerGenerateAdapter(this, playersToSee);
        listViewGenerate.setAdapter(playerGenerateAdapter);

        txtNSelected = findViewById(R.id.txtNSelected);
        txtNSelected.setText(String.valueOf(selected.size()));

        btnSelect.setOnClickListener(v -> {
            if (btnSelect.getTag().equals(getResources().getString(R.string.select_all))) {
                switchSelectDeselect("SELECT");
            } else if (btnSelect.getTag().equals(getResources().getString(R.string.deselect_all))) {
                switchSelectDeselect("DESELECT");
            }
        });

        if (selected.size() == PlayerRepository.getPlayersActive(true).size()) {
            switchSelectDeselect("SELECT");
        } else if (selected.isEmpty()) {
            switchSelectDeselect("DESELECT");
        }

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

                for (Player player : PlayerRepository.getPlayersActive(true)) {
                    if (player.containsString(s.toString()) && player.isActive) {
                        playersToSee.add(player);
                    }
                }

                if (!type.equals("TOURNAMENT")) {
                    playersToSee.sort(
                            Comparator.comparing((Player p) -> p.name, String.CASE_INSENSITIVE_ORDER)
                                    .thenComparing(p -> p.surname, String.CASE_INSENSITIVE_ORDER)
                    );
                }

                playerGenerateAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        Button btnGenerate = findViewById(R.id.btnGenerate);
        btnGenerate.setOnClickListener(v -> openActivityPopUpGenerate(type));

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());
    }

    void openActivityPopUpGenerate(String type) {
        Intent intent = new Intent(this, GenerateTeamsPopupActivity.class);
        intent.putExtra("GENERATE_FOR", type);
        startActivity(intent);
    }

    /**
     * Aggiorna il contatore giocatori selezionati mostrato in alto.
     * Chiamato dall'adapter quando l'utente tocca una card.
     */
    public void updateSelectedCount() {
        if (txtNSelected != null) {
            txtNSelected.setText(String.valueOf(selected.size()));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void switchSelectDeselect(String type) {
        if (btnSelect == null || playerGenerateAdapter == null) return;
        if (type.equals("SELECT")) {
            btnSelect.setTag(getResources().getString(R.string.deselect_all));
            btnSelect.setImageDrawable(getResources().getDrawable(R.drawable.deselect_all));
            selected.clear();
            selected.addAll(PlayerRepository.getPlayersActive(true));
            txtNSelected.setText(String.valueOf(selected.size()));
            playerGenerateAdapter.notifyDataSetChanged();
        } else if (type.equals("DESELECT")) {
            btnSelect.setTag(getResources().getString(R.string.select_all));
            btnSelect.setImageDrawable(getResources().getDrawable(R.drawable.select_all));
            selected.clear();
            txtNSelected.setText(String.valueOf(selected.size()));
            playerGenerateAdapter.notifyDataSetChanged();
        }
    }
}
