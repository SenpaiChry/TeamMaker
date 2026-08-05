package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.TournamentUtility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TournamentActivityPopUpEditTeam extends AppCompatActivity {
    private ArrayList<String> playersString = new ArrayList<>();
    private ArrayList<Player> playersEntity = new ArrayList<>();
    private ArrayList<String> playersTaken = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_edit_team);

        Tournament tournament = TournamentUtility.getTournamentByKey(getIntent().getExtras().getString("tournament_key"));

        TextView txtTitle = findViewById(R.id.txtTitle);
        Spinner spinnerPlayer1 = findViewById(R.id.spinnerPlayer1);
        Spinner spinnerPlayer2 = findViewById(R.id.spinnerPlayer2);
        Spinner spinnerPlayer3 = findViewById(R.id.spinnerPlayer3);
        Spinner spinnerPlayer4 = findViewById(R.id.spinnerPlayer4);
        Spinner spinnerPlayer5 = findViewById(R.id.spinnerPlayer5);

        Button btnConfirm = findViewById(R.id.btnConfirm);

        if (getIntent().getExtras().getString("team_key").equals("null")) { // NEW
            prepareArraysCreate(tournament);

            txtTitle.setText(R.string.new_squad);

            TournamentSpinnerTeamAdapter adapterTeam = new TournamentSpinnerTeamAdapter(this, playersString);
            spinnerPlayer1.setAdapter(adapterTeam);
            spinnerPlayer2.setAdapter(adapterTeam);
            spinnerPlayer3.setAdapter(adapterTeam);
            spinnerPlayer4.setAdapter(adapterTeam);
            spinnerPlayer5.setAdapter(adapterTeam);

            btnConfirm.setOnClickListener(view -> {
                Team team = new Team();
                if (spinnerPlayer1.getSelectedItemPosition() != 0)
                    team.players.add(playersEntity.get(spinnerPlayer1.getSelectedItemPosition()));
                if (spinnerPlayer2.getSelectedItemPosition() != 0)
                    team.players.add(playersEntity.get(spinnerPlayer2.getSelectedItemPosition()));
                if (spinnerPlayer3.getSelectedItemPosition() != 0)
                    team.players.add(playersEntity.get(spinnerPlayer3.getSelectedItemPosition()));
                if (spinnerPlayer4.getSelectedItemPosition() != 0)
                    team.players.add(playersEntity.get(spinnerPlayer4.getSelectedItemPosition()));
                if (spinnerPlayer5.getSelectedItemPosition() != 0)
                    team.players.add(playersEntity.get(spinnerPlayer5.getSelectedItemPosition()));

                if (!team.players.isEmpty()) {
                    TournamentTeamUtility.addTeamToTournament(tournament.key, team);
                    finish();
                } else {
                    Toast.makeText(this, R.string.select_at_least_one_player, Toast.LENGTH_SHORT).show();
                }
            });
        }
        else // EDIT
        {
            prepareArraysEdit(tournament);

            txtTitle.setText(R.string.edit_squad);

            TournamentSpinnerTeamAdapter adapterTeam = new TournamentSpinnerTeamAdapter(this, playersString);
            spinnerPlayer1.setAdapter(adapterTeam);
            spinnerPlayer2.setAdapter(adapterTeam);
            spinnerPlayer3.setAdapter(adapterTeam);
            spinnerPlayer4.setAdapter(adapterTeam);
            spinnerPlayer5.setAdapter(adapterTeam);

            Team teamOriginal = TournamentTeamUtility.getTeamByKey(getIntent().getExtras().getString("team_key"));

            spinnerPlayer1.setSelection(getIndexByPlayerOrDefault(teamOriginal, 0), false);
            spinnerPlayer2.setSelection(getIndexByPlayerOrDefault(teamOriginal, 1), false);
            spinnerPlayer3.setSelection(getIndexByPlayerOrDefault(teamOriginal, 2), false);
            spinnerPlayer4.setSelection(getIndexByPlayerOrDefault(teamOriginal, 3), false);
            spinnerPlayer5.setSelection(getIndexByPlayerOrDefault(teamOriginal, 4), false);

            btnConfirm.setOnClickListener(view -> {
                Team teamEdited = new Team();
                if (spinnerPlayer1.getSelectedItemPosition() != 0)
                    teamEdited.players.add(playersEntity.get(spinnerPlayer1.getSelectedItemPosition()));
                if (spinnerPlayer2.getSelectedItemPosition() != 0)
                    teamEdited.players.add(playersEntity.get(spinnerPlayer2.getSelectedItemPosition()));
                if (spinnerPlayer3.getSelectedItemPosition() != 0)
                    teamEdited.players.add(playersEntity.get(spinnerPlayer3.getSelectedItemPosition()));
                if (spinnerPlayer4.getSelectedItemPosition() != 0)
                    teamEdited.players.add(playersEntity.get(spinnerPlayer4.getSelectedItemPosition()));
                if (spinnerPlayer5.getSelectedItemPosition() != 0)
                    teamEdited.players.add(playersEntity.get(spinnerPlayer5.getSelectedItemPosition()));

                if (checkEditedTeam(tournament, teamEdited, teamOriginal)) {
                    TournamentTeamUtility.editTeamsTournament(tournament);
                    TournamentActivityManageTeams.notifyDataChange();
                    TournamentActivityManageTournaments.reloadTournaments();

                    finish();
                }
            });
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> finish());
    }

    private void prepareArraysEdit(Tournament tournament) {
        int nCharSurname = 20;
//        TODO int nCharSurname = getIntent().getExtras().getInt("nCharSurname");

        playersString.clear();
        playersEntity.clear();
        playersString.add("---");
        playersEntity.add(null);

        // all players in tournament
        for (Team team : tournament.teams) {
            for (Player player : team.players) {
                playersString.add(player.getNameAndSurname(nCharSurname));
                playersEntity.add(player);
            }
        }

        // + all players active
        for (Player player : PlayerUtility.getPlayersActive(true)) {
            if (!playersString.contains(player.getNameAndSurname(nCharSurname))) {
                playersString.add(player.getNameAndSurname(nCharSurname));
                playersEntity.add(player);
            }
        }
    }

    private void prepareArraysCreate(Tournament tournament) {
        int nCharSurname = 20;
//        TODO int nCharSurname = getIntent().getExtras().getInt("nCharSurname");

        playersString.clear();
        playersEntity.clear();
        playersString.add("---");
        playersEntity.add(null);

        for (Team team : tournament.teams) {
            for (Player player : team.players) {
                playersTaken.add(player.getNameAndSurname(nCharSurname));
            }
        }

        for (Player player : PlayerUtility.getPlayersActive(true)) {
            if (!playersTaken.contains(player.getNameAndSurname(nCharSurname))) {
                playersString.add(player.getNameAndSurname(nCharSurname));
                playersEntity.add(player);
            }
        }
    }

    private int getIndexByPlayerOrDefault(Team team, int index) {
        if (team.players.size() > index && team.players.get(index) != null) {
            return getIndexByPlayer(team.players.get(index));
        }
        return 0;
    }

    private int getIndexByPlayer(Player player) {
        int nCharSurname = 20;
//        TODO int nCharSurname = getIntent().getExtras().getInt("nCharSurname");

        String playerString = PlayerUtility.getPlayerByKey(player.key).getNameAndSurname(nCharSurname);

        for (int i = 0; i < playersString.size(); i ++){
            if (playersString.get(i).equals(playerString)) {
                return  i;
            }
        }

        return 0;
    }

    private boolean checkEditedTeam(Tournament tournament, Team teamEdited, Team teamOriginal) {
        if (teamEdited.players.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_player, Toast.LENGTH_SHORT).show();
            return false;
        }

        Set<Player> seenPlayers = new HashSet<>();
        for (Player player : teamEdited.players) {
            if (player != null) {
                if (!seenPlayers.add(player)) {
                    Toast.makeText(this, R.string.same_player_selected_twice, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }

        ArrayList<Player> newPlayers = new ArrayList<>();

        for (int i = 0; i < teamEdited.players.size(); i++) {
            Player editedPlayer = teamEdited.players.get(i);

            if (editedPlayer == null) {
                continue;
            }

            for (Team team : tournament.teams) {
                if (team.key.equals(teamOriginal.key)) continue;

                if (team.players.contains(editedPlayer)) {
                    Player originalPlayer = (i < teamOriginal.players.size()) ? teamOriginal.players.get(i) : null;

                    int indexInOtherTeam = team.players.indexOf(editedPlayer);

                    if (originalPlayer != null && !team.players.contains(originalPlayer)) {
                        team.players.set(indexInOtherTeam, originalPlayer);
                    } else {
                        team.players.remove(indexInOtherTeam);
                    }

                    break;
                }
            }

            newPlayers.add(editedPlayer);
        }
        teamOriginal.players = newPlayers;

        return true;
    }
}
