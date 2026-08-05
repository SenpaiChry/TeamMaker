package com.example.myapplication;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.TopToast.TopToast;
import com.example.myapplication.Utility.LiveMatchUtility;
import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;
import com.example.myapplication.Model.Constants;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    static MainActivity mainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseDatabase.getInstance().setPersistenceEnabled(false);

        setContentView(R.layout.activity_main);
        mainActivity = this;

        PlayerUtility.downloadPlayers();

        if (dbRoot.equals("teammakerStaging/")) {
            findViewById(R.id.txtDB).setVisibility(View.VISIBLE);
        }

        if (!Utility.isNetworkAvailable(this)) {
            TopToast.show(this,
                    "Nessuna connessione",
                    "Errore nella connessione a internet",
                    TopToast.MessageType.DANGER);
        }

        // ---- Tasto LIVE: appare/scompare in base al nodo live_match su Firebase ----
        Button btnOpenLive = findViewById(R.id.btnOpenLive);
        btnOpenLive.setVisibility(View.GONE);

        // Pulsazione del tasto LIVE
        ObjectAnimator pulse = ObjectAnimator.ofFloat(btnOpenLive, "alpha", 1f, 0.5f);
        pulse.setDuration(900);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);

        LiveMatchUtility.startListening((active, snapshot) -> {
            if (active) {
                btnOpenLive.setVisibility(View.VISIBLE);
                if (!pulse.isStarted()) pulse.start();
            } else {
                btnOpenLive.setVisibility(View.GONE);
                if (pulse.isStarted()) pulse.cancel();
                btnOpenLive.setAlpha(1f);
            }
        });

        btnOpenLive.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityLiveMatch.class);
            startActivity(intent);
        });

        // ---- Bottoni esistenti ----
        Button btnOpenGenerate = findViewById(R.id.btnOpenGenerate);
        btnOpenGenerate.setOnClickListener(v -> {
            if (Constants.downloadEnd) {
                Intent intent = new Intent(this, ActivityGenerate.class);
                intent.putExtra("GENERATE_FOR", "TEMPORARY");
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.downloading_data, Toast.LENGTH_SHORT).show();
            }
        });

        Button btnOpenTournament = findViewById(R.id.btnOpenTournament);
        btnOpenTournament.setOnClickListener(v -> {
            if (Constants.downloadEnd) {
                if (TournamentUtility.getActiveTournament() != null) {
                    Intent intent = new Intent(this, TournamentActivityTeamsBracketsTable.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.no_tournaments, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.downloading_data, Toast.LENGTH_SHORT).show();
            }
        });

        Button btnOpenScorecard = findViewById(R.id.btnOpenScorecard);
        btnOpenScorecard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityScorecard.class);
            startActivity(intent);
        });

        Button btnOpenLogin = findViewById(R.id.btnOpenLogin);
        btnOpenLogin.setOnClickListener(v -> {
            Intent intent;
            if (Constants.logged) {
                if (Constants.downloadEnd) {
                    intent = new Intent(this, TournamentActivityManage.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.downloading_data, Toast.LENGTH_SHORT).show();
                }
            } else {
                intent = new Intent(this, ActivityLogin.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveMatchUtility.stopListening();
        PlayerUtility.removePlayersListener();
        TournamentUtility.removeTournamentsListener();
    }
}