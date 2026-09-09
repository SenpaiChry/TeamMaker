package com.example.myapplication.Utility;

import static com.example.myapplication.Model.Constants.dbRoot;

import com.example.myapplication.Team;
import com.example.myapplication.Tournament;
import com.example.myapplication.TournamentActivityManageTeams;
import com.example.myapplication.TournamentActivityManageTournaments;
import com.example.myapplication.Model.Constants;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

                FirebaseWriteHelper.attach(null, "deleteTeam", dbRef.removeValue());

                return;
            }
        }
    }

    public static void editTeamsTournament(Tournament tournament) {
        // Mappa completa via TeamMapper: setValue sostituisce il nodo per intero,
        // così i playerN residui (es. player4 quando la squadra passa da 4 a 3) spariscono.
        for (Team team : tournament.teams) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(dbRoot + "tournaments/" + tournament.key + "/teams/" + team.key);
            FirebaseWriteHelper.attach(null, "editTeamsTournament", dbRef.setValue(TeamMapper.toMap(team)));
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

        // Scrittura atomica del nodo (era una sequenza di setValue distinte per ogni player)
        FirebaseWriteHelper.attach(null, "addTeamToTournament", dbRef.setValue(TeamMapper.toMap(newTeam)));
    }
}
