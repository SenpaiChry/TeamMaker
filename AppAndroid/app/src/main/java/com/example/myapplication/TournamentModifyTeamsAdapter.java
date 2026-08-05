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

import java.util.ArrayList;

public class TournamentModifyTeamsAdapter extends BaseAdapter {

    private final Context context;
    private final int nCharSurname;
    private final Tournament tournament;

    private static final int[][] NAME_IDS = {
            {R.id.txtPlayer1Name, R.id.txtPlayer1Surname},
            {R.id.txtPlayer2Name, R.id.txtPlayer2Surname},
            {R.id.txtPlayer3Name, R.id.txtPlayer3Surname},
            {R.id.txtPlayer4Name, R.id.txtPlayer4Surname},
            {R.id.txtPlayer5Name, R.id.txtPlayer5Surname},
    };
    private static final int[] CONTAINER_IDS = {
            R.id.llPlayer1, R.id.llPlayer2, R.id.llPlayer3, R.id.llPlayer4, R.id.llPlayer5
    };

    public TournamentModifyTeamsAdapter(Context context, int nCharSurname, Tournament tournament) {
        this.context = context;
        this.nCharSurname = nCharSurname;
        this.tournament = tournament;
    }

    @Override
    public int getCount() { return tournament.teams.size(); }

    @Override
    public Team getItem(int position) { return tournament.teams.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tournament_layout_manage_teams, parent, false);
        }

        Team team = getItem(position);
        ArrayList<Player> players = team.players;

        TextView txtTeamN = convertView.findViewById(R.id.txtTeamN);
        txtTeamN.setText(context.getString(R.string.team) + " " + tournament.getNTeamByKey(team.key));

        // Riempie i 5 slot giocatore, nasconde quelli non usati
        LinearLayout llPlayer34 = convertView.findViewById(R.id.llPlayer34);
        LinearLayout llPlayer5 = convertView.findViewById(R.id.llPlayer5);

        llPlayer34.setVisibility(View.GONE);
        llPlayer5.setVisibility(View.GONE);

        for (int i = 0; i < CONTAINER_IDS.length; i++) {
            View container = convertView.findViewById(CONTAINER_IDS[i]);
            TextView txtName = convertView.findViewById(NAME_IDS[i][0]);
            TextView txtSurname = convertView.findViewById(NAME_IDS[i][1]);

            if (i < players.size()) {
                container.setVisibility(View.VISIBLE);
                txtName.setText(players.get(i).name);
                txtSurname.setText(players.get(i).getSurnameOrNickname());

                if (i >= 2 && i <= 3) llPlayer34.setVisibility(View.VISIBLE);
                if (i == 4) llPlayer5.setVisibility(View.VISIBLE);
            } else {
                container.setVisibility(View.GONE);
            }
        }

        // Nasconde anche il container del player 2 se c'è solo 1 giocatore
        convertView.findViewById(R.id.llPlayer2)
                .setVisibility(players.size() >= 2 ? View.VISIBLE : View.GONE);

        ImageView btnEdit = convertView.findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, TournamentActivityPopUpEditTeam.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("team_key", team.key);
            intent.putExtra("nCharSurname", nCharSurname);
            context.startActivity(intent);
        });

        ImageView btnDelete = convertView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> {
            Intent intent = new Intent(context, ActivityPopUp.class);
            intent.putExtra("team_key", team.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_TEAM);
            context.startActivity(intent);
        });

        return convertView;
    }
}