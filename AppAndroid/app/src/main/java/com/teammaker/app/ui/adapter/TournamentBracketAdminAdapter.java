package com.teammaker.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.domain.MatchLabels;
import com.teammaker.app.domain.MatchPhases;
import com.teammaker.app.data.model.Match;
import com.teammaker.app.data.model.PopUpType;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.ui.activity.EditMatchActivity;
import com.teammaker.app.ui.activity.NextMatchActivity;
import com.teammaker.app.ui.popup.ConfirmPopupActivity;
import com.teammaker.app.R;

public class TournamentBracketAdminAdapter extends RecyclerView.Adapter<TournamentBracketAdminAdapter.ViewHolder> {

    private final Tournament tournament;

    public TournamentBracketAdminAdapter(Tournament tournament) {
        this.tournament = tournament;
    }

    @Override
    public int getItemCount() { return tournament.matches.size(); }

    public void refresh() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tournament_layout_manage_matches, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Match match = tournament.matches.get(position);

        h.txtDay.setText(h.itemView.getContext().getString(R.string.day) + " " + match.day);

        String phaseLabel = MatchPhases.label(h.itemView.getContext(), match.type);
        if (!phaseLabel.isEmpty()) {
            h.txtType.setText(phaseLabel);
            h.txtType.setVisibility(View.VISIBLE);
        } else {
            h.txtType.setVisibility(View.GONE);
        }

        h.txtTime.setText(match.time);
        h.txtTeam1.setText(MatchLabels.teamOrPlaceholder(h.itemView.getContext(), tournament, match, 1));
        h.txtTeam2.setText(MatchLabels.teamOrPlaceholder(h.itemView.getContext(), tournament, match, 2));
        h.txtPoints1.setText(String.valueOf(match.points1));
        h.txtPoints2.setText(String.valueOf(match.points2));

        String setDetail = match.detailString();
        if (setDetail.isEmpty()) {
            h.txtSetDetail.setVisibility(View.GONE);
        } else {
            h.txtSetDetail.setText(setDetail);
            h.txtSetDetail.setVisibility(View.VISIBLE);
        }

        // Torneo bloccato: niente modifica/eliminazione/play sulle righe partita.
        if (tournament.locked) {
            h.btnPlay.setVisibility(View.GONE);
            h.btnEdit.setVisibility(View.GONE);
            h.btnDelete.setVisibility(View.GONE);
        } else {
            h.btnPlay.setVisibility(View.VISIBLE);
            h.btnEdit.setVisibility(View.VISIBLE);
            h.btnDelete.setVisibility(View.VISIBLE);
        }

        h.btnPlay.setOnClickListener(v -> {
            android.content.Context ctx = h.itemView.getContext();
            String orientation = ctx.getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait";
            Intent intent = new Intent(ctx, NextMatchActivity.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("position", position - 1);
            intent.putExtra("orientation", orientation);
            ctx.startActivity(intent);
        });

        h.btnEdit.setOnClickListener(v -> {
            android.content.Context ctx = h.itemView.getContext();
            Intent intent = new Intent(ctx, EditMatchActivity.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("position", position);
            ctx.startActivity(intent);
        });

        h.btnDelete.setOnClickListener(v -> {
            android.content.Context ctx = h.itemView.getContext();
            Intent intent = new Intent(ctx, ConfirmPopupActivity.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("match_key", match.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_MATCH);
            ctx.startActivity(intent);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDay, txtType, txtTime, txtTeam1, txtTeam2, txtPoints1, txtPoints2, txtSetDetail;
        final ImageView btnPlay, btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDay = itemView.findViewById(R.id.txtDay);
            txtType = itemView.findViewById(R.id.txtType);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtTeam1 = itemView.findViewById(R.id.txtTeam1);
            txtTeam2 = itemView.findViewById(R.id.txtTeam2);
            txtPoints1 = itemView.findViewById(R.id.txtPoints1);
            txtPoints2 = itemView.findViewById(R.id.txtPoints2);
            txtSetDetail = itemView.findViewById(R.id.txtSetDetail);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
