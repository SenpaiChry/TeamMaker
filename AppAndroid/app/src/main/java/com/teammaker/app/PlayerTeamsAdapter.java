package com.teammaker.app;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.Model.Constants;

import java.util.ArrayList;

public class PlayerTeamsAdapter extends RecyclerView.Adapter<PlayerTeamsAdapter.ViewHolder> {

    private final String type;

    public PlayerTeamsAdapter(String type) {
        this.type = type;
    }

    @Override
    public int getItemCount() { return Constants.teams.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_player_teams, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.txtTeamN.setText(h.itemView.getContext().getResources().getString(R.string.team) + " " + (position + 1));

        if ("TOURNAMENT".equals(type)) {
            h.txtValueTeam.setText(Constants.teams.get(position).getTotalVote() + "");
            h.txtValueTeam.setVisibility(View.VISIBLE);
            h.spaceValue.setVisibility(View.VISIBLE);
        } else {
            h.txtValueTeam.setVisibility(View.GONE);
            h.spaceValue.setVisibility(View.GONE);
        }

        h.llPlayer2.setVisibility(View.GONE);
        h.llPlayer34.setVisibility(View.GONE);
        h.llPlayer5.setVisibility(View.GONE);

        ArrayList<Player> players = Constants.teams.get(position).players;

        // Squadra senza giocatori (es. stato transitorio durante la generazione): niente crash
        if (players.isEmpty()) {
            h.txtPlayer1Name.setText("");
            h.txtPlayer1Surname.setText("");
            return;
        }

        h.txtPlayer1Name.setText(players.get(0).name);
        h.txtPlayer1Surname.setText(players.get(0).getSurnameOrNickname());

        if (players.size() >= 2) {
            h.llPlayer2.setVisibility(View.VISIBLE);
            h.txtPlayer2Name.setText(players.get(1).name);
            h.txtPlayer2Surname.setText(players.get(1).getSurnameOrNickname());
        }
        if (players.size() >= 3) {
            h.llPlayer34.setVisibility(View.VISIBLE);
            h.llPlayer4.setVisibility(View.GONE);
            h.txtPlayer3Name.setText(players.get(2).name);
            h.txtPlayer3Surname.setText(players.get(2).getSurnameOrNickname());
        }
        if (players.size() >= 4) {
            h.llPlayer4.setVisibility(View.VISIBLE);
            h.txtPlayer4Name.setText(players.get(3).name);
            h.txtPlayer4Surname.setText(players.get(3).getSurnameOrNickname());
        }
        if (players.size() >= 5) {
            h.llPlayer5.setVisibility(View.VISIBLE);
            h.txtPlayer5Name.setText(players.get(4).name);
            h.txtPlayer5Surname.setText(players.get(4).getSurnameOrNickname());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTeamN, txtValueTeam;
        final Space spaceValue;
        final LinearLayout llPlayer2, llPlayer4, llPlayer34, llPlayer5;
        final TextView txtPlayer1Name, txtPlayer2Name, txtPlayer3Name, txtPlayer4Name, txtPlayer5Name;
        final TextView txtPlayer1Surname, txtPlayer2Surname, txtPlayer3Surname, txtPlayer4Surname, txtPlayer5Surname;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTeamN = itemView.findViewById(R.id.txtTeamN);
            txtValueTeam = itemView.findViewById(R.id.txtValueTeam);
            spaceValue = itemView.findViewById(R.id.spaceValue);
            llPlayer2 = itemView.findViewById(R.id.llPlayer2);
            llPlayer4 = itemView.findViewById(R.id.llPlayer4);
            llPlayer34 = itemView.findViewById(R.id.llPlayer34);
            llPlayer5 = itemView.findViewById(R.id.llPlayer5);
            txtPlayer1Name = itemView.findViewById(R.id.txtPlayer1Name);
            txtPlayer2Name = itemView.findViewById(R.id.txtPlayer2Name);
            txtPlayer3Name = itemView.findViewById(R.id.txtPlayer3Name);
            txtPlayer4Name = itemView.findViewById(R.id.txtPlayer4Name);
            txtPlayer5Name = itemView.findViewById(R.id.txtPlayer5Name);
            txtPlayer1Surname = itemView.findViewById(R.id.txtPlayer1Surname);
            txtPlayer2Surname = itemView.findViewById(R.id.txtPlayer2Surname);
            txtPlayer3Surname = itemView.findViewById(R.id.txtPlayer3Surname);
            txtPlayer4Surname = itemView.findViewById(R.id.txtPlayer4Surname);
            txtPlayer5Surname = itemView.findViewById(R.id.txtPlayer5Surname);
        }
    }
}
