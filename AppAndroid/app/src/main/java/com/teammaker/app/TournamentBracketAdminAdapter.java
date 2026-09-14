package com.teammaker.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.teammaker.app.Utility.MatchLabelUtility;
import com.teammaker.app.Utility.PhaseUtility;
import com.teammaker.app.Utility.TournamentUtility;

public class TournamentBracketAdminAdapter extends BaseAdapter {

    private final Tournament tournament;

    public TournamentBracketAdminAdapter(Tournament tournament) {
        this.tournament = tournament;
    }

    @Override
    public int getCount() { return tournament.matches.size(); }

    @Override
    public Match getItem(int position) { return tournament.matches.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    public void refresh() {
        notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.tournament_layout_manage_matches, parent, false);
        }

        Match match = getItem(position);

        TextView txtDay = convertView.findViewById(R.id.txtDay);
        txtDay.setText(parent.getContext().getString(R.string.day) + " " + match.day);

        TextView txtType = convertView.findViewById(R.id.txtType);
        String phaseLabel = PhaseUtility.label(parent.getContext(), match.type);
        if (!phaseLabel.isEmpty()) {
            txtType.setText(phaseLabel);
            txtType.setVisibility(View.VISIBLE);
        } else {
            txtType.setVisibility(View.GONE);
        }

        TextView txtTime = convertView.findViewById(R.id.txtTime);
        txtTime.setText(match.time);

        TextView txtTeam1 = convertView.findViewById(R.id.txtTeam1);
        TextView txtTeam2 = convertView.findViewById(R.id.txtTeam2);
        txtTeam1.setText(MatchLabelUtility.teamOrPlaceholder(parent.getContext(), tournament, match, 1));
        txtTeam2.setText(MatchLabelUtility.teamOrPlaceholder(parent.getContext(), tournament, match, 2));

        TextView txtPoints1 = convertView.findViewById(R.id.txtPoints1);
        TextView txtPoints2 = convertView.findViewById(R.id.txtPoints2);
        txtPoints1.setText(String.valueOf(match.points1));
        txtPoints2.setText(String.valueOf(match.points2));

        TextView txtSetDetail = convertView.findViewById(R.id.txtSetDetail);
        String setDetail = match.detailString();
        if (setDetail.isEmpty()) {
            txtSetDetail.setVisibility(View.GONE);
        } else {
            txtSetDetail.setText(setDetail);
            txtSetDetail.setVisibility(View.VISIBLE);
        }

        ImageView btnPlay = convertView.findViewById(R.id.btnPlay);
        ImageView btnEditRow = convertView.findViewById(R.id.btnEdit);
        ImageView btnDeleteRow = convertView.findViewById(R.id.btnDelete);
        // Torneo bloccato: niente modifica/eliminazione/play sulle righe partita.
        if (tournament.locked) {
            btnPlay.setVisibility(View.GONE);
            btnEditRow.setVisibility(View.GONE);
            btnDeleteRow.setVisibility(View.GONE);
        } else {
            btnPlay.setVisibility(View.VISIBLE);
            btnEditRow.setVisibility(View.VISIBLE);
            btnDeleteRow.setVisibility(View.VISIBLE);
        }

        View finalConvertView = convertView;
        btnPlay.setOnClickListener(v -> {
            String orientation = finalConvertView.getContext().getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait";
            Intent intent = new Intent(TournamentActivityManageMatches.tournamentActivityManageMatches, ActivityNextMatch.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("position", position - 1);
            intent.putExtra("orientation", orientation);
            TournamentActivityManageMatches.tournamentActivityManageMatches.startActivity(intent);
        });

        btnEditRow.setOnClickListener(v -> {
            Intent intent = new Intent(TournamentActivityManageMatches.tournamentActivityManageMatches, TournamentActivityEditMatch.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("position", position);
            TournamentActivityManageMatches.tournamentActivityManageMatches.startActivity(intent);
        });

        btnDeleteRow.setOnClickListener(v -> {
            Intent intent = new Intent(TournamentActivityManageMatches.tournamentActivityManageMatches, ActivityPopUp.class);
            intent.putExtra("tournament_key", tournament.key);
            intent.putExtra("match_key", match.key);
            intent.putExtra("pop_up_type", PopUpType.DELETE_MATCH);
            TournamentActivityManageMatches.tournamentActivityManageMatches.startActivity(intent);
        });

        return convertView;
    }
}