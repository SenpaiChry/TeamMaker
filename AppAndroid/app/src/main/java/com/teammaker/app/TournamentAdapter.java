package com.teammaker.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.Model.Constants;

import java.util.ArrayList;
import java.util.Collections;

public class TournamentAdapter extends RecyclerView.Adapter<TournamentAdapter.ViewHolder> {

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
    public int getItemCount() { return tournaments.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tournament_layout_tournaments, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        tournamentAdapter = this;
        Tournament tournament = tournaments.get(position);

        h.llTournament.setBackground(ContextCompat.getDrawable(context,
                tournament.isValid ? R.drawable.bg_list_card_highlight : R.drawable.bg_list_card));

        // Torneo bloccato: nascondo il 🗑 (l'eliminazione avviene solo dopo lo sblocco).
        h.btnDelete.setVisibility(tournament.locked ? View.GONE : View.VISIBLE);
        h.btnDelete.setOnClickListener(v -> {
            Intent intent = new Intent(TournamentActivityManageTournaments.tournamentActivityManageTournaments.getApplicationContext(), ActivityPopUp.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_TOURNAMENT);
            TournamentActivityManageTournaments.tournamentActivityManageTournaments.startActivity(intent);
        });

        h.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(TournamentActivityManageTournaments.tournamentActivityManageTournaments.getApplicationContext(), ActivityPopUpManageTournament.class);
            intent.putExtra("tournament_key", tournament.key);
            TournamentActivityManageTournaments.tournamentActivityManageTournaments.startActivity(intent);
        });

        if (tournament.name == null || tournament.name.isEmpty()) {
            h.txtTournamentN.setText(context.getString(R.string.tournament) + " " + (position + 1));
        } else {
            h.txtTournamentN.setText(tournament.name);
        }

        // Colore titolo: ciano se attivo, bianco se no
        h.txtTournamentN.setTextColor(ContextCompat.getColor(context,
                tournament.isValid ? R.color.list_highlight_text : R.color.list_text_primary));

        h.llTeamsPlayers.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(h.llTeamsPlayers.getContext());
        for (Team team : tournament.teams) {
            View teamItem = inflater.inflate(R.layout.layout_teams_short, h.llTeamsPlayers, false);
            TextView txtPlayers = teamItem.findViewById(R.id.txtPlayers);
            txtPlayers.setText(team.toStringNameAndSurname());
            h.llTeamsPlayers.addView(teamItem);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout llTournament;
        final LinearLayout llTeamsPlayers;
        final ImageView btnDelete;
        final ImageView btnEdit;
        final TextView txtTournamentN;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            llTournament = itemView.findViewById(R.id.llTournament);
            llTeamsPlayers = itemView.findViewById(R.id.llTeamsPlayers);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            txtTournamentN = itemView.findViewById(R.id.txtTournamentN);
        }
    }
}
