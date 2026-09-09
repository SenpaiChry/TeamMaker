package com.example.myapplication.Utility;

import com.example.myapplication.Match;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un solo posto per convertire una partita da/verso Firebase: se cambia il modello
 * (nuovi campi, formati diversi) si tocca solo qui, non nei quattro punti che prima
 * duplicavano il parsing. fromSnapshot() e' difensivo: numeri malformati diventano 0,
 * campi mancanti restano vuoti, il type viene normalizzato al codice canonico.
 */
public class MatchMapper {

    /** Costruisce una Match dal nodo Firebase. Restituisce null se la partita e' inutilizzabile. */
    public static Match fromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        Match match = new Match(
                snapshot.child("team1").getValue(String.class),
                snapshot.child("team2").getValue(String.class),
                parseIntOrZero(snapshot.child("day").getValue(String.class)),
                snapshot.child("time").getValue(String.class),
                parseIntOrZero(snapshot.child("points1").getValue(String.class)),
                parseIntOrZero(snapshot.child("points2").getValue(String.class))
        );
        match.key = snapshot.getKey();

        if (snapshot.hasChild("type")) {
            match.type = PhaseUtility.normalize(
                    String.valueOf(snapshot.child("type").getValue(String.class)));
        }

        String s1t = snapshot.child("source1_type").getValue(String.class);
        if (s1t != null && !s1t.isEmpty()) {
            match.source1Type = s1t;
            String s1r = snapshot.child("source1_ref").getValue(String.class);
            match.source1Ref = s1r != null ? s1r : "";
        }
        String s2t = snapshot.child("source2_type").getValue(String.class);
        if (s2t != null && !s2t.isEmpty()) {
            match.source2Type = s2t;
            String s2r = snapshot.child("source2_ref").getValue(String.class);
            match.source2Ref = s2r != null ? s2r : "";
        }

        for (DataSnapshot setSnap : snapshot.child("detail").getChildren()) {
            match.detail.add(new int[]{
                    parseIntOrZero(setSnap.child("points1").getValue(String.class)),
                    parseIntOrZero(setSnap.child("points2").getValue(String.class))
            });
        }

        return match;
    }

    /**
     * Serializza una Match nel formato Firebase (stringhe per i numeri, come nel resto
     * del DB). Le sorgenti si scrivono SOLO se presenti: le partite di girone/champions
     * non si sporcano di campi vuoti.
     */
    public static Map<String, Object> toMap(Match match) {
        Map<String, Object> data = new HashMap<>();
        data.put("day", match.day + "");
        data.put("time", match.time);
        data.put("team1", match.keyTeam1);
        data.put("team2", match.keyTeam2);
        data.put("points1", match.points1 + "");
        data.put("points2", match.points2 + "");
        data.put("type", match.type);
        data.put("detail", buildDetail(match));

        if (match.hasSource1()) {
            data.put("source1_type", match.source1Type);
            data.put("source1_ref", match.source1Ref);
        }
        if (match.hasSource2()) {
            data.put("source2_type", match.source2Type);
            data.put("source2_ref", match.source2Ref);
        }
        return data;
    }

    /** Nodo "detail" (punti dei set). Vuoto per le partite senza dettaglio. */
    private static List<Map<String, Object>> buildDetail(Match match) {
        List<Map<String, Object>> detailList = new ArrayList<>();
        for (int[] set : match.detail) {
            Map<String, Object> setData = new HashMap<>();
            setData.put("points1", set[0] + "");
            setData.put("points2", set[1] + "");
            detailList.add(setData);
        }
        return detailList;
    }

    private static int parseIntOrZero(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
