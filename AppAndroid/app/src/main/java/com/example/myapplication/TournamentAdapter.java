package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.Model.Constants;

import java.util.ArrayList;
import java.util.Collections;

public class TournamentAdapter extends BaseAdapter {

    static TournamentAdapter tournamentAdapter;
    private final Context context;
    private final ArrayList<Tournament> tournaments;

    public TournamentAdapter(Context context) {
        this.context = context;
        this.tournaments = Constants.tournaments;

        Collections.sort(tournaments, (t1, t2) -> {
            if (t1.date == null && t2.date == null) return 0;
            if (t1.date == null) return 1;
            if (t2.date == null) return -1;
            return t2.date.getTime().compareTo(t1.date.getTime());
        });
    }

    @Override
    public int getCount() { return tournaments.size(); }

    @Override
    public Tournament getItem(int position) { return tournaments.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tournament_layout_tournaments, parent, false);
        }

        tournamentAdapter = this;
        Tournament tournament = getItem(position);

        LinearLayout llTournament = convertView.findViewById(R.id.llTournament);
        llTournament.setBackground(ContextCompat.getDrawable(context,
                tournament.isValid ? R.drawable.bg_list_card_highlight : R.drawable.bg_list_card));

        ImageView btnDelete = convertView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            Intent intent = new Intent(TournamentActivityManageTournaments.tournamentActivityManageTournaments.getApplicationContext(), ActivityPopUp.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_TOURNAMENT);
            TournamentActivityManageTournaments.tournamentActivityManageTournaments.startActivity(intent);
        });

        ImageView btnEdit = convertView.findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(TournamentActivityManageTournaments.tournamentActivityManageTournaments.getApplicationContext(), ActivityPopUpManageTournament.class);
            intent.putExtra("tournament_key", tournament.key);
            TournamentActivityManageTournaments.tournamentActivityManageTournaments.startActivity(intent);
        });

        TextView txtTournamentN = convertView.findViewById(R.id.txtTournamentN);
        if (tournament.name == null || tournament.name.isEmpty()) {
            txtTournamentN.setText(context.getString(R.string.tournament) + " " + (position + 1));
        } else {
            txtTournamentN.setText(tournament.name);
        }

        // Colore titolo: ciano se attivo, bianco se no
        txtTournamentN.setTextColor(ContextCompat.getColor(context,
                tournament.isValid ? R.color.list_highlight_text : R.color.list_text_primary));

        LinearLayout llTeamsPlayers = convertView.findViewById(R.id.llTeamsPlayers);
        llTeamsPlayers.removeAllViews();

        for (Team team : tournament.teams) {
            View teamItem = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_teams_short, parent, false);
            TextView txtPlayers = teamItem.findViewById(R.id.txtPlayers);
            txtPlayers.setText(team.toStringNameAndSurname());
            llTeamsPlayers.addView(teamItem);
        }

        return convertView;
    }
}