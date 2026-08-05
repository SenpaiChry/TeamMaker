package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Utility.Utility;

public class ActivityInfoPlayer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_info_player);

        // Allarga la finestra dialog al 92% dello schermo (richiede windowIsFloating=false nel tema)
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        if (getIntent().getExtras() != null) {
            Player player = PlayerUtility.getPlayerByKey(getIntent().getExtras().get("player_key").toString());

            ListView listViewInfo = findViewById(R.id.listInfo);
            TextView txtName = findViewById(R.id.txtName);
            TextView txtSurname = findViewById(R.id.txtSurname);
            TextView txtNickname = findViewById(R.id.txtNickname);
            TextView vote = findViewById(R.id.txtVote);

            txtName.setText(player.name);
            txtSurname.setText(player.surname);
            txtNickname.setText(player.nickname);

            if (player.surname == null || player.surname.isEmpty()) {
                txtSurname.setVisibility(View.GONE);
            }
            if (player.nickname == null || player.nickname.isEmpty()) {
                txtNickname.setVisibility(View.GONE);
            }

            vote.setText(String.valueOf(player.getVote()).replace('.', ','));

            // Colori per genere (varianti chiare, leggibili su fondo scuro)
            boolean isF = "F".equals(player.gender);
            int nameColor = ContextCompat.getColor(this,
                    isF ? R.color.women_color_name_dark : R.color.men_color_name_dark);
            int shadowColor = ContextCompat.getColor(this,
                    isF ? R.color.women_shadow : R.color.men_shadow);

            txtName.setTextColor(nameColor);
            txtSurname.setTextColor(nameColor);
            txtNickname.setTextColor(nameColor);
            vote.setTextColor(nameColor);
            vote.setShadowLayer(10, 1, 1, shadowColor);

            PlayerInfoAdapter adapter = new PlayerInfoAdapter(this, player.getStats());
            listViewInfo.setAdapter(adapter);
        }

        Button btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(view -> finish());
    }
}