package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestisce il nodo "live_match" su Firebase.
 * Il segnapunti scrive ogni N secondi; gli altri dispositivi ascoltano.
 */
public class LiveMatchUtility {

    private static final String LIVE_NODE = dbRoot + "live_match";
    private static ValueEventListener liveListener;
    private static DatabaseReference liveRef;

    // ---- Scrittura (dal segnapunti) ----

    /** Scrive lo stato corrente della partita nel nodo live. */
    public static void writeLiveMatch(String tournamentKey, int matchPosition,
                                      String team1Name, String team2Name,
                                      String team1Players, String team2Players,
                                      int points1, int points2,
                                      int sets1, int sets2,
                                      boolean swapped) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIVE_NODE);

        Map<String, Object> data = new HashMap<>();
        data.put("active", true);
        data.put("tournament_key", tournamentKey);
        data.put("match_position", matchPosition);
        data.put("team1_name", team1Name);
        data.put("team2_name", team2Name);
        data.put("team1_players", team1Players);
        data.put("team2_players", team2Players);
        data.put("points1", points1);
        data.put("points2", points2);
        data.put("sets1", sets1);
        data.put("sets2", sets2);
        data.put("swapped", swapped); // cambio campo: la diretta inverte i lati
        data.put("timestamp", System.currentTimeMillis());

        ref.updateChildren(data);
    }

    /**
     * Chiede a Firebase di rimettere "active" a false in automatico quando il
     * client si disconnette (crash, kill del processo, rete persa): onDestroy()
     * non è garantito, così si evita una diretta fantasma sempre attiva.
     */
    public static void registerOnDisconnect() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIVE_NODE);
        ref.child("active").onDisconnect().setValue(false);
    }

    /** Segna la partita come non più live (quando il segnapunti si chiude). */
    public static void clearLiveMatch() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(LIVE_NODE);
        ref.child("active").onDisconnect().cancel();
        ref.child("active").setValue(false);
    }

    // ---- Lettura (dalla home e dalla schermata live) ----

    /** Callback per chi ascolta il nodo live. */
    public interface LiveMatchCallback {
        void onLiveMatchChanged(boolean active, DataSnapshot snapshot);
    }

    /** Inizia ad ascoltare il nodo live in tempo reale. */
    public static void startListening(LiveMatchCallback callback) {
        liveRef = FirebaseDatabase.getInstance().getReference(LIVE_NODE);

        liveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean active = snapshot.exists()
                        && snapshot.child("active").getValue(Boolean.class) != null
                        && Boolean.TRUE.equals(snapshot.child("active").getValue(Boolean.class));
                callback.onLiveMatchChanged(active, snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LiveMatch", "Errore listener live", error.toException());
            }
        };

        liveRef.addValueEventListener(liveListener);
    }

    /** Ferma l'ascolto. */
    public static void stopListening() {
        if (liveRef != null && liveListener != null) {
            liveRef.removeEventListener(liveListener);
            liveListener = null;
        }
    }
}