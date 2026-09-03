package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.TournamentUtility;
import com.example.myapplication.Utility.Utility;

import java.util.ArrayList;

public class TournamentBracketAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Match> matches;

    /** Testo cercato: la squadra che contiene quel giocatore viene evidenziata nella riga. */
    private String highlightQuery = "";

    public TournamentBracketAdapter(Context context, ArrayList<Match> matches) {
        this.context = context;
        this.matches = new ArrayList<>(matches);
    }

    /**
     * Aggiorna i dati senza dover ricreare l'adapter a ogni ricerca.
     * (La lista interna e' una copia, quindi il solo notifyDataSetChanged non basterebbe.)
     */
    public void setMatches(ArrayList<Match> newMatches) {
        matches.clear();
        if (newMatches != null) matches.addAll(newMatches);
        notifyDataSetChanged();
    }

    /** Imposta il testo cercato: evidenzia la squadra del giocatore trovato. */
    public void setHighlightQuery(String query) {
        this.highlightQuery = (query == null) ? "" : query.trim();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return matches.size();
    }

    @Override
    public Match getItem(int position) {
        return matches.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.tournament_layout_bracket, parent, false);
        }

        Match match = getItem(position);

        TextView txtDay = convertView.findViewById(R.id.txtDay);
        txtDay.setText(context.getString(R.string.day) + " " + match.day);

        TextView txtType = convertView.findViewById(R.id.txtType);
        if (match.type != null && !match.type.isEmpty()) {
            txtType.setText(match.type);
            txtType.setVisibility(View.VISIBLE);
        } else {
            txtType.setVisibility(View.GONE);
        }

        TextView txtTime = convertView.findViewById(R.id.txtTime);
        TextView txtTeam1 = convertView.findViewById(R.id.txtTeam1);
        TextView txtTeam2 = convertView.findViewById(R.id.txtTeam2);
        TextView txtPoints1 = convertView.findViewById(R.id.txtPoints1);
        TextView txtPoints2 = convertView.findViewById(R.id.txtPoints2);

        txtTime.setText(match.time);
        txtTeam1.setText(TournamentUtility.getActiveTournament().toStringNTeamByKey(match.keyTeam1));
        txtTeam2.setText(TournamentUtility.getActiveTournament().toStringNTeamByKey(match.keyTeam2));
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

        // Evidenzia il lato in cui gioca il giocatore cercato
        applyTeamHighlight(txtTeam1, txtPoints1, teamMatchesQuery(match.keyTeam1));
        applyTeamHighlight(txtTeam2, txtPoints2, teamMatchesQuery(match.keyTeam2));

        Button btnInfo = convertView.findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(v -> {
            Intent intent = new Intent(context.getApplicationContext(), ActivityPopUpInfoMatch.class);
            intent.putExtra("match_key", match.key);
            context.startActivity(intent);
        });

        return convertView;
    }

    /** True se la squadra contiene un giocatore che corrisponde al testo cercato. */
    private boolean teamMatchesQuery(String keyTeam) {
        if (highlightQuery.isEmpty() || keyTeam == null) return false;
        Team team = TournamentTeamUtility.getTeamByKey(keyTeam);
        if (team == null || team.players == null) return false;
        for (Player player : team.players) {
            if (player.containsString(highlightQuery)) return true;
        }
        return false;
    }

    private void applyTeamHighlight(TextView txtTeam, TextView txtPoints, boolean highlighted) {
        int color = ContextCompat.getColor(context,
                highlighted ? R.color.list_highlight_text : R.color.list_text_primary);
        txtTeam.setTextColor(color);
        txtPoints.setTextColor(color);

        // Pillola attorno alla squadra evidenziata (setBackground* azzera il padding: lo ripristino)
        int l = txtTeam.getPaddingLeft(), t = txtTeam.getPaddingTop();
        int r = txtTeam.getPaddingRight(), bo = txtTeam.getPaddingBottom();
        if (highlighted) {
            txtTeam.setBackgroundResource(R.drawable.bg_team_chip);
        } else {
            txtTeam.setBackground(null);
        }
        txtTeam.setPadding(l, t, r, bo);
    }
}