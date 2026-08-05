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

public class MatchUtility {

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

        tournament.matches.add(match);

        Collections.sort(tournament.matches, (m1, m2) -> {
            if (m1.day != m2.day) {
                return Integer.compare(m1.day, m2.day);
            } else {
                return m2.time.compareTo(m1.time);
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

                tournament.matches.get(i).day = match.day;
                tournament.matches.get(i).time = match.time;
                tournament.matches.get(i).keyTeam1 = match.keyTeam1;
                tournament.matches.get(i).keyTeam2 = match.keyTeam2;
                tournament.matches.get(i).points1 = match.points1;
                tournament.matches.get(i).points2 = match.points2;
                tournament.matches.get(i).type = match.type;

                Collections.sort(tournament.matches, (m1, m2) -> {
                    if (m1.day != m2.day) return Integer.compare(m1.day, m2.day);
                    return m1.time.compareTo(m2.time); // ASC
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
