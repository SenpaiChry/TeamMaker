package com.teammaker.app.data.mapper;

import com.teammaker.app.data.model.Match;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.teammaker.app.domain.MatchPhases;

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
                MapperUtils.str(snapshot, "team1"),
                MapperUtils.str(snapshot, "team2"),
                MapperUtils.intOr(snapshot, "day", 0),
                MapperUtils.str(snapshot, "time"),
                MapperUtils.intOr(snapshot, "points1", 0),
                MapperUtils.intOr(snapshot, "points2", 0)
        );
        match.key = snapshot.getKey();

        if (snapshot.hasChild("type")) {
            match.type = MatchPhases.normalize(MapperUtils.str(snapshot, "type"));
        }

        String s1t = MapperUtils.str(snapshot, "source1_type");
        if (!s1t.isEmpty()) {
            match.source1Type = s1t;
            match.source1Ref = MapperUtils.str(snapshot, "source1_ref");
        }
        String s2t = MapperUtils.str(snapshot, "source2_type");
        if (!s2t.isEmpty()) {
            match.source2Type = s2t;
            match.source2Ref = MapperUtils.str(snapshot, "source2_ref");
        }

        for (DataSnapshot setSnap : snapshot.child("detail").getChildren()) {
            match.detail.add(new int[]{
                    MapperUtils.intOr(setSnap, "points1", 0),
                    MapperUtils.intOr(setSnap, "points2", 0)
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

}
