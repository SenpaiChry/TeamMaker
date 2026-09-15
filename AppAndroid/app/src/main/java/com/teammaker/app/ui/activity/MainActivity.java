package com.teammaker.app.ui.activity;

import static com.teammaker.app.data.model.Constants.dbRoot;

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

import com.teammaker.app.ui.common.TopToast;
import com.teammaker.app.auth.AdminAuth;
import com.teammaker.app.domain.LiveMatchTimer;
import com.teammaker.app.util.Sound;
import com.teammaker.app.data.repository.TournamentRepository;
import com.teammaker.app.update.AppUpdater;
import com.teammaker.app.util.NetworkUtils;
import com.teammaker.app.data.model.Constants;
import com.teammaker.app.TeamMakerApplication;
import com.teammaker.app.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Init Firebase globale (persistenza off + listener stats/players/tournaments)
        // vive in TeamMakerApplication: parte prima di qualsiasi Activity e resta su
        // per tutta la durata del processo.

        // Firebase Auth persiste la sessione: se l'admin era gia' loggato in una
        // precedente esecuzione dell'app, ripristina il flag e permetti l'accesso
        // diretto senza riscrivere la password.
        Constants.logged = AdminAuth.isAdmin();

        if (dbRoot.equals("teammakerStaging/")) {
            findViewById(R.id.txtDB).setVisibility(View.VISIBLE);
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            TopToast.show(this,
                    "Nessuna connessione",
                    "Errore nella connessione a internet",
                    TopToast.MessageType.DANGER);
        } else {
            // Controllo aggiornamenti in silenzio: nessun feedback se non c'e' nulla.
            AppUpdater.checkForUpdate(this, true);
        }

        // ---- Tasto LIVE: appare/scompare in base al nodo live_match su Firebase ----
        Button btnOpenLive = findViewById(R.id.btnOpenLive);
        btnOpenLive.setVisibility(View.GONE);

        // Pulsazione del tasto LIVE
        ObjectAnimator pulse = ObjectAnimator.ofFloat(btnOpenLive, "alpha", 1f, 0.5f);
        pulse.setDuration(900);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);

        LiveMatchTimer.startListening((active, snapshot) -> {
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
            Intent intent = new Intent(this, LiveMatchActivity.class);
            startActivity(intent);
        });

        // ---- Bottoni esistenti ----
        Button btnOpenGenerate = findViewById(R.id.btnOpenGenerate);
        btnOpenGenerate.setOnClickListener(v -> {
            if (Constants.downloadEnd) {
                Intent intent = new Intent(this, GenerateActivity.class);
                intent.putExtra("GENERATE_FOR", "TEMPORARY");
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.downloading_data, Toast.LENGTH_SHORT).show();
            }
        });

        Button btnOpenTournament = findViewById(R.id.btnOpenTournament);
        btnOpenTournament.setOnClickListener(v -> {
            if (Constants.downloadEnd) {
                if (TournamentRepository.getActiveTournament() != null) {
                    Intent intent = new Intent(this, TeamsBracketsTableActivity.class);
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
            Intent intent = new Intent(this, ScorecardActivity.class);
            startActivity(intent);
        });

        Button btnOpenLogin = findViewById(R.id.btnOpenLogin);
        btnOpenLogin.setOnClickListener(v -> {
            Intent intent;
            if (Constants.logged) {
                if (Constants.downloadEnd) {
                    intent = new Intent(this, ManageTournamentActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.downloading_data, Toast.LENGTH_SHORT).show();
                }
            } else {
                intent = new Intent(this, LoginActivity.class);
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
                        Sound.play(this, R.raw.sixtyseven);
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
                Sound.play(this, R.raw.fatass);
                logoTapCount = 0;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Solo il listener LiveMatch e' locale a questa Activity (aggiorna btnOpenLive).
        // Gli altri (stats, players, tournaments) vivono in TeamMakerApplication e non
        // vanno staccati qui: il ciclo di vita del processo li chiude gia' da solo.
        LiveMatchTimer.stopListening();
    }
}