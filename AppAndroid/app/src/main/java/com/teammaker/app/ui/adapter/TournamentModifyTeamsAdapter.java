package com.teammaker.app.ui.adapter;

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
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.data.model.PopUpType;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.ui.popup.ConfirmPopupActivity;
import com.teammaker.app.ui.popup.EditTeamPopupActivity;
import com.teammaker.app.R;

public class TournamentModifyTeamsAdapter extends RecyclerView.Adapter<TournamentModifyTeamsAdapter.ViewHolder> {

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
    public int getItemCount() { return tournament.teams.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tournament_layout_manage_teams, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Team team = tournament.teams.get(position);
        ArrayList<Player> players = team.players;

        h.txtTeamN.setText(context.getString(R.string.team) + " " + tournament.getNTeamByKey(team.key));

        // Riempie i 5 slot giocatore, nasconde quelli non usati
        h.llPlayer34.setVisibility(View.GONE);
        h.llPlayer5.setVisibility(View.GONE);

        for (int i = 0; i < CONTAINER_IDS.length; i++) {
            View container = h.itemView.findViewById(CONTAINER_IDS[i]);
            TextView txtName = h.itemView.findViewById(NAME_IDS[i][0]);
            TextView txtSurname = h.itemView.findViewById(NAME_IDS[i][1]);

            if (i < players.size()) {
                container.setVisibility(View.VISIBLE);
                txtName.setText(players.get(i).name);
                txtSurname.setText(players.get(i).getSurnameOrNickname());

                if (i >= 2 && i <= 3) h.llPlayer34.setVisibility(View.VISIBLE);
                if (i == 4) h.llPlayer5.setVisibility(View.VISIBLE);
            } else {
                container.setVisibility(View.GONE);
            }
        }

        // Nasconde anche il container del player 2 se c'è solo 1 giocatore
        h.itemView.findViewById(R.id.llPlayer2)
                .setVisibility(players.size() >= 2 ? View.VISIBLE : View.GONE);

        h.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditTeamPopupActivity.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("team_key", team.key);
            intent.putExtra("nCharSurname", nCharSurname);
            context.startActivity(intent);
        });

        h.btnDelete.setOnClickListener(v -> {
            Intent intent = new Intent(context, ConfirmPopupActivity.class);
            intent.putExtra("team_key", team.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_TEAM);
            context.startActivity(intent);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTeamN;
        final LinearLayout llPlayer34, llPlayer5;
        final ImageView btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTeamN = itemView.findViewById(R.id.txtTeamN);
            llPlayer34 = itemView.findViewById(R.id.llPlayer34);
            llPlayer5 = itemView.findViewById(R.id.llPlayer5);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
