package com.example.myapplication;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.TopToast.TopToast;
import com.example.myapplication.Utility.AdminUtility;
import com.example.myapplication.Utility.LiveMatchUtility;
import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Utility.SoundUtility;
import com.example.myapplication.Utility.StatsUtility;
import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.UpdateUtility;
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

        // Il catalogo statistiche va acceso PRIMA dei player: getVote() lo usa.
        StatsUtility.startListening();
        PlayerUtility.downloadPlayers();

        // Firebase Auth persiste la sessione: se l'admin era gia' loggato in una
        // precedente esecuzione dell'app, ripristina il flag e permetti l'accesso
        // diretto senza riscrivere la password.
        Constants.logged = AdminUtility.isAdmin();

        if (dbRoot.equals("teammakerStaging/")) {
            findViewById(R.id.txtDB).setVisibility(View.VISIBLE);
        }

        if (!Utility.isNetworkAvailable(this)) {
            TopToast.show(this,
                    "Nessuna connessione",
                    "Errore nella connessione a internet",
                    TopToast.MessageType.DANGER);
        } else {
            // Controllo aggiornamenti in silenzio: nessun feedback se non c'e' nulla.
            UpdateUtility.checkForUpdate(this, true);
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

        // Easter egg: 5 tap sul logo -> fatass.mp3; press-hold 6-7s -> sixtyseven.mp3
        setupLogoEasterEggs();
    }

    // ---- Easter egg logo ----
    /** Reset del contatore tap se passa piu' di questo tempo tra un tap e l'altro. */
    private static final long TAP_RESET_MS = 1500L;
    private int logoTapCount = 0;
    private long lastLogoTapMs = 0L;
    private long logoPressDownMs = 0L;

    private void setupLogoEasterEggs() {
        ImageView imgLogo = findViewById(R.id.imgLogo);
        if (imgLogo == null) return;

        imgLogo.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    logoPressDownMs = SystemClock.uptimeMillis();
                    return false; // lascio arrivare il click normale
                case MotionEvent.ACTION_UP: {
                    long heldMs = SystemClock.uptimeMillis() - logoPressDownMs;
                    logoPressDownMs = 0L;
                    // Hold da 6 a 7 secondi: '67'
                    if (heldMs >= 6000L && heldMs <= 7000L) {
                        SoundUtility.play(this, R.raw.sixtyseven);
                        // Un hold non deve contare come tap
                        logoTapCount = 0;
                        return true;
                    }
                    return false;
                }
                case MotionEvent.ACTION_CANCEL:
                    logoPressDownMs = 0L;
                    return false;
                default:
                    return false;
            }
        });

        imgLogo.setOnClickListener(v -> {
            long now = SystemClock.uptimeMillis();
            if (now - lastLogoTapMs > TAP_RESET_MS) {
                logoTapCount = 0;
            }
            lastLogoTapMs = now;
            logoTapCount++;
            if (logoTapCount >= 5) {
                SoundUtility.play(this, R.raw.fatass);
                logoTapCount = 0;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LiveMatchUtility.stopListening();
        PlayerUtility.removePlayersListener();
        StatsUtility.stopListening();
        TournamentUtility.removeTournamentsListener();
    }
}