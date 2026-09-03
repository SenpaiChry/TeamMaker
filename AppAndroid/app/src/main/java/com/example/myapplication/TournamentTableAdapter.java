package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.example.myapplication.Utility.StandingsUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TournamentTableAdapter extends BaseAdapter {

    private static final int VIEW_TYPE_TEAM = 0;
    private static final int VIEW_TYPE_BRACKET_HEADER = 1;

    private final Tournament tournament;
    private final Context context;
    private final ArrayList<Object> items;      // String (intestazione girone) oppure Team
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

        List<StandingsUtility.TeamStanding> standings = new ArrayList<>();
        for (String bracket : brackets) {
            List<Team> inBracket = new ArrayList<>();
            for (Team t : tournament.teams) {
                if (bracket.equals(t.bracket == null ? "" : t.bracket)) inBracket.add(t);
            }
            standings.addAll(StandingsUtility.computeStandings(inBracket, tournament.matches));
        }

        // Aggiunge le intestazioni quando cambia il girone e numera le posizioni
        String lastBracket = null;
        int positionInBracket = 0;
        for (StandingsUtility.TeamStanding ts : standings) {
            String bracket = ts.team.bracket == null ? "" : ts.team.bracket;
            if (!bracket.equals(lastBracket)) {
                lastBracket = bracket;
                positionInBracket = 0;
                items.add(lastBracket);
                positions.add(0);
            }
            positionInBracket++;
            items.add(ts);
            positions.add(positionInBracket);
        }
    }

    /** Imposta il testo cercato e ridisegna: nessun team viene rimosso, cambia solo l'evidenziazione. */
    public void setHighlightQuery(String query) {
        this.highlightQuery = (query == null) ? "" : query.trim();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof StandingsUtility.TeamStanding) ? VIEW_TYPE_TEAM : VIEW_TYPE_BRACKET_HEADER;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_BRACKET_HEADER) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context)
                        .inflate(R.layout.tournament_layout_bracket_header, parent, false);
            }
            TextView txtHeader = convertView.findViewById(R.id.txtBracketHeader);
            txtHeader.setText(context.getString(R.string.bracket) + " " + items.get(position));
            return convertView;
        }

        // ---- Riga squadra ----
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.tournament_layout_table, parent, false);
        }

        StandingsUtility.TeamStanding standing = (StandingsUtility.TeamStanding) items.get(position);
        Team team = standing.team;
        TextView txtPosition = convertView.findViewById(R.id.txtPosition);
        TextView txtTeam = convertView.findViewById(R.id.txtTeam);
        TextView txtTeamPlayers = convertView.findViewById(R.id.txtTeamPlayers);
        TextView txtWins = convertView.findViewById(R.id.txtWins);
        TextView txtSetQuotient = convertView.findViewById(R.id.txtSetQuotient);
        TextView txtPointsQuotient = convertView.findViewById(R.id.txtPointsQuotient);
        TextView txtPoints = convertView.findViewById(R.id.txtPoints);

        // txtPosition puo' non esistere se si usa un layout riga senza la posizione
        if (txtPosition != null) {
            txtPosition.setText(String.format(Locale.getDefault(), "%d", positions.get(position)));
        }
        txtTeam.setText(context.getString(R.string.team) + " " + tournament.getNTeamByKey(team.key));
        if (txtTeamPlayers != null) {
            txtTeamPlayers.setText(team.toStringNameAndSurname());
        }
        if (txtWins != null) {
            txtWins.setText(String.valueOf(standing.wins));
        }
        if (txtSetQuotient != null) {
            txtSetQuotient.setText(formatQuotient(standing.setsWon, standing.setsLost));
        }
        if (txtPointsQuotient != null) {
            txtPointsQuotient.setText(formatQuotient(standing.pointsFor, standing.pointsAgainst));
        }
        txtPoints.setText(String.valueOf(standing.classificaPoints));

        applyHighlight(convertView, txtPosition, txtTeam, txtTeamPlayers, txtPoints, matchesQuery(team));

        return convertView;
    }

    /** Quoziente vinti/persi come decimale; "∞" se non ha mai perso, "–" se nessun dato. */
    private String formatQuotient(long won, long lost) {
        if (lost == 0) {
            return won == 0 ? "–" : "∞";
        }
        return String.format(Locale.getDefault(), "%.2f", (double) won / lost);
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
}