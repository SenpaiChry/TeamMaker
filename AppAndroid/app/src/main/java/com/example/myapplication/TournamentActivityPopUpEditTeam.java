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

        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        MaxHeightScrollView scrollContent = findViewById(R.id.scrollContent);
        float density = getResources().getDisplayMetrics().density;
        scrollContent.setMaxHeight(getResources().getDisplayMetrics().heightPixels - (int) (200 * density));

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

            bindSpinner(spinnerPlayer1);
            bindSpinner(spinnerPlayer2);
            bindSpinner(spinnerPlayer3);
            bindSpinner(spinnerPlayer4);
            bindSpinner(spinnerPlayer5);

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

            bindSpinner(spinnerPlayer1);
            bindSpinner(spinnerPlayer2);
            bindSpinner(spinnerPlayer3);
            bindSpinner(spinnerPlayer4);
            bindSpinner(spinnerPlayer5);

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

    /** Un adapter per spinner, così la tendina evidenzia il giocatore scelto di QUELLO spinner. */
    private void bindSpinner(Spinner spinner) {
        TournamentSpinnerTeamAdapter adapter = new TournamentSpinnerTeamAdapter(this, playersString);
        spinner.setAdapter(adapter);
        adapter.setSpinner(spinner);
    }

    private void prepareArraysEdit(Tournament tournament) {
        int nCharSurname = 20;
//        TODO int nCharSurname = getIntent().getExtras().getInt("nCharSurname");

        playersString.clear();
        playersEntity.clear();
        playersString.add("---");
        playersEntity.add(null);

        // Deduplica per player.key (non per Nome+Cognome: due omonimi devono comparire entrambi).
        Set<String> keysSeen = new HashSet<>();

        // all players in tournament
        for (Team team : tournament.teams) {
            for (Player player : team.players) {
                if (player != null && player.key != null && keysSeen.add(player.key)) {
                    playersString.add(player.getNameAndSurname(nCharSurname));
                    playersEntity.add(player);
                }
            }
        }

        // + all players active
        for (Player player : PlayerUtility.getPlayersActive(true)) {
            if (player.key != null && keysSeen.add(player.key)) {
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

        // Set delle key gia' prese in altre squadre del torneo (non piu' Nome+Cognome).
        Set<String> takenKeys = new HashSet<>();
        for (Team team : tournament.teams) {
            for (Player player : team.players) {
                if (player != null && player.key != null) takenKeys.add(player.key);
            }
        }

        for (Player player : PlayerUtility.getPlayersActive(true)) {
            if (player.key != null && !takenKeys.contains(player.key)) {
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

    /** Cerca per player.key: cosi' due omonimi non collidono nella selezione dello spinner. */
    private int getIndexByPlayer(Player player) {
        if (player == null || player.key == null) return 0;
        for (int i = 0; i < playersEntity.size(); i++) {
            Player p = playersEntity.get(i);
            if (p != null && player.key.equals(p.key)) {
                return i;
            }
        }
        return 0;
    }

    private boolean checkEditedTeam(Tournament tournament, Team teamEdited, Team teamOriginal) {
        if (teamEdited.players.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_player, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Duplicati: uso player.key invece di equals (che sarebbe identita' di riferimento).
        Set<String> seenKeys = new HashSet<>();
        for (Player player : teamEdited.players) {
            if (player != null && player.key != null) {
                if (!seenKeys.add(player.key)) {
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

            // Se editedPlayer e' gia' in un'altra squadra, va rimosso (o sostituito con l'originale).
            for (Team team : tournament.teams) {
                if (team.key.equals(teamOriginal.key)) continue;

                int indexInOtherTeam = indexOfByKey(team.players, editedPlayer.key);
                if (indexInOtherTeam >= 0) {
                    Player originalPlayer = (i < teamOriginal.players.size()) ? teamOriginal.players.get(i) : null;

                    if (originalPlayer != null && indexOfByKey(team.players, originalPlayer.key) < 0) {
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

    /** Indice del giocatore identificato dalla key, -1 se non trovato. */
    private static int indexOfByKey(ArrayList<Player> players, String key) {
        if (key == null) return -1;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p != null && key.equals(p.key)) return i;
        }
        return -1;
    }
}
