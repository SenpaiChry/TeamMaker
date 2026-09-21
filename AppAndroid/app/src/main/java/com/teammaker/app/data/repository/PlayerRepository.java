package com.teammaker.app.data.repository;


import com.teammaker.app.auth.AdminAuth;
import static com.teammaker.app.data.AppConfig.DB_ROOT;

import android.util.Log;

import androidx.annotation.NonNull;

import com.teammaker.app.data.model.Player;
import com.teammaker.app.data.model.PlayerStats;
import com.teammaker.app.data.model.StatDefinition;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.util.NetworkUtils;
import com.teammaker.app.data.AppConfig;
import com.teammaker.app.data.InputSanitizer;

public class PlayerRepository {

    /** Cache in memoria della lista giocatori (sincronizzata dal listener realtime). */
    private static final ArrayList<Player> players = new ArrayList<>();

    /** Vista mutabile della cache: chi la modifica lo fa a suo rischio. */
    public static ArrayList<Player> getAll() { return players; }

    /** Listener persistente: si stacca con removePlayersListener() */
    private static ValueEventListener playersListener;
    private static DatabaseReference playersRef;

    /** Lunghezza massima per name / surname / nickname del giocatore. */
    private static final int PLAYER_TEXT_MAX = 40;

    public static void addPlayer(Player player, NetworkUtils.FirebaseCallback callback) {
        if (!AdminAuth.requireAdmin()) return;
        String key = FirebaseDatabase.getInstance().getReference(DB_ROOT + "players/").push().getKey();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "players/" + key);

        player.key = key;
        player.name     = InputSanitizer.clean(player.name,     PLAYER_TEXT_MAX);
        player.surname  = InputSanitizer.clean(player.surname,  PLAYER_TEXT_MAX);
        player.nickname = InputSanitizer.clean(player.nickname, PLAYER_TEXT_MAX);
        // Non aggiorniamo la lista locale: il listener in tempo reale se ne occupa

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", player.name);
        playerData.put("surname", player.surname);
        playerData.put("nickname", player.nickname);
        playerData.put("gender", player.gender);
        playerData.put("is_active", player.isActive);

        Map<String, Object> statsMap = new HashMap<>();
        for (StatDefinition def : StatsRepository.getDefinitions()) {
            statsMap.put(def.key, player.stats.get(def.key));
        }
        playerData.put("stats", statsMap);

        // Bonus: scrivo solo le stat con bonus attivo per compattezza
        Map<String, Object> bonusMap = new HashMap<>();
        for (Map.Entry<String, Boolean> e : player.bonus.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) bonusMap.put(e.getKey(), true);
        }
        playerData.put("bonus", bonusMap);

        dbRef.updateChildren(playerData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("Firebase", "Giocatore aggiunto con successo!");
                callback.onComplete(true);
            } else {
                Log.e("Firebase", "Errore nel salvataggio", task.getException());
                callback.onComplete(false);
            }
        });
    }

    public static void addEditPlayer(Player playerChanged, String playerKey) {
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "players/" + playerChanged.key);

        playerChanged.name     = InputSanitizer.clean(playerChanged.name,     PLAYER_TEXT_MAX);
        playerChanged.surname  = InputSanitizer.clean(playerChanged.surname,  PLAYER_TEXT_MAX);
        playerChanged.nickname = InputSanitizer.clean(playerChanged.nickname, PLAYER_TEXT_MAX);

        dbRef.child("name").setValue(playerChanged.name);
        dbRef.child("surname").setValue(playerChanged.surname);
        dbRef.child("nickname").setValue(playerChanged.nickname);
        dbRef.child("gender").setValue(playerChanged.gender);
        dbRef.child("is_active").setValue(playerChanged.isActive);

        for (StatDefinition def : StatsRepository.getDefinitions()) {
            dbRef.child("stats").child(def.key).setValue(playerChanged.stats.get(def.key));
        }

        // Riscrivo il nodo bonus per intero (setValue con la mappa completa: cosi'
        // eventuali flag disattivati vengono rimossi dal DB, niente residui).
        Map<String, Object> bonusMap = new HashMap<>();
        for (Map.Entry<String, Boolean> e : playerChanged.bonus.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) bonusMap.put(e.getKey(), true);
        }
        dbRef.child("bonus").setValue(bonusMap);

        // Aggiorna anche i riferimenti nei tornei
        for (Tournament tournament : TournamentRepository.getAll()) {
            for (Team team : tournament.teams) {
                for (int i = 0; i < team.players.size(); i++) {
                    if (team.players.get(i).key.equals(playerChanged.key)) {
                        team.players.get(i).name = playerChanged.name;
                        DatabaseReference dbRefTournament = FirebaseDatabase.getInstance()
                                .getReference(DB_ROOT + "tournaments/" + tournament.key + "/teams/" + team.key);
                        dbRefTournament.child("player" + (i + 1)).setValue(playerChanged.key);
                    }
                }
            }
        }
    }

    public static void deletePlayer(String playerKey) {
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "players/" + playerKey);
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d("Firebase", "Giocatore eliminato: " + playerKey);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Errore eliminazione giocatore", e);
        });
    }

    public static void archivePlayer(String playerKey) {
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "players/" + playerKey);
        dbRef.child("is_active").setValue(false);
    }

    public static void unarchivePlayer(String playerKey) {
        if (!AdminAuth.requireAdmin()) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "players/" + playerKey);
        dbRef.child("is_active").setValue(true);
    }

    /**
     * Ascolta i giocatori in TEMPO REALE: ogni modifica su Firebase
     * (da qualsiasi dispositivo) aggiorna automaticamente la lista locale.
     */
    public static void downloadPlayers() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        playersRef = firebaseDatabase.getReference(DB_ROOT + "players/");

        playersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PlayerRepository.getAll().clear();

                if (!snapshot.exists()) {
                    // Primo avvio: avvia anche il listener dei tornei
                    if (!TournamentRepository.isDataReady()) {
                        TournamentRepository.downloadTournaments();
                    }
                    return;
                }

                for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                    // Conversione grezzo Firebase -> PlayerStats tipizzato in un solo posto.
                    HashMap<String, Object> rawStats =
                            (HashMap<String, Object>) playerSnapshot.child("stats").getValue();
                    PlayerStats stats = PlayerStats.fromRawMap(rawStats);

                    Player player = new Player(
                            String.valueOf(playerSnapshot.getKey()),
                            String.valueOf(playerSnapshot.child("name").getValue(String.class)),
                            playerSnapshot.child("surname").getValue(String.class) != null
                                    ? String.valueOf(playerSnapshot.child("surname").getValue(String.class)) : "",
                            playerSnapshot.child("nickname").getValue(String.class) != null
                                    ? String.valueOf(playerSnapshot.child("nickname").getValue(String.class)) : "",
                            String.valueOf(playerSnapshot.child("gender").getValue(String.class)),
                            playerSnapshot.child("is_active").getValue(boolean.class),
                            stats);
                    // Bonus per-stat: assente = nessun bonus, retrocompatibile
                    for (DataSnapshot b : playerSnapshot.child("bonus").getChildren()) {
                        Boolean v = b.getValue(Boolean.class);
                        if (Boolean.TRUE.equals(v)) player.bonus.put(b.getKey(), true);
                    }
                    PlayerRepository.getAll().add(player);
                }

                Collections.sort(PlayerRepository.getAll());

                // Primo avvio: avvia il listener dei tornei
                if (!TournamentRepository.isDataReady()) {
                    TournamentRepository.downloadTournaments();
                }

                // Notifica chi mostra la lista giocatori (Activity visibile lo raccoglie)
                DataChangeBus.emit(DataChangeBus.Event.PLAYERS);

                Log.d("Firebase", "Players aggiornati in tempo reale: " + PlayerRepository.getAll().size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Errore listener players", error.toException());
            }
        };

        playersRef.addValueEventListener(playersListener);
    }

    /** Rimuove il listener in tempo reale (da chiamare in onDestroy della MainActivity). */
    public static void removePlayersListener() {
        if (playersRef != null && playersListener != null) {
            playersRef.removeEventListener(playersListener);
            playersListener = null;
        }
    }

    public static ArrayList<Player> getPlayersActive(boolean active) {
        ArrayList<Player> players = new ArrayList<>();
        for (Player p : PlayerRepository.getAll()) {
            if (p.isActive == active) {
                players.add(p);
            }
        }
        return players;
    }

    public static Player getPlayerByKey(String key) {
        for (Player p : PlayerRepository.getAll()) {
            if (p.key.equals(key)) {
                return p;
            }
        }
        return null;
    }
}