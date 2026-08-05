package com.example.myapplication;

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
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Constants;
import com.example.myapplication.Utility.PlayerUtility;

import java.util.ArrayList;
import java.util.Comparator;

public class ActivityGenerate extends AppCompatActivity {

    static PlayerGenerateAdapter playerGenerateAdapter;
    private ArrayList<Player> playersToSee = new ArrayList<>();
    static ActivityGenerate activityGenerate;
    static TextView txtNSelected;
    static ImageView btnSelect;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint({"UseCompatLoadingForDrawables", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate);

        String type = getIntent().getExtras().getString("GENERATE_FOR");

        Constants.playersSelected.clear();
        activityGenerate = this;
        btnSelect = findViewById(R.id.btnSelect);

        playersToSee = PlayerUtility.getPlayersActive(true);
        playersToSee.sort(
                Comparator.comparing((Player p) -> p.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(p -> p.surname, String.CASE_INSENSITIVE_ORDER)
        );

        ListView listViewGenerate = findViewById(R.id.listViewGenerate);
        playerGenerateAdapter = new PlayerGenerateAdapter(listViewGenerate.getContext(), playersToSee);
        listViewGenerate.setAdapter(playerGenerateAdapter);

        txtNSelected = findViewById(R.id.txtNSelected);
        txtNSelected.setText(String.valueOf(Constants.playersSelected.size()));

        btnSelect.setOnClickListener(v -> {
            if (btnSelect.getTag().equals(getResources().getString(R.string.select_all))) {
                switchSelectDeselect("SELECT");
            } else if (btnSelect.getTag().equals(getResources().getString(R.string.deselect_all))) {
                switchSelectDeselect("DESELECT");
            }
        });

        if (Constants.playersSelected.size() == PlayerUtility.getPlayersActive(true).size()) {
            switchSelectDeselect("SELECT");
        } else if (Constants.playersSelected.isEmpty()) {
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

                for (Player player : PlayerUtility.getPlayersActive(true)) {
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
        Intent intent = new Intent(activityGenerate.getApplicationContext(), ActivityPopUpGenerateTeams.class);
        intent.putExtra("GENERATE_FOR", type);
        activityGenerate.startActivity(intent);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    static void switchSelectDeselect(String type) {
        if (type.equals("SELECT")) {
            btnSelect.setTag(activityGenerate.getResources().getString(R.string.deselect_all));
            btnSelect.setImageDrawable(activityGenerate.getResources().getDrawable(R.drawable.deselect_all));
            Constants.playersSelected = PlayerUtility.getPlayersActive(true);
            txtNSelected.setText(String.valueOf(Constants.playersSelected.size()));
            playerGenerateAdapter.notifyDataSetChanged();
        } else if (type.equals("DESELECT")) {
            btnSelect.setTag(activityGenerate.getResources().getString(R.string.select_all));
            btnSelect.setImageDrawable(activityGenerate.getResources().getDrawable(R.drawable.select_all));
            Constants.playersSelected.clear();
            txtNSelected.setText(String.valueOf(Constants.playersSelected.size()));
            playerGenerateAdapter.notifyDataSetChanged();
        }
    }
}