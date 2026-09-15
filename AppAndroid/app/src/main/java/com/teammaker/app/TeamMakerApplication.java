package com.teammaker.app;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.teammaker.app.Utility.PlayerUtility;
import com.teammaker.app.Utility.StatsUtility;

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

    @Override
    public void onCreate() {
        super.onCreate();

        // Deve stare qui (non in MainActivity): setPersistenceEnabled crasha
        // se chiamato dopo che qualcuno ha gia' fatto FirebaseDatabase.getInstance().
        FirebaseDatabase.getInstance().setPersistenceEnabled(false);

        // Catalogo statistiche PRIMA dei player (getVote() lo usa).
        StatsUtility.startListening();

        // A cascata: PlayerUtility.downloadPlayers() a fine caricamento
        // chiama TournamentUtility.downloadTournaments().
        PlayerUtility.downloadPlayers();
    }
}
