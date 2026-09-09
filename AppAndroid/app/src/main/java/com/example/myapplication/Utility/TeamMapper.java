package com.example.myapplication.Utility;

import com.example.myapplication.Team;
import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Conversione Team <-> Firebase in un solo posto.
 * Il nodo team ha: "bracket" + "player1..playerN" (chiavi dei giocatori).
 */
public class TeamMapper {

    public static Team fromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        Team team = new Team();
        team.key = snapshot.getKey();
        team.bracket = snapshot.child("bracket").getValue(String.class);

        // I giocatori sono in player1, player2, ... (bracket incluso nel count)
        for (int i = 1; i < snapshot.getChildrenCount() + 1; i++) {
            team.addPlayerByKey(snapshot.child("player" + i).getValue(String.class));
        }
        return team;
    }

    /**
     * Serializza una Team nel formato Firebase. setValue con questa mappa sostituisce
     * il nodo per intero, così eventuali playerN residui (es. quando una squadra
     * passa da 4 a 3 giocatori) spariscono senza chiamate esplicite di removeValue.
     */
    public static Map<String, Object> toMap(Team team) {
        Map<String, Object> data = new HashMap<>();
        data.put("bracket", team.bracket != null ? team.bracket : "");
        for (int i = 0; i < team.players.size(); i++) {
            data.put("player" + (i + 1), team.players.get(i).key);
        }
        return data;
    }
}
