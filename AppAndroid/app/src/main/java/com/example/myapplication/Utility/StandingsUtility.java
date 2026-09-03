package com.example.myapplication.Utility;

import com.example.myapplication.Match;
import com.example.myapplication.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcolo della classifica di un girone con i criteri, in ordine:
 *  1) punti classifica  2) vittorie  3) quoziente set  4) quoziente punti
 *  5) mini-classifica fra le squadre ancora pari (solo gli scontri diretti fra loro).
 *
 * Il criterio 5 copre tutti i formati: per 2 squadre pari equivale allo scontro
 * diretto; per 3+ alla mini-classifica; nel Champions, se le pari non si sono
 * affrontate, la mini-classifica non ha dati e restano pari.
 *
 * Nota sui dati: una partita salva SOLO points1/points2, che rappresentano i set
 * (numeri piccoli) oppure i punti di un set unico. Per questo i due numeri sono
 * interpretati in base alla grandezza (vedi SET_SCORE_MAX).
 */
public class StandingsUtility {

    /** Sotto/uguale a questa soglia i due numeri sono set, sopra sono punti. */
    private static final int SET_SCORE_MAX = 5;

    private static class TeamStats {
        int classificaPoints = 0;
        int wins = 0;
        long setsWon = 0, setsLost = 0;
        long pointsFor = 0, pointsAgainst = 0;
    }

    /**
     * Ordina le squadre di un girone secondo la catena di criteri. Considera solo
     * le partite di girone (type BRACKET/GIRONE) giocate FRA le squadre passate.
     */
    public static List<Team> orderTeams(List<Team> teams, List<Match> matches) {
        List<Team> list = new ArrayList<>(teams);
        if (list.size() <= 1) {
            return list;
        }

        Map<String, TeamStats> stats = computeStats(list, matches);
        list.sort((a, b) -> compareByChain(stats.get(a.key), stats.get(b.key)));

        // Raggruppa le squadre pari sui criteri 1-4 e le risolve con la mini-classifica
        List<Team> result = new ArrayList<>();
        int i = 0;
        while (i < list.size()) {
            int j = i + 1;
            while (j < list.size()
                    && compareByChain(stats.get(list.get(i).key), stats.get(list.get(j).key)) == 0) {
                j++;
            }

            List<Team> tiedGroup = new ArrayList<>(list.subList(i, j));
            if (tiedGroup.size() == 1 || tiedGroup.size() == list.size()) {
                // singola, oppure tutte pari e irriducibili: mantiene l'ordine corrente
                result.addAll(tiedGroup);
            } else {
                // mini-classifica: stessi criteri, ma solo gli scontri fra le pari
                result.addAll(orderTeams(tiedGroup, matches));
            }
            i = j;
        }
        return result;
    }

    /** Riga di classifica: squadra + statistiche (per la visualizzazione). */
    public static class TeamStanding {
        public Team team;
        public int classificaPoints;
        public int wins;
        public long setsWon, setsLost;
        public long pointsFor, pointsAgainst;
    }

    /**
     * Come orderTeams, ma restituisce anche le statistiche di ogni squadra
     * (calcolate sull'intero girone, non sulla mini-classifica) per mostrarle.
     */
    public static List<TeamStanding> computeStandings(List<Team> teams, List<Match> matches) {
        List<Team> ordered = orderTeams(teams, matches);
        Map<String, TeamStats> stats = computeStats(new ArrayList<>(teams), matches);

        List<TeamStanding> result = new ArrayList<>();
        for (Team t : ordered) {
            TeamStats s = stats.get(t.key);
            TeamStanding ts = new TeamStanding();
            ts.team = t;
            ts.classificaPoints = s.classificaPoints;
            ts.wins = s.wins;
            ts.setsWon = s.setsWon;
            ts.setsLost = s.setsLost;
            ts.pointsFor = s.pointsFor;
            ts.pointsAgainst = s.pointsAgainst;
            result.add(ts);
        }
        return result;
    }

    private static Map<String, TeamStats> computeStats(List<Team> teams, List<Match> matches) {
        Set<String> keys = new HashSet<>();
        Map<String, TeamStats> stats = new HashMap<>();
        for (Team t : teams) {
            keys.add(t.key);
            stats.put(t.key, new TeamStats());
        }

        for (Match m : matches) {
            if (m.type == null || !(m.type.contains("BRACKET") || m.type.contains("GIRONE"))) {
                continue;
            }
            // solo gli scontri fra le squadre passate (per la mini-classifica)
            if (!keys.contains(m.keyTeam1) || !keys.contains(m.keyTeam2)) {
                continue;
            }

            int v1 = m.points1, v2 = m.points2;
            if (v1 == v2) {
                continue; // non giocata / senza esito
            }

            TeamStats s1 = stats.get(m.keyTeam1);
            TeamStats s2 = stats.get(m.keyTeam2);

            boolean team1Wins = v1 > v2;
            TeamStats winner = team1Wins ? s1 : s2;
            TeamStats loser = team1Wins ? s2 : s1;
            winner.wins++;

            // 1) punti classifica
            int winnerPoints, loserPoints;
            boolean hasDetail = m.detail != null && !m.detail.isEmpty();
            boolean isSets = hasDetail || Math.max(v1, v2) <= SET_SCORE_MAX;
            if (isSets) {
                // points1/points2 = set vinti
                int winSets = Math.max(v1, v2);
                int loseSets = Math.min(v1, v2);
                if (winSets >= 2) {
                    // più set: scarto >=2 vittoria netta (3/0), scarto 1 al set decisivo (2/1)
                    boolean netta = (winSets - loseSets) >= 2;
                    winnerPoints = netta ? 3 : 2;
                    loserPoints = netta ? 0 : 1;
                } else {
                    // set unico: ai vantaggi (punti del set > 25) -> 2/1, altrimenti vittoria netta 3/0
                    boolean vantaggi = false;
                    if (hasDetail) {
                        int[] set = m.detail.get(m.detail.size() - 1);
                        vantaggi = Math.max(set[0], set[1]) > 25;
                    }
                    winnerPoints = vantaggi ? 2 : 3;
                    loserPoints = vantaggi ? 1 : 0;
                }
            } else {
                // legacy non migrata (punti di un set unico): vecchia regola
                int winVal = Math.max(v1, v2);
                boolean netta = (winVal == 15 || winVal == 25);
                winnerPoints = netta ? 3 : 2;
                loserPoints = netta ? 0 : 1;
            }
            winner.classificaPoints += winnerPoints;
            loser.classificaPoints += loserPoints;

            if (m.detail != null && !m.detail.isEmpty()) {
                // Formato nuovo: points1/points2 = set vinti, detail = punti dei set
                s1.setsWon += v1; s1.setsLost += v2;
                s2.setsWon += v2; s2.setsLost += v1;
                for (int[] set : m.detail) {
                    s1.pointsFor += set[0]; s1.pointsAgainst += set[1];
                    s2.pointsFor += set[1]; s2.pointsAgainst += set[0];
                }
            } else {
                // Legacy: euristica per grandezza (numeri piccoli = set, grandi = punti)
                boolean isSetMatch = Math.max(v1, v2) <= SET_SCORE_MAX;
                if (isSetMatch) {
                    s1.setsWon += v1; s1.setsLost += v2;
                    s2.setsWon += v2; s2.setsLost += v1;
                } else {
                    if (v1 > v2) {
                        s1.setsWon += 1; s2.setsLost += 1;
                    } else {
                        s2.setsWon += 1; s1.setsLost += 1;
                    }
                    s1.pointsFor += v1; s1.pointsAgainst += v2;
                    s2.pointsFor += v2; s2.pointsAgainst += v1;
                }
            }
        }
        return stats;
    }

    /** Confronto sui criteri 1-4 (0 = pari, da spezzare con la mini-classifica). */
    private static int compareByChain(TeamStats a, TeamStats b) {
        if (a.classificaPoints != b.classificaPoints) {
            return Integer.compare(b.classificaPoints, a.classificaPoints);
        }
        if (a.wins != b.wins) {
            return Integer.compare(b.wins, a.wins);
        }

        int setCmp = compareQuotient(a.setsWon, a.setsLost, b.setsWon, b.setsLost);
        if (setCmp != 0) {
            return setCmp;
        }

        return compareQuotient(a.pointsFor, a.pointsAgainst, b.pointsFor, b.pointsAgainst);
    }

    /**
     * Confronta due quozienti vinti/persi senza divisioni (evita la divisione per
     * zero): a/aLost vs b/bLost diventa aWon*bLost vs bWon*aLost. Quoziente più alto prima.
     */
    private static int compareQuotient(long aWon, long aLost, long bWon, long bLost) {
        long left = aWon * bLost;
        long right = bWon * aLost;
        return Long.compare(right, left);
    }
}
