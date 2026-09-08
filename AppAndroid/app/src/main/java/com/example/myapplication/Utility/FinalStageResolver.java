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
 * Risolve i placeholder delle fasi finali: trasforma le sorgenti degli slot
 * (STANDING, GROUP_STANDING, WINNER, LOSER) nelle chiavi squadra reali quando
 * diventano note, riempiendo team1/team2.
 *
 * STANDING / GROUP_STANDING si risolvono solo a fase chiusa (posizioni congelate);
 * WINNER / LOSER appena la partita sorgente ha un risultato.
 */
public class FinalStageResolver {

    /** Chiave squadra per uno slot, oppure null se non ancora nota. */
    public static String resolveSlot(Tournament tournament, String type, String ref) {
        if (type == null || type.isEmpty() || ref == null || ref.isEmpty()) {
            return null;
        }

        switch (type) {
            case "STANDING":
                return isInitialPhaseComplete(tournament) ? standingTeam(tournament, tournament.teams, parseIntOrZero(ref)) : null;
            case "GROUP_STANDING":
                return isInitialPhaseComplete(tournament) ? groupStandingTeam(tournament, ref) : null;
            case "WINNER":
                return winnerOf(tournament, ref, true);
            case "LOSER":
                return winnerOf(tournament, ref, false);
            default:
                return null;
        }
    }

    /**
     * Riempie i team1/team2 risolvibili di tutte le partite e salva le modifiche in
     * un'unica scrittura. Ripete finche' emergono novita' (catene vincente->vincente).
     */
    public static void resolveAll(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        if (tournament == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Match match : tournament.matches) {
                if (match.hasSource1()) {
                    String key = resolveSlot(tournament, match.source1Type, match.source1Ref);
                    if (key != null && !key.equals(match.keyTeam1)) {
                        match.keyTeam1 = key;
                        updates.put(match.key + "/team1", key);
                        changed = true;
                    }
                }
                if (match.hasSource2()) {
                    String key = resolveSlot(tournament, match.source2Type, match.source2Ref);
                    if (key != null && !key.equals(match.keyTeam2)) {
                        match.keyTeam2 = key;
                        updates.put(match.key + "/team2", key);
                        changed = true;
                    }
                }
            }
        }

        if (!updates.isEmpty()) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance()
                    .getReference(dbRoot + "tournaments/" + tournament.key + "/matches/");
            dbRef.updateChildren(updates);
        }
    }

    /**
     * La fase iniziale è completa quando tutte le partite di girone/champions
     * (type GIRONE/BRACKET) hanno un esito: solo allora la classifica è definitiva
     * e le posizioni possono diventare squadre reali nelle fasi finali.
     */
    private static boolean isInitialPhaseComplete(Tournament tournament) {
        boolean hasGroupMatch = false;
        for (Match m : tournament.matches) {
            if (PhaseUtility.isGroup(m.type)) {
                hasGroupMatch = true;
                if (m.points1 == m.points2) {
                    return false; // una partita di girone ancora senza esito
                }
            }
        }
        return hasGroupMatch;
    }

    private static String standingTeam(Tournament tournament, List<Team> pool, int position) {
        List<StandingsUtility.TeamStanding> standings =
                StandingsUtility.computeStandings(pool, tournament.matches);
        if (position >= 1 && position <= standings.size()) {
            return standings.get(position - 1).team.key;
        }
        return null;
    }

    private static String groupStandingTeam(Tournament tournament, String ref) {
        // ref = lettera girone + posizione, es. "A1" (girone A, 1a classificata)
        int split = 0;
        while (split < ref.length() && !Character.isDigit(ref.charAt(split))) {
            split++;
        }
        if (split == 0 || split >= ref.length()) {
            return null;
        }

        String groupLetter = ref.substring(0, split);
        int position = parseIntOrZero(ref.substring(split));

        List<Team> groupTeams = new ArrayList<>();
        for (Team t : tournament.teams) {
            if (t.bracket != null && t.bracket.endsWith(groupLetter)) {
                groupTeams.add(t);
            }
        }
        return standingTeam(tournament, groupTeams, position);
    }

    private static String winnerOf(Tournament tournament, String matchKey, boolean wantWinner) {
        Match source = tournament.getMatchByKey(matchKey);
        if (source == null || source.points1 == source.points2) {
            return null; // partita inesistente o senza esito
        }
        boolean team1Won = source.points1 > source.points2;
        String key = (wantWinner == team1Won) ? source.keyTeam1 : source.keyTeam2;
        return (key != null && !key.equals("TO DO")) ? key : null;
    }

    private static int parseIntOrZero(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
