package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.MatchUtility;
import com.example.myapplication.Utility.PlayerUtility;
import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.TournamentUtility;

public class ActivityPopUp extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up);

        if (getWindow() != null) {
            getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.90), WindowManager.LayoutParams.WRAP_CONTENT);
        }

        PopUpType type = PopUpType.valueOf(String.valueOf(getIntent().getExtras().get("pop_up_type")));

        TextView txtTitle = findViewById(R.id.txtTitle);
        TextView txtSubtitle1 = findViewById(R.id.txtSubtitle1);
        TextView txtSubtitle2 = findViewById(R.id.txtSubtitle2);
        Button btnConfirm = findViewById(R.id.btnConfirm);

        switch (type) {
            case DELETE_PLAYER: {
                Player player = PlayerUtility.getPlayerByKey(getIntent().getExtras().getString("player_key"));
                txtSubtitle1.setText(player.name);
                txtSubtitle2.setText(player.getSurnameOrNickname());
                if (player.getSurnameOrNickname().isEmpty()) txtSubtitle2.setVisibility(View.GONE);
                txtTitle.setText(R.string.delete_player);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_danger));
                btnConfirm.setOnClickListener(view -> { PlayerUtility.deletePlayer(player.key); finish(); });
                break;
            }
            case ARCHIVE_PLAYER: {
                Player player = PlayerUtility.getPlayerByKey(getIntent().getExtras().getString("player_key"));
                txtSubtitle1.setText(player.name);
                txtSubtitle2.setText(player.getSurnameOrNickname());
                if (player.getSurnameOrNickname().isEmpty()) txtSubtitle2.setVisibility(View.GONE);
                txtTitle.setText(R.string.archive_player);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_warning));
                btnConfirm.setOnClickListener(view -> { PlayerUtility.archivePlayer(player.key); finish(); });
                break;
            }
            case UNARCHIVE_PLAYER: {
                Player player = PlayerUtility.getPlayerByKey(getIntent().getExtras().getString("player_key"));
                txtSubtitle1.setText(player.name);
                txtSubtitle2.setText(player.getSurnameOrNickname());
                if (player.getSurnameOrNickname().isEmpty()) txtSubtitle2.setVisibility(View.GONE);
                txtTitle.setText(R.string.unarchive_player);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_warning));
                btnConfirm.setOnClickListener(view -> { PlayerUtility.unarchivePlayer(player.key); finish(); });
                break;
            }
            case DELETE_TOURNAMENT: {
                String tournamentKey = getIntent().getExtras().getString("tournament_key");
                Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
                txtSubtitle1.setText(tournament.name);
                txtSubtitle2.setVisibility(View.GONE);
                if (tournament.name.isEmpty()) txtSubtitle1.setVisibility(View.GONE);
                txtTitle.setText(R.string.delete_tournament);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_danger));
                btnConfirm.setOnClickListener(view -> {
                    TournamentUtility.deleteTournament(tournamentKey);
                    TournamentAdapter.tournamentAdapter.notifyDataSetChanged();
                    finish();
                });
                break;
            }
            case ACTIVATE_TOURNAMENT: {
                String tournamentKey = getIntent().getExtras().getString("tournament_key");
                Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
                txtSubtitle1.setText(tournament.name);
                txtSubtitle2.setVisibility(View.GONE);
                if (tournament.name.isEmpty()) txtSubtitle1.setVisibility(View.GONE);
                txtTitle.setText(R.string.activate_tournament);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_confirm));
                btnConfirm.setOnClickListener(view -> {
                    TournamentUtility.setActiveTournament(tournamentKey);
                    TournamentAdapter.tournamentAdapter.notifyDataSetChanged();
                    ActivityPopUpManageTournament.updateButtons();
                    finish();
                });
                break;
            }
            case DEACTIVATE_TOURNAMENT: {
                String tournamentKey = getIntent().getExtras().getString("tournament_key");
                Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
                txtSubtitle1.setText(tournament.name);
                txtSubtitle2.setVisibility(View.GONE);
                if (tournament.name.isEmpty()) txtSubtitle1.setVisibility(View.GONE);
                txtTitle.setText(R.string.deactivate_tournament);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_warning));
                btnConfirm.setOnClickListener(view -> {
                    TournamentUtility.deactivateAllTournaments();
                    TournamentAdapter.tournamentAdapter.notifyDataSetChanged();
                    ActivityPopUpManageTournament.updateButtons();
                    finish();
                });
                break;
            }
            case DELETE_TEAM: {
                String teamKey = getIntent().getExtras().getString("team_key");
                Team team = TournamentTeamUtility.getTeamByKey(teamKey);
                txtTitle.setText(R.string.delete_team);
                txtSubtitle1.setText(team.toStringNameAndSurname());
                txtSubtitle1.setTextSize(15);
                txtSubtitle2.setVisibility(View.GONE);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_danger));
                btnConfirm.setOnClickListener(view -> { TournamentTeamUtility.deleteTeam(teamKey); finish(); });
                break;
            }
            case DELETE_MATCH: {
                String matchKey = getIntent().getExtras().getString("match_key");
                String tournamentKey = getIntent().getExtras().getString("tournament_key");
                Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
                Match match = tournament.getMatchByKey(matchKey);
                txtTitle.setText(R.string.delete_match);
                txtSubtitle1.setText(getString(R.string.team_1) + ": " + (match.keyTeam1.equals("TO DO") ? "TO DO" : TournamentTeamUtility.getTeamByKey(match.keyTeam1).toStringNameAndSurname()));
                txtSubtitle2.setText(getString(R.string.team_2) + ": " + (match.keyTeam2.equals("TO DO") ? "TO DO" : TournamentTeamUtility.getTeamByKey(match.keyTeam2).toStringNameAndSurname()));
                txtSubtitle1.setTextSize(15);
                txtSubtitle2.setTextSize(15);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_danger));
                btnConfirm.setOnClickListener(view -> { MatchUtility.deleteMatch(tournamentKey, matchKey); finish(); });
                break;
            }
            case DELETE_EVERY_MATCH: {
                String tournamentKey = getIntent().getExtras().getString("tournament_key");
                Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
                txtTitle.setText(R.string.delete_every_match);
                txtSubtitle1.setText(tournament.name);
                txtSubtitle2.setVisibility(View.GONE);
                btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.button_danger));
                btnConfirm.setOnClickListener(view -> { MatchUtility.deleteEveryMatch(tournamentKey); finish(); });
                break;
            }
        }

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> finish());
    }
}