package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

import java.util.ArrayList;

public class TournamentActivityTeamsBracketsTable extends AppCompatActivity {

    private ArrayList<Team> teamsTooSee = new ArrayList<>();
    private ArrayList<Match> matchesTooSee = new ArrayList<>();
    private ListView listViewTeams;
    private ListView listViewBracket;
    private ListView listViewTable;
    private TournamentTeamsAdapter tournamentTeamsAdapter;
    private TournamentBracketAdapter tournamentBracketAdapter;
    private TournamentTableAdapter tournamentTableAdapter;
    private Tournament tournamentToShow;
    Button btnTeams;
    Button btnBracket;
    Button btnTable;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tournament_activity_teams_brackets_table);

        tournamentToShow = TournamentUtility.getActiveTournament();
        teamsTooSee = new ArrayList<>(tournamentToShow.teams);
        matchesTooSee = new ArrayList<>(tournamentToShow.matches);

        TextView txtTournament = findViewById(R.id.txtTournament);
        if (!tournamentToShow.name.isEmpty()) {
            txtTournament.setText(tournamentToShow.name);
        }

        listViewTeams = findViewById(R.id.listViewTeams);
        listViewBracket = findViewById(R.id.listViewBracket);
        listViewTable = findViewById(R.id.listViewTable);

        tournamentTeamsAdapter = new TournamentTeamsAdapter(teamsTooSee);
        listViewTeams.setAdapter(tournamentTeamsAdapter);

        tournamentBracketAdapter = new TournamentBracketAdapter(this, matchesTooSee);
        listViewBracket.setAdapter(tournamentBracketAdapter);

        tournamentTableAdapter = new TournamentTableAdapter(this, tournamentToShow);
        listViewTable.setAdapter(tournamentTableAdapter);

        btnTeams = findViewById(R.id.btnTeams);
        btnBracket = findViewById(R.id.btnBracket);
        btnTable = findViewById(R.id.btnTable);

        btnTeams.setOnClickListener(v -> switchAdapter("TEAMS"));
        btnBracket.setOnClickListener(v -> switchAdapter("BRACKET"));
        btnTable.setOnClickListener(v -> switchAdapter("TABLE"));

        ImageButton btnGoBack = findViewById(R.id.btnGoBack);
        btnGoBack.setOnClickListener(v -> finish());

        EditText txtSearch = findViewById(R.id.txtSearch);
        txtSearch.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP
                    && txtSearch.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                int clearWidth = txtSearch.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                if (event.getRawX() >= (txtSearch.getRight() - clearWidth)) {
                    txtSearch.setText("");
                    return true;
                }
            }
            return false;
        });

        txtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    /**
     * SQUADRE e PARTITE vengono filtrate (mostra solo cio' che corrisponde).
     * CLASSIFICA no: resta completa, ma le squadre che corrispondono vengono evidenziate.
     */
    private void applySearch(String query) {
        teamsTooSee.clear();
        matchesTooSee.clear();

        for (Team team : tournamentToShow.teams) {
            if (teamContainsPlayer(team, query)) {
                teamsTooSee.add(team);
            }
        }

        for (Match match : tournamentToShow.matches) {
            Team team1 = TournamentTeamUtility.getTeamByKey(match.keyTeam1);
            Team team2 = TournamentTeamUtility.getTeamByKey(match.keyTeam2);
            if (teamContainsPlayer(team1, query) || teamContainsPlayer(team2, query)) {
                matchesTooSee.add(match);
            }
        }

        tournamentTeamsAdapter.notifyDataSetChanged();
        tournamentBracketAdapter.setMatches(matchesTooSee);
        tournamentBracketAdapter.setHighlightQuery(query);
        tournamentTableAdapter.setHighlightQuery(query);
    }

    private boolean teamContainsPlayer(Team team, String query) {
        if (team == null || team.players == null) return false;
        for (Player player : team.players) {
            if (player.containsString(query)) return true;
        }
        return false;
    }

    private void switchAdapter(String mode) {
        listViewTeams.setVisibility(View.GONE);
        listViewBracket.setVisibility(View.GONE);
        listViewTable.setVisibility(View.GONE);

        btnTeams.setBackground(ContextCompat.getDrawable(this, R.drawable.buttons_main));
        btnBracket.setBackground(ContextCompat.getDrawable(this, R.drawable.buttons_main));
        btnTable.setBackground(ContextCompat.getDrawable(this, R.drawable.buttons_main));

        switch (mode) {
            case "TEAMS": {
                listViewTeams.setVisibility(View.VISIBLE);
                btnTeams.setBackground(ContextCompat.getDrawable(this, R.drawable.button_main_selected));
                break;
            }
            case "BRACKET": {
                listViewBracket.setVisibility(View.VISIBLE);
                btnBracket.setBackground(ContextCompat.getDrawable(this, R.drawable.button_main_selected));
                break;
            }
            case "TABLE": {
                listViewTable.setVisibility(View.VISIBLE);
                btnTable.setBackground(ContextCompat.getDrawable(this, R.drawable.button_main_selected));
                break;
            }
        }
    }
}