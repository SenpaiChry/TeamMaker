package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.example.myapplication.TournamentActivityManageTeams;
import com.example.myapplication.TournamentActivityManageTournaments;
import com.example.myapplication.Model.Constants;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class TournamentTeamUtility {

    public static void saveBracketForTeams(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/teams/");

        for (Team team : tournament.teams) {
            DatabaseReference dbRefTeam = dbRef.child(team.key + "/");
            dbRefTeam.child("bracket").setValue(team.bracket);
        }
    }

    public static void deleteTeam(String teamKey) {
        Team team = getTeamByKey(teamKey);

        for (Tournament tournament : Constants.tournaments) {
            if (tournament.teams.contains(team)) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/teams/" + team.key);

                tournament.teams.remove(team);
                TournamentActivityManageTeams.tournamentModifyTeamsAdapter.notifyDataSetChanged();
                TournamentActivityManageTournaments.reloadTournaments();

                dbRef.removeValue().addOnSuccessListener(aVoid -> { }).addOnFailureListener(e -> { });

                return;
            }
        }
    }

    public static void editTeamsTournament(Tournament tournament) {
        for (Team team : tournament.teams) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/teams/" + team.key);

            // Mappa completa del nodo: setValue lo sostituisce per intero, così i
            // playerN residui (es. player4 quando la squadra passa da 4 a 3) spariscono.
            Map<String, Object> teamData = new HashMap<>();
            teamData.put("bracket", team.bracket);

            for (int i = 0; i < team.players.size(); i ++) {
                teamData.put("player" + (i + 1), team.players.get(i).key);
            }

            dbRef.setValue(teamData);
        }
    }

    public static Team getTeamByKey(String key) {
        for (Tournament tournament : Constants.tournaments) {
            for (Team team : tournament.teams) {
                if (team.key.equals(key)) {
                    return team;
                }
            }
        }

        return null;
    }

    public static void addTeamToTournament(String tournamentKey, Team newTeam) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        String key = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/teams/").push().getKey();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/teams/" + key);

        newTeam.key = key;
        tournament.teams.add(newTeam);
        TournamentActivityManageTeams.notifyDataChange();

        for (int i = 0; i < newTeam.players.size(); i++) {
            dbRef.child("player" + (i + 1)).setValue(newTeam.players.get(i).key);
        }
    }
}
