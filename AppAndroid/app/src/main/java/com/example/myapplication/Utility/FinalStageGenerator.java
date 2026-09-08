package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import com.example.myapplication.Match;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera il tabellone a eliminazione diretta (quarti / semifinali / finale + 3°/4°)
 * come partite con sorgenti placeholder, senza ancora giorno e orario.
 *
 * Seeding unico per classifica unica e gironi: al seed 1..N si assegna una sorgente,
 * poi si applica l'ordine standard del tabellone (1 e 2 su lati opposti). Per i gironi
 * seed(girone g, posizione p) = p*G + g + 1, così le teste di serie dei gironi finiscono
 * su lati opposti e si evitano rivincite immediate (vedi report §4.7).
 */
public class FinalStageGenerator {

    public static final int QUARTER = 8, SEMI = 4, FINAL = 2;

    /** Dimensioni tabellone valide per questo torneo (per popolare la UI). */
    public static List<Integer> availableSizes(Tournament tournament) {
        List<Integer> sizes = new ArrayList<>();
        int nTeams = tournament.teams.size();
        boolean gironi = tournament.nBracket >= 2;

        for (int size : new int[]{QUARTER, SEMI, FINAL}) {
            if (gironi) {
                int g = tournament.nBracket;
                if (size % g == 0 && (size / g) <= minGironeSize(tournament) && (size / g) >= 1) {
                    sizes.add(size);
                }
            } else if (nTeams >= size) {
                sizes.add(size);
            }
        }
        return sizes;
    }

    /**
     * Costruisce le partite delle fasi finali. Le key sono pre-assegnate così i
     * WINNER/LOSER puntano alle partite corrette; vanno salvate con MatchUtility.saveMatches.
     * Le etichette di fase arrivano dalla UI (stringhe localizzate usate come type).
     */
    public static List<Match> generate(Tournament tournament, int bracketSize, boolean thirdPlace) {
        List<Match> all = new ArrayList<>();
        if (!availableSizes(tournament).contains(bracketSize)) {
            return all;
        }

        DatabaseReference matchesRef = FirebaseDatabase.getInstance()
                .getReference(dbRoot + "tournaments/" + tournament.key + "/matches/");
        int g = tournament.nBracket >= 2 ? tournament.nBracket : 1;
        boolean gironi = tournament.nBracket >= 2;

        // Primo turno: coppie dall'ordine standard del tabellone
        List<Integer> order = seedOrder(bracketSize);
        List<Match> round = new ArrayList<>();
        String firstType = labelForCount(bracketSize / 2);
        for (int i = 0; i < bracketSize; i += 2) {
            Match m = new Match("TO DO", "TO DO", 0, "0:00", 0, 0, firstType);
            setSeedSource(m, 1, order.get(i), gironi, g);
            setSeedSource(m, 2, order.get(i + 1), gironi, g);
            m.key = matchesRef.push().getKey();
            round.add(m);
            all.add(m);
        }

        // Turni successivi: vincente vs vincente, fino alla finale
        List<Match> semis = null;
        while (round.size() > 1) {
            if (round.size() == 2) {
                semis = round; // livello che alimenta la finale = semifinali
            }
            List<Match> next = new ArrayList<>();
            String type = labelForCount(round.size() / 2);
            for (int i = 0; i < round.size(); i += 2) {
                Match m = new Match("TO DO", "TO DO", 0, "0:00", 0, 0, type);
                m.source1Type = "WINNER"; m.source1Ref = round.get(i).key;
                m.source2Type = "WINNER"; m.source2Ref = round.get(i + 1).key;
                m.key = matchesRef.push().getKey();
                next.add(m);
                all.add(m);
            }
            round = next;
        }

        // Finale 3°/4° posto
        if (thirdPlace) {
            Match m = null;
            if (semis != null) {
                // Partenza da quarti/semi: perdenti delle semifinali
                m = new Match("TO DO", "TO DO", 0, "0:00", 0, 0, PhaseUtility.THIRD);
                m.source1Type = "LOSER"; m.source1Ref = semis.get(0).key;
                m.source2Type = "LOSER"; m.source2Ref = semis.get(1).key;
            } else if (bracketSize == FINAL && availableSizes(tournament).contains(SEMI)) {
                // Partenza dalla finale: 3a e 4a della classifica (seed 3 e 4)
                m = new Match("TO DO", "TO DO", 0, "0:00", 0, 0, PhaseUtility.THIRD);
                setSeedSource(m, 1, 3, gironi, g);
                setSeedSource(m, 2, 4, gironi, g);
            }
            if (m != null) {
                m.key = matchesRef.push().getKey();
                all.add(m);
            }
        }

        return all;
    }

    /** Sorgente di uno slot dal numero di seed (1..N). */
    private static void setSeedSource(Match m, int slot, int seed, boolean gironi, int g) {
        String type, ref;
        if (gironi) {
            int s0 = seed - 1;
            int gironeIndex = s0 % g;
            int position = s0 / g + 1;
            type = "GROUP_STANDING";
            ref = (char) ('A' + gironeIndex) + String.valueOf(position);
        } else {
            type = "STANDING";
            ref = String.valueOf(seed);
        }
        if (slot == 1) {
            m.source1Type = type; m.source1Ref = ref;
        } else {
            m.source2Type = type; m.source2Ref = ref;
        }
    }

    /** Ordine standard del tabellone: 1 e 2 su lati opposti (es. N=8 -> 1,8,4,5,2,7,3,6). */
    private static List<Integer> seedOrder(int n) {
        List<Integer> order = new ArrayList<>();
        order.add(1);
        for (int size = 2; size <= n; size *= 2) {
            List<Integer> next = new ArrayList<>();
            for (int seed : order) {
                next.add(seed);
                next.add(size + 1 - seed);
            }
            order = next;
        }
        return order;
    }

    private static String labelForCount(int matchCount) {
        if (matchCount >= 4) return PhaseUtility.QUARTER;
        if (matchCount == 2) return PhaseUtility.SEMIFINAL;
        return PhaseUtility.FINAL;
    }

    private static int minGironeSize(Tournament tournament) {
        Map<String, Integer> counts = new HashMap<>();
        for (Team t : tournament.teams) {
            String b = t.bracket != null ? t.bracket : "";
            counts.put(b, counts.getOrDefault(b, 0) + 1);
        }
        int min = Integer.MAX_VALUE;
        for (int c : counts.values()) {
            min = Math.min(min, c);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }
}
