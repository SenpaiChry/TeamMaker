package com.example.myapplication.Utility;

import android.content.Context;

import com.example.myapplication.Match;
import com.example.myapplication.R;
import com.example.myapplication.Tournament;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Etichetta da mostrare per uno slot di una partita: la squadra reale se nota,
 * altrimenti il placeholder della fase finale ("1ª", "1ª GIR. A", "VINC. Q1", ...).
 */
public class MatchLabelUtility {

    public static String teamOrPlaceholder(Context ctx, Tournament tournament, Match match, int slot) {
        String key = slot == 1 ? match.keyTeam1 : match.keyTeam2;
        boolean unresolved = key == null || key.equals("TO DO") || tournament.getTeamEntityByKey(key) == null;

        String type = slot == 1 ? match.source1Type : match.source2Type;
        String ref = slot == 1 ? match.source1Ref : match.source2Ref;

        if (unresolved && type != null && !type.isEmpty()) {
            return placeholderLabel(ctx, tournament, type, ref);
        }
        return tournament.toStringNTeamByKey(key);
    }

    private static String placeholderLabel(Context ctx, Tournament tournament, String type, String ref) {
        switch (type) {
            case "STANDING":
                return ref + ctx.getString(R.string.ordinal_a);
            case "GROUP_STANDING": {
                int split = 0;
                while (split < ref.length() && !Character.isDigit(ref.charAt(split))) split++;
                String letter = ref.substring(0, split);
                String pos = ref.substring(split);
                return pos + ctx.getString(R.string.ordinal_a) + " " + ctx.getString(R.string.group_abbr) + " " + letter;
            }
            case "WINNER":
                return ctx.getString(R.string.winner_abbr) + " " + codeOf(ctx, tournament, ref);
            case "LOSER":
                return ctx.getString(R.string.loser_abbr) + " " + codeOf(ctx, tournament, ref);
            default:
                return "TO DO";
        }
    }

    /** Codice breve (Q1..Q4, S1..S2, F) della partita finale, per i placeholder WINNER/LOSER. */
    private static String codeOf(Context ctx, Tournament tournament, String matchKey) {
        String quarter = ctx.getString(R.string.quarter);
        String semi = ctx.getString(R.string.semifinal);
        String finalLabel = ctx.getString(R.string.finalString);

        List<Match> quarters = new ArrayList<>();
        List<Match> semis = new ArrayList<>();
        Match finalMatch = null;
        for (Match m : tournament.matches) {
            if (m.type == null) continue;
            if (m.type.equals(quarter)) quarters.add(m);
            else if (m.type.equals(semi)) semis.add(m);
            else if (m.type.equals(finalLabel)) finalMatch = m;
        }
        sortByDayTime(quarters);
        sortByDayTime(semis);

        for (int i = 0; i < quarters.size(); i++) {
            if (quarters.get(i).key.equals(matchKey)) return "Q" + (i + 1);
        }
        for (int i = 0; i < semis.size(); i++) {
            if (semis.get(i).key.equals(matchKey)) return "S" + (i + 1);
        }
        if (finalMatch != null && finalMatch.key.equals(matchKey)) return "F";
        return "?";
    }

    private static void sortByDayTime(List<Match> matches) {
        Collections.sort(matches, (m1, m2) -> {
            if (m1.day != m2.day) return Integer.compare(m1.day, m2.day);
            return Integer.compare(TimeUtility.toMinutes(m1.time), TimeUtility.toMinutes(m2.time));
        });
    }
}
