package com.teammaker.app;

import android.app.Application;
import android.content.Context;

import com.google.firebase.database.FirebaseDatabase;
import com.teammaker.app.data.repository.PlayerRepository;
import com.teammaker.app.data.repository.StatsRepository;
import com.teammaker.app.ui.common.SystemBarsPaddingCallback;

/**
 * Classe Application: e' il primo pezzo di codice che Android istanzia quando
 * parte il processo, PRIMA di qualsiasi Activity. E' l'unico posto sicuro per
 * chiamare {@link FirebaseDatabase#setPersistenceEnabled(boolean)}, che deve
 * essere la primissima interazione con Firebase e crasha se viene invocata
 * dopo che qualcun altro ha gia' toccato l'istanza.
 *
 * Anche i listener globali (stats, players, tournaments) vengono attaccati qui:
 * cosi' vivono per tutta la durata del processo e sopravvivono al ciclo di
 * vita delle Activity (rotazione, back stack, deep link su una schermata
 * diversa dalla MainActivity).
 */
public class TeamMakerApplication extends Application {

    private static Context appContext;

    /**
     * Application context globale, utile alle NetworkUtils per mostrare Toast
     * senza dover conoscere l'Activity corrente. Non usarlo mai come Context
     * di un componente UI (Dialog, inflater di layout non tema-aware, ecc.).
     */
    public static Context getAppContext() { return appContext; }

    @Override
    public void onCreate() {
        super.onCreate();

        appContext = getApplicationContext();

        // Padding automatico status bar / minimo nav bar per ogni Activity (edge-to-edge
        // di target 36). Vedi SystemBarsPaddingCallback.
        registerActivityLifecycleCallbacks(new SystemBarsPaddingCallback());

        // Deve stare qui (non in MainActivity): setPersistenceEnabled crasha
        // se chiamato dopo che qualcuno ha gia' fatto FirebaseDatabase.getInstance().
        FirebaseDatabase.getInstance().setPersistenceEnabled(false);

        // Catalogo statistiche PRIMA dei player (getVote() lo usa).
        StatsRepository.startListening();

        // A cascata: PlayerRepository.downloadPlayers() a fine caricamento
        // chiama TournamentRepository.downloadTournaments().
        PlayerRepository.downloadPlayers();
    }
}
