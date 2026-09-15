package com.teammaker.app.data.repository;

import static com.teammaker.app.data.AppConfig.DB_ROOT;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.teammaker.app.bus.DataChangeBus;
import com.teammaker.app.data.firebase.FirebaseWriteHelper;
import com.teammaker.app.data.mapper.TeamMapper;
import com.teammaker.app.data.AppConfig;

public class TeamRepository {

    public static void saveBracketForTeams(String tournamentKey) {
        Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key + "/teams/");

        for (Team team : tournament.teams) {
            DatabaseReference dbRefTeam = dbRef.child(team.key + "/");
            dbRefTeam.child("bracket").setValue(team.bracket);
        }
    }

    public static void deleteTeam(String teamKey) {
        Team team = getTeamByKey(teamKey);

        for (Tournament tournament : TournamentRepository.getAll()) {
            if (tournament.teams.contains(team)) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key + "/teams/" + team.key);

                tournament.teams.remove(team);
                DataChangeBus.emit(DataChangeBus.Event.TEAMS);
                DataChangeBus.emit(DataChangeBus.Event.TOURNAMENTS);

                FirebaseWriteHelper.attach(null, "deleteTeam", dbRef.removeValue());

                return;
            }
        }
    }

    public static void editTeamsTournament(Tournament tournament) {
        // Mappa completa via TeamMapper: setValue sostituisce il nodo per intero,
        // così i playerN residui (es. player4 quando la squadra passa da 4 a 3) spariscono.
        for (Team team : tournament.teams) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key + "/teams/" + team.key);
            FirebaseWriteHelper.attach(null, "editTeamsTournament", dbRef.setValue(TeamMapper.toMap(team)));
        }
    }

    public static Team getTeamByKey(String key) {
        for (Tournament tournament : TournamentRepository.getAll()) {
            for (Team team : tournament.teams) {
                if (team.key.equals(key)) {
                    return team;
                }
            }
        }

        return null;
    }

    public static void addTeamToTournament(String tournamentKey, Team newTeam) {
        Tournament tournament = TournamentRepository.getTournamentByKey(tournamentKey);

        String key = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key + "/teams/").push().getKey();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(DB_ROOT + "tournaments/" + tournament.key + "/teams/" + key);

        newTeam.key = key;
        tournament.teams.add(newTeam);
        DataChangeBus.emit(DataChangeBus.Event.TEAMS);

        // Scrittura atomica del nodo (era una sequenza di setValue distinte per ogni player)
        FirebaseWriteHelper.attach(null, "addTeamToTournament", dbRef.setValue(TeamMapper.toMap(newTeam)));
    }
}
