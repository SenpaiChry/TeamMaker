package com.teammaker.app.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.teammaker.app.domain.Standings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.teammaker.app.data.model.Player;
import com.teammaker.app.data.model.Team;
import com.teammaker.app.data.model.Tournament;
import com.teammaker.app.R;

public class TournamentTableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_TEAM = 0;
    private static final int VIEW_TYPE_BRACKET_HEADER = 1;

    private final Tournament tournament;
    private final Context context;
    private final ArrayList<Object> items;      // String (intestazione girone) oppure Standings.TeamStanding
    private final ArrayList<Integer> positions; // posizione nel girone; 0 per le intestazioni

    /** Testo cercato: la classifica mostra SEMPRE tutti i team, ma evidenzia quelli che lo contengono. */
    private String highlightQuery = "";

    @RequiresApi(api = Build.VERSION_CODES.N)
    public TournamentTableAdapter(Context context, Tournament tournament) {
        this.context = context;
        this.tournament = tournament;
        this.items = new ArrayList<>();
        this.positions = new ArrayList<>();

        // Raggruppa per girone (ordine alfabetico) e ordina ogni girone con i criteri completi
        List<String> brackets = new ArrayList<>();
        for (Team t : tournament.teams) {
            String bracket = t.bracket == null ? "" : t.bracket;
            if (!brackets.contains(bracket)) brackets.add(bracket);
        }
        Collections.sort(brackets);

        List<Standings.TeamStanding> standings = new ArrayList<>();
        for (String bracket : brackets) {
            List<Team> inBracket = new ArrayList<>();
            for (Team t : tournament.teams) {
                if (bracket.equals(t.bracket == null ? "" : t.bracket)) inBracket.add(t);
            }
            standings.addAll(Standings.computeStandings(inBracket, tournament.matches));
        }

        // Aggiunge le intestazioni quando cambia il girone e numera le posizioni
        String lastBracket = null;
        for (Standings.TeamStanding ts : standings) {
            String bracket = ts.team.bracket == null ? "" : ts.team.bracket;
            if (!bracket.equals(lastBracket)) {
                lastBracket = bracket;
                items.add(lastBracket);
                positions.add(0);
            }
            items.add(ts);
            positions.add(ts.rank); // rank condiviso: le squadre davvero pari hanno lo stesso numero
        }
    }

    /** Imposta il testo cercato e ridisegna: nessun team viene rimosso, cambia solo l'evidenziazione. */
    public void setHighlightQuery(String query) {
        this.highlightQuery = (query == null) ? "" : query.trim();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return items.size(); }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof Standings.TeamStanding) ? VIEW_TYPE_TEAM : VIEW_TYPE_BRACKET_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_BRACKET_HEADER) {
            View v = inflater.inflate(R.layout.tournament_layout_bracket_header, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = inflater.inflate(R.layout.tournament_layout_table, parent, false);
        return new TeamViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.txtHeader.setText(context.getString(R.string.bracket) + " " + items.get(position));
            return;
        }

        TeamViewHolder h = (TeamViewHolder) holder;
        Standings.TeamStanding standing = (Standings.TeamStanding) items.get(position);
        Team team = standing.team;

        if (h.txtPosition != null) {
            h.txtPosition.setText(String.format(Locale.getDefault(), "%d", positions.get(position)));
        }
        h.txtTeam.setText(context.getString(R.string.team) + " " + tournament.getNTeamByKey(team.key));
        if (h.txtTeamPlayers != null) {
            h.txtTeamPlayers.setText(team.toStringNameAndSurname());
        }
        if (h.txtWins != null) {
            h.txtWins.setText(String.valueOf(standing.wins));
        }
        if (h.txtSetQuotient != null) {
            h.txtSetQuotient.setText(formatQuotient(standing.setsWon, standing.setsLost));
        }
        if (h.txtPointsQuotient != null) {
            h.txtPointsQuotient.setText(formatQuotient(standing.pointsFor, standing.pointsAgainst));
        }
        h.txtPoints.setText(String.valueOf(standing.classificaPoints));

        if (h.txtDirectClash != null) {
            h.txtDirectClash.setVisibility(standing.directClash ? View.VISIBLE : View.GONE);
        }

        applyHighlight(h.itemView, h.txtPosition, h.txtTeam, h.txtTeamPlayers, h.txtPoints, matchesQuery(team));
    }

    /** Quoziente vinti/persi come decimale; "∞" se non ha mai perso, "–" se nessun dato. */
    private String formatQuotient(long won, long lost) {
        if (lost == 0) {
            return won == 0 ? "–" : "∞";
        }
        return String.format(Locale.getDefault(), "%.3f", (double) won / lost);
    }

    /** True se un giocatore della squadra corrisponde al testo cercato. */
    private boolean matchesQuery(Team team) {
        if (highlightQuery.isEmpty() || team.players == null) return false;
        for (Player player : team.players) {
            if (player.containsString(highlightQuery)) return true;
        }
        return false;
    }

    /**
     * Applica (o rimuove) lo stile di evidenziazione.
     * setBackgroundResource azzera il padding della view, quindi lo salvo e lo ripristino.
     */
    private void applyHighlight(View row, TextView txtPosition, TextView txtTeam,
                                TextView txtTeamPlayers, TextView txtPoints, boolean highlighted) {
        int left = row.getPaddingLeft(), top = row.getPaddingTop();
        int right = row.getPaddingRight(), bottom = row.getPaddingBottom();

        row.setBackgroundResource(highlighted
                ? R.drawable.bg_list_card_highlight
                : R.drawable.bg_list_card);

        row.setPadding(left, top, right, bottom);

        int teamColor = ContextCompat.getColor(context,
                highlighted ? R.color.list_highlight_text : R.color.list_text_primary);
        int positionColor = ContextCompat.getColor(context,
                highlighted ? R.color.list_highlight_text : R.color.list_text_muted);

        txtTeam.setTextColor(teamColor);
        txtPoints.setTextColor(teamColor);
        if (txtPosition != null) txtPosition.setTextColor(positionColor);
        if (txtTeamPlayers != null) {
            txtTeamPlayers.setTextColor(ContextCompat.getColor(context,
                    highlighted ? R.color.list_highlight_text : R.color.list_text_secondary));
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView txtHeader;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            txtHeader = itemView.findViewById(R.id.txtBracketHeader);
        }
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {
        final TextView txtPosition, txtTeam, txtTeamPlayers, txtWins;
        final TextView txtSetQuotient, txtPointsQuotient, txtPoints, txtDirectClash;

        TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            txtPosition = itemView.findViewById(R.id.txtPosition);
            txtTeam = itemView.findViewById(R.id.txtTeam);
            txtTeamPlayers = itemView.findViewById(R.id.txtTeamPlayers);
            txtWins = itemView.findViewById(R.id.txtWins);
            txtSetQuotient = itemView.findViewById(R.id.txtSetQuotient);
            txtPointsQuotient = itemView.findViewById(R.id.txtPointsQuotient);
            txtPoints = itemView.findViewById(R.id.txtPoints);
            txtDirectClash = itemView.findViewById(R.id.txtDirectClash);
        }
    }
}
