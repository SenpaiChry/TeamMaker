package com.teammaker.app.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.data.repository.PlayerRepository;
import com.teammaker.app.util.NetworkUtils;
import com.teammaker.app.ui.common.VerticalSpacingItemDecoration;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.ui.adapter.PlayerInfoAdapter;
import com.teammaker.app.R;

public class InfoPlayerActivity extends AppCompatActivity {

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
            Player player = PlayerRepository.getPlayerByKey(getIntent().getExtras().get("player_key").toString());

            RecyclerView listViewInfo = findViewById(R.id.listInfo);
            listViewInfo.setLayoutManager(new LinearLayoutManager(this));
            listViewInfo.addItemDecoration(new VerticalSpacingItemDecoration(this, 4));
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

            PlayerInfoAdapter adapter = new PlayerInfoAdapter(this, player.getStats(), player.bonus);
            listViewInfo.setAdapter(adapter);
        }

        Button btnOk = findViewById(R.id.btnOk);
        btnOk.setOnClickListener(view -> finish());
    }
}