package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import com.example.myapplication.Match;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.example.myapplication.TournamentActivityManageMatches;
import com.example.myapplication.TournamentActivityManageTournaments;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchUtility {

    /**
     * Ordine cronologico delle partite: prima per giorno, poi per orario (convertito
     * in minuti — il confronto stringa sbaglia con "9:30" vs "10:00"). Usare SEMPRE
     * questo comparatore per non avere logiche di ordinamento divergenti nel codice.
     */
    public static final Comparator<Match> BY_DAY_TIME = (m1, m2) -> {
        if (m1.day != m2.day) return Integer.compare(m1.day, m2.day);
        return Integer.compare(TimeUtility.toMinutes(m1.time), TimeUtility.toMinutes(m2.time));
    };

    /**
     * Salva più partite in un'unica scrittura atomica: ogni nodo partita viene
     * scritto completo (mai a metà) con updateChildren. Usato dalla generazione
     * calendario, al posto di tanti addNewMatch in loop (fragili e frammentati).
     */
    public static void saveMatches(String tournamentKey, List<Match> matches) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/matches/");

        Map<String, Object> updates = new HashMap<>();
        for (Match match : matches) {
            // Riusa la key se gia' assegnata (fasi finali: i WINNER/LOSER puntano a
            // partite di questo stesso batch), altrimenti ne genera una nuova.
            String key = (match.key != null && !match.key.isEmpty()) ? match.key : dbRef.push().getKey();
            match.key = key;
            updates.put(key, MatchMapper.toMap(match));
        }

        FirebaseWriteHelper.attach(
                TournamentActivityManageMatches.tournamentActivityManageMatches,
                "saveMatches",
                dbRef.updateChildren(updates));
    }

    public static void addNewMatch(String tournamentKey, Match match) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/matches/");

        match.key = dbRef.push().getKey();
        // Scrittura atomica del nodo completo (era una sequenza di 8 setValue distinte)
        FirebaseWriteHelper.attach(
                TournamentActivityManageMatches.tournamentActivityManageMatches,
                "addNewMatch",
                dbRef.child(match.key).setValue(MatchMapper.toMap(match)));

        tournament.matches.add(match);
        Collections.sort(tournament.matches, BY_DAY_TIME);

        TournamentActivityManageMatches.tournamentBracketAdminAdapter.notifyDataSetChanged();
        TournamentActivityManageTournaments.reloadTournaments();
        TournamentActivityManageMatches.tournamentActivityManageMatches.recreate();
    }

    public static void editMatch(String tournamentKey, Match match) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        for (int i = 0; i < tournament.matches.size(); i++) {
            if (tournament.matches.get(i).key.equals(match.key)) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/matches/" + tournament.matches.get(i).key);
                // Scrittura atomica del nodo (era una sequenza di 8 setValue distinte)
                FirebaseWriteHelper.attach(
                        TournamentActivityManageMatches.tournamentActivityManageMatches,
                        "editMatch",
                        databaseReference.setValue(MatchMapper.toMap(match)));

                tournament.matches.get(i).day = match.day;
                tournament.matches.get(i).time = match.time;
                tournament.matches.get(i).keyTeam1 = match.keyTeam1;
                tournament.matches.get(i).keyTeam2 = match.keyTeam2;
                tournament.matches.get(i).points1 = match.points1;
                tournament.matches.get(i).points2 = match.points2;
                tournament.matches.get(i).type = match.type;
                tournament.matches.get(i).detail = match.detail;

                Collections.sort(tournament.matches, BY_DAY_TIME);

                TournamentActivityManageMatches.tournamentBracketAdminAdapter.refresh();

                // Propaga il vincente/perdente alle fasi finali che dipendono da questa partita
                FinalStageResolver.resolveAll(tournamentKey);

                return;
            }
        }
    }

    public static void deleteMatch(String tournamentKey, String matchKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        for (Match match : tournament.matches) {
            if (match.key.equals(matchKey)) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournamentKey + "/matches/" + match.key);

                tournament.matches.remove(match);
                TournamentActivityManageMatches.tournamentBracketAdminAdapter.notifyDataSetChanged();

                FirebaseWriteHelper.attach(
                        TournamentActivityManageMatches.tournamentActivityManageMatches,
                        "deleteMatch",
                        dbRef.removeValue());

                return;
            }
        }
    }

    public static void deleteEveryMatch(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        // Reset atomico: nBracket=0, tutte le partite via, bracket delle squadre svuotato
        Map<String, Object> updates = new HashMap<>();
        updates.put("nBracket", 0);
        updates.put("matches", null);
        for (Team team : tournament.teams) {
            updates.put("teams/" + team.key + "/bracket", "");
            team.bracket = "";
        }

        tournament.matches.clear();
        tournament.nBracket = 0;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournamentKey);
        FirebaseWriteHelper.attach(
                TournamentActivityManageMatches.tournamentActivityManageMatches,
                "deleteEveryMatch",
                dbRef.updateChildren(updates));

        TournamentActivityManageMatches.tournamentBracketAdminAdapter.notifyDataSetChanged();
        TournamentActivityManageMatches.tournamentActivityManageMatches.recreate();
    }
}
