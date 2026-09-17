package com.teammaker.app.data.mapper;

import com.teammaker.app.data.model.Team;
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
        team.bracket = MapperUtils.str(snapshot, "bracket");

        // Itero solo i figli con chiave "playerN": prima usavamo getChildrenCount()
        // che comprendeva anche il nodo "bracket", con due bug conseguenti — se
        // bracket mancava si perdeva l'ultimo giocatore, se comparivano campi extra
        // finivano passati ad addPlayerByKey come chiavi. Iteriamo direttamente.
        for (DataSnapshot child : snapshot.getChildren()) {
            String childKey = child.getKey();
            if (childKey != null && childKey.matches("^player\\d+$")) {
                team.addPlayerByKey(child.getValue(String.class));
            }
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
