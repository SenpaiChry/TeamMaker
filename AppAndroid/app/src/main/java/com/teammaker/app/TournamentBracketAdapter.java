package com.teammaker.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.Utility.MatchLabelUtility;
import com.teammaker.app.Utility.PhaseUtility;
import com.teammaker.app.Utility.TournamentTeamUtility;
import com.teammaker.app.Utility.TournamentUtility;

import java.util.ArrayList;

public class TournamentBracketAdapter extends RecyclerView.Adapter<TournamentBracketAdapter.ViewHolder> {

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
    public int getItemCount() { return matches.size(); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tournament_layout_bracket, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Match match = matches.get(position);

        h.txtDay.setText(context.getString(R.string.day) + " " + match.day);

        String phaseLabel = PhaseUtility.label(context, match.type);
        if (!phaseLabel.isEmpty()) {
            h.txtType.setText(phaseLabel);
            h.txtType.setVisibility(View.VISIBLE);
        } else {
            h.txtType.setVisibility(View.GONE);
        }

        h.txtTime.setText(match.time);
        Tournament activeTournament = TournamentUtility.getActiveTournament();
        h.txtTeam1.setText(MatchLabelUtility.teamOrPlaceholder(context, activeTournament, match, 1));
        h.txtTeam2.setText(MatchLabelUtility.teamOrPlaceholder(context, activeTournament, match, 2));
        h.txtPoints1.setText(String.valueOf(match.points1));
        h.txtPoints2.setText(String.valueOf(match.points2));

        String setDetail = match.detailString();
        if (setDetail.isEmpty()) {
            h.txtSetDetail.setVisibility(View.GONE);
        } else {
            h.txtSetDetail.setText(setDetail);
            h.txtSetDetail.setVisibility(View.VISIBLE);
        }

        // Evidenzia il lato in cui gioca il giocatore cercato
        applyTeamHighlight(h.txtTeam1, h.txtPoints1, teamMatchesQuery(match.keyTeam1));
        applyTeamHighlight(h.txtTeam2, h.txtPoints2, teamMatchesQuery(match.keyTeam2));

        h.btnInfo.setOnClickListener(v -> {
            Intent intent = new Intent(context.getApplicationContext(), ActivityPopUpInfoMatch.class);
            intent.putExtra("match_key", match.key);
            context.startActivity(intent);
        });
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDay, txtType, txtTime, txtTeam1, txtTeam2, txtPoints1, txtPoints2, txtSetDetail;
        final Button btnInfo;

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
            btnInfo = itemView.findViewById(R.id.btnInfo);
        }
    }
}
