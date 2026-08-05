package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.ActivityAdmin;
import com.example.myapplication.Player;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.example.myapplication.Model.Constants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerUtility {

    /** Listener persistente: si stacca con removePlayersListener() */
    private static ValueEventListener playersListener;
    private static DatabaseReference playersRef;

    public static void addPlayer(Player player, Utility.FirebaseCallback callback) {
        String key = FirebaseDatabase.getInstance().getReference(dbRoot + "players/").push().getKey();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "players/" + key);

        player.key = key;
        // Non aggiorniamo la lista locale: il listener in tempo reale se ne occupa

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("name", player.name);
        playerData.put("surname", player.surname);
        playerData.put("nickname", player.nickname);
        playerData.put("gender", player.gender);
        playerData.put("is_active", player.isActive);

        Map<String, Object> statsMap = new HashMap<>();
        for (int i = 0; i < Constants.statsDescription.length; i++) {
            statsMap.put(Constants.statsDescriptionEng[i], player.stats.get(Constants.statsDescriptionEng[i]));
        }
        playerData.put("stats", statsMap);

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
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "players/" + playerChanged.key);

        dbRef.child("name").setValue(playerChanged.name);
        dbRef.child("surname").setValue(playerChanged.surname);
        dbRef.child("nickname").setValue(playerChanged.nickname);
        dbRef.child("gender").setValue(playerChanged.gender);
        dbRef.child("is_active").setValue(playerChanged.isActive);

        for (int i = 0; i < Constants.statsDescription.length; i++) {
            dbRef.child("stats").child(Constants.statsDescriptionEng[i])
                    .setValue(playerChanged.stats.get(Constants.statsDescriptionEng[i]));
        }

        // Aggiorna anche i riferimenti nei tornei
        for (Tournament tournament : Constants.tournaments) {
            for (Team team : tournament.teams) {
                for (int i = 0; i < team.players.size(); i++) {
                    if (team.players.get(i).key.equals(playerChanged.key)) {
                        team.players.get(i).name = playerChanged.name;
                        DatabaseReference dbRefTournament = FirebaseDatabase.getInstance()
                                .getReference(dbRoot + "tournaments/" + tournament.key + "/teams/" + team.key);
                        dbRefTournament.child("player" + (i + 1)).setValue(playerChanged.key);
                    }
                }
            }
        }
    }

    public static void deletePlayer(String playerKey) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "players/" + playerKey);
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d("Firebase", "Giocatore eliminato: " + playerKey);
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Errore eliminazione giocatore", e);
        });
    }

    public static void archivePlayer(String playerKey) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "players/" + playerKey);
        dbRef.child("is_active").setValue(false);
    }

    public static void unarchivePlayer(String playerKey) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "players/" + playerKey);
        dbRef.child("is_active").setValue(true);
    }

    /**
     * Ascolta i giocatori in TEMPO REALE: ogni modifica su Firebase
     * (da qualsiasi dispositivo) aggiorna automaticamente la lista locale.
     */
    public static void downloadPlayers() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        playersRef = firebaseDatabase.getReference(dbRoot + "players/");

        playersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Constants.players.clear();

                if (!snapshot.exists()) {
                    // Primo avvio: avvia anche il listener dei tornei
                    if (!Constants.downloadEnd) {
                        TournamentUtility.downloadTournaments();
                    }
                    return;
                }

                for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                    Constants.players.add(new Player(
                            String.valueOf(playerSnapshot.getKey()),
                            String.valueOf(playerSnapshot.child("name").getValue(String.class)),
                            playerSnapshot.child("surname").getValue(String.class) != null
                                    ? String.valueOf(playerSnapshot.child("surname").getValue(String.class)) : "",
                            playerSnapshot.child("nickname").getValue(String.class) != null
                                    ? String.valueOf(playerSnapshot.child("nickname").getValue(String.class)) : "",
                            String.valueOf(playerSnapshot.child("gender").getValue(String.class)),
                            playerSnapshot.child("is_active").getValue(boolean.class),
                            (HashMap<String, Object>) playerSnapshot.child("stats").getValue()));
                }

                Collections.sort(Constants.players);

                // Primo avvio: avvia il listener dei tornei
                if (!Constants.downloadEnd) {
                    TournamentUtility.downloadTournaments();
                }

                // Aggiorna la lista admin se aperta
                if (ActivityAdmin.playerAdminAdapter != null) {
                    ActivityAdmin.reloadPlayers();
                }

                Log.d("Firebase", "Players aggiornati in tempo reale: " + Constants.players.size());
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
        for (Player p : Constants.players) {
            if (p.isActive == active) {
                players.add(p);
            }
        }

        if (ActivityAdmin.playerAdminAdapter != null) {
            ActivityAdmin.playerAdminAdapter.notifyDataSetChanged();
        }

        return players;
    }

    public static Player getPlayerByKey(String key) {
        for (Player p : Constants.players) {
            if (p.key.equals(key)) {
                return p;
            }
        }
        return null;
    }
}