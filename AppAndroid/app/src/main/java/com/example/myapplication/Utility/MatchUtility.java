package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import com.example.myapplication.Match;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.example.myapplication.TournamentActivityManageMatches;
import com.example.myapplication.TournamentActivityManageTournaments;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchUtility {

    /** Costruisce il nodo "detail" (punti dei set) da salvare su Firebase. */
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
            String key = dbRef.push().getKey();
            match.key = key;

            Map<String, Object> matchData = new HashMap<>();
            matchData.put("day", match.day + "");
            matchData.put("time", match.time);
            matchData.put("team1", match.keyTeam1);
            matchData.put("team2", match.keyTeam2);
            matchData.put("points1", match.points1 + "");
            matchData.put("points2", match.points2 + "");
            matchData.put("type", match.type);
            matchData.put("detail", buildDetail(match));

            updates.put(key, matchData);
        }

        dbRef.updateChildren(updates);
    }

    public static void addNewMatch(String tournamentKey, Match match) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/matches/");

        match.key = dbRef.push().getKey();
        dbRef.child(match.key).child("day").setValue(match.day + "");
        dbRef.child(match.key).child("time").setValue(match.time);
        dbRef.child(match.key).child("team1").setValue(match.keyTeam1);
        dbRef.child(match.key).child("team2").setValue(match.keyTeam2);
        dbRef.child(match.key).child("points1").setValue(match.points1 + "");
        dbRef.child(match.key).child("points2").setValue(match.points2 + "");
        dbRef.child(match.key).child("type").setValue(match.type);
        dbRef.child(match.key).child("detail").setValue(buildDetail(match));

        tournament.matches.add(match);

        Collections.sort(tournament.matches, (m1, m2) -> {
            if (m1.day != m2.day) {
                return Integer.compare(m1.day, m2.day);
            } else {
                return Integer.compare(TimeUtility.toMinutes(m2.time), TimeUtility.toMinutes(m1.time));
            }
        });

        TournamentActivityManageMatches.tournamentBracketAdminAdapter.notifyDataSetChanged();
        TournamentActivityManageTournaments.reloadTournaments();
        TournamentActivityManageMatches.tournamentActivityManageMatches.recreate();
        Collections.reverse(tournament.matches);
    }

    public static void editMatch(String tournamentKey, Match match) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        for (int i = 0; i < tournament.matches.size(); i++) {
            if (tournament.matches.get(i).key.equals(match.key)) {
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/matches/" + tournament.matches.get(i).key);
                databaseReference.child("day").setValue(match.day + "");
                databaseReference.child("time").setValue(match.time);
                databaseReference.child("team1").setValue(match.keyTeam1);
                databaseReference.child("team2").setValue(match.keyTeam2);
                databaseReference.child("points1").setValue(match.points1 + "");
                databaseReference.child("points2").setValue(match.points2 + "");
                databaseReference.child("type").setValue(match.type);
                databaseReference.child("detail").setValue(buildDetail(match));

                tournament.matches.get(i).day = match.day;
                tournament.matches.get(i).time = match.time;
                tournament.matches.get(i).keyTeam1 = match.keyTeam1;
                tournament.matches.get(i).keyTeam2 = match.keyTeam2;
                tournament.matches.get(i).points1 = match.points1;
                tournament.matches.get(i).points2 = match.points2;
                tournament.matches.get(i).type = match.type;
                tournament.matches.get(i).detail = match.detail;

                Collections.sort(tournament.matches, (m1, m2) -> {
                    if (m1.day != m2.day) return Integer.compare(m1.day, m2.day);
                    return Integer.compare(TimeUtility.toMinutes(m1.time), TimeUtility.toMinutes(m2.time)); // ASC
                });

                TournamentActivityManageMatches.tournamentBracketAdminAdapter.refresh();

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

                dbRef.removeValue().addOnSuccessListener(aVoid -> { }).addOnFailureListener(e -> { });

                return;
            }
        }
    }

    public static void deleteEveryMatch(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournamentKey);
        dbRef.child("nBracket").setValue(0);
        DatabaseReference dbRefMatches = dbRef.child("/matches/");

        for (Team team : tournament.teams) {
            DatabaseReference dbRefTeams = dbRef.child("/teams/" + team.key);
            dbRefTeams.child("bracket").setValue("");

            team.bracket = "";
        }

        tournament.matches.clear();
        tournament.nBracket = 0;

        dbRefMatches.removeValue().addOnSuccessListener(aVoid -> { }).addOnFailureListener(e -> { });

        TournamentActivityManageMatches.tournamentBracketAdminAdapter.notifyDataSetChanged();
        TournamentActivityManageMatches.tournamentActivityManageMatches.recreate();
    }
}
