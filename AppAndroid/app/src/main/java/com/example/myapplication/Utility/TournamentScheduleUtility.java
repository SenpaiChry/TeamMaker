package com.example.myapplication.Utility;

import com.example.myapplication.Match;
import com.example.myapplication.Team;
import com.example.myapplication.Tournament;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TournamentScheduleUtility {
    // scheduleMatchesWithConstraints ?????

    private static boolean backtrack(List<Match> scheduled, Set<Match> remaining, Map<String, Integer> lastPlayedAt, int teamCount, int index) {
        if (remaining.isEmpty()) return true;

        for (Match match : new ArrayList<>(remaining)) {
            String team1 = match.keyTeam1;
            String team2 = match.keyTeam2;

            int last1 = lastPlayedAt.getOrDefault(team1, -2);
            int last2 = lastPlayedAt.getOrDefault(team2, -2);

            if ((index - last1 > 1) && (index - last2 > 1)) {
                scheduled.add(match);
                remaining.remove(match);
                lastPlayedAt.put(team1, index);
                lastPlayedAt.put(team2, index);

                if (backtrack(scheduled, remaining, lastPlayedAt, teamCount, index + 1)) {
                    return true;
                }

                scheduled.remove(scheduled.size() - 1);
                remaining.add(match);
                if (index - 1 >= 0) {
                    lastPlayedAt.put(team1, findLastPlayedIndex(scheduled, team1));
                    lastPlayedAt.put(team2, findLastPlayedIndex(scheduled, team2));
                } else {
                    lastPlayedAt.remove(team1);
                    lastPlayedAt.remove(team2);
                }
            }
        }

        return false;
    }

    private static int findLastPlayedIndex(List<Match> scheduled, String team) {
        for (int i = scheduled.size() - 1; i >= 0; i--) {
            Match m = scheduled.get(i);
            if (m.keyTeam1.equals(team) || m.keyTeam2.equals(team)) {
                return i;
            }
        }
        return -2;
    }

    public static void generateItalianBracket(String tournamentKey, ArrayList<String> stringsInput, String minutesPerMatch) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        for (Team team : tournament.teams) {
            team.bracket = "A";
        }

        List<List<Match>> rounds = buildRoundRobinRounds(tournament.teams, "GIRONE "); // todo sistemare
        ArrayList<Match> matches = orderRoundsBySpacing(rounds);

        int index = 0;
        int day = Integer.parseInt(stringsInput.get(index));
        int hours = Integer.parseInt(stringsInput.get(index + 1).split(":")[0]);
        int minutes = Integer.parseInt(stringsInput.get(index + 1).split(":")[1]);
        int minutesForMatch = Integer.parseInt(minutesPerMatch);

        int endHours = Integer.parseInt(stringsInput.get(index + 2).split(":")[0]);
        int endMinutes = Integer.parseInt(stringsInput.get(index + 2).split(":")[1]);
        int startTotalMinutes = hours * 60 + minutes;
        int endTotalMinutes = endHours * 60 + endMinutes;

        for (int i = 0; i < matches.size(); i++) {
            matches.get(i).day = day;
            matches.get(i).time = String.format("%d:%02d", hours, minutes);

            startTotalMinutes += minutesForMatch;

            if (i < matches.size() - 1) {
                if (startTotalMinutes + minutesForMatch <= endTotalMinutes) {
                    hours = startTotalMinutes / 60;
                    minutes = startTotalMinutes % 60;
                } else if (stringsInput.size() > index + 3) {
                    index += 3;
                    day = Integer.parseInt(stringsInput.get(index));
                    hours = Integer.parseInt(stringsInput.get(index + 1).split(":")[0]);
                    minutes = Integer.parseInt(stringsInput.get(index + 1).split(":")[1]);
                    startTotalMinutes = hours * 60 + minutes;
                    endHours = Integer.parseInt(stringsInput.get(index + 2).split(":")[0]);
                    endMinutes = Integer.parseInt(stringsInput.get(index + 2).split(":")[1]);
                    endTotalMinutes = endHours * 60 + endMinutes;
                }
            }

            MatchUtility.addNewMatch(tournament.key, matches.get(i));
        }

        tournament.matches = matches;
        TournamentTeamUtility.saveBracketForTeams(tournamentKey);
        TournamentUtility.updateNBracketsTournament(tournamentKey, 1);
    }

    private static ArrayList<Match> orderRoundsBySpacing(List<List<Match>> rounds) {
        ArrayList<Match> ordered = new ArrayList<>();
        Map<String, Integer> lastPlayedAt = new HashMap<>();
        int slot = 0;

        for (List<Match> round : rounds) {
            List<Match> pool = new ArrayList<>(round);

            while (!pool.isEmpty()) {
                Match best = null;
                int bestMinRest = Integer.MIN_VALUE;
                int bestSumRest = Integer.MIN_VALUE;

                for (Match m : pool) {
                    int rest1 = slot - lastPlayedAt.getOrDefault(m.keyTeam1, -1000);
                    int rest2 = slot - lastPlayedAt.getOrDefault(m.keyTeam2, -1000);
                    int minRest = Math.min(rest1, rest2);
                    int sumRest = rest1 + rest2;

                    if (minRest > bestMinRest || (minRest == bestMinRest && sumRest > bestSumRest)) {
                        best = m;
                        bestMinRest = minRest;
                        bestSumRest = sumRest;
                    }
                }

                ordered.add(best);
                pool.remove(best);
                lastPlayedAt.put(best.keyTeam1, slot);
                lastPlayedAt.put(best.keyTeam2, slot);
                slot++;
            }
        }

        return ordered;
    }

    public static void generateMultipleBracket(String tournamentKey, ArrayList<String> stringsInput, String minutesPerMatch, int nBrackets) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        ArrayList<ArrayList<Team>> brackets = new ArrayList<>();
        for (int i = 0; i < nBrackets; i++) brackets.add(new ArrayList<>());

        for (int i = 0; i < tournament.teams.size(); i++) {
            int b = i % nBrackets;
            Team t = tournament.teams.get(i);
            brackets.get(b).add(t);
            t.bracket = getBracketLetter(b);
        }

        ArrayList<List<Match>> perBracketOrderedMatches = new ArrayList<>();
        for (int b = 0; b < nBrackets; b++) {
            List<Team> teams = brackets.get(b);
            List<List<Match>> rounds = buildRoundRobinRounds(teams, getBracketLabel(b));

            List<Match> ordered = new ArrayList<>();
            for (List<Match> r : rounds) ordered.addAll(r);
            perBracketOrderedMatches.add(ordered);
        }

        List<Match> globalOrder = interleaveOneByOne(perBracketOrderedMatches);

        int idx = 0;
        int day = Integer.parseInt(stringsInput.get(idx));
        int hours = Integer.parseInt(stringsInput.get(idx + 1).split(":")[0]);
        int minutes = Integer.parseInt(stringsInput.get(idx + 1).split(":")[1]);
        int minutesForMatch = Integer.parseInt(minutesPerMatch);

        int endHours = Integer.parseInt(stringsInput.get(idx + 2).split(":")[0]);
        int endMinutes = Integer.parseInt(stringsInput.get(idx + 2).split(":")[1]);

        int startTotalMinutes = hours * 60 + minutes;
        int endTotalMinutes = endHours * 60 + endMinutes;

        for (int i = 0; i < globalOrder.size(); i++) {
            Match m = globalOrder.get(i);
            m.day = day;
            m.time = String.format("%d:%02d", hours, minutes);

            startTotalMinutes += minutesForMatch;

            boolean hasNext = (i < globalOrder.size() - 1);
            if (hasNext) {
                if (startTotalMinutes + minutesForMatch <= endTotalMinutes) {
                    hours = startTotalMinutes / 60;
                    minutes = startTotalMinutes % 60;
                } else {
                    if (stringsInput.size() > idx + 3) {
                        idx += 3;
                        day = Integer.parseInt(stringsInput.get(idx));
                        hours = Integer.parseInt(stringsInput.get(idx + 1).split(":")[0]);
                        minutes = Integer.parseInt(stringsInput.get(idx + 1).split(":")[1]);
                        startTotalMinutes = hours * 60 + minutes;
                        endHours = Integer.parseInt(stringsInput.get(idx + 2).split(":")[0]);
                        endMinutes = Integer.parseInt(stringsInput.get(idx + 2).split(":")[1]);
                        endTotalMinutes = endHours * 60 + endMinutes;
                    } else {
                        throw new RuntimeException("Fasce orarie esaurite: non riesco a schedulare tutti i match.");
                    }
                }
            }

            MatchUtility.addNewMatch(tournament.key, m);
        }

        tournament.matches = new ArrayList<>(globalOrder);
        TournamentTeamUtility.saveBracketForTeams(tournamentKey);
        TournamentUtility.updateNBracketsTournament(tournamentKey, nBrackets);
    }

    private static List<List<Match>> buildRoundRobinRounds(List<Team> originalTeams, String bracketLabel) {
        List<Team> teams = new ArrayList<>(originalTeams);
        if (teams.size() % 2 == 1) {
            Team bye = new Team();
            bye.key = "__BYE__";
            teams.add(bye);
        }

        int n = teams.size();
        int roundsCount = n - 1;
        int half = n / 2;

        List<Team> rot = new ArrayList<>(teams);
        List<List<Match>> rounds = new ArrayList<>();

        for (int r = 0; r < roundsCount; r++) {
            List<Match> roundMatches = new ArrayList<>();
            for (int i = 0; i < half; i++) {
                Team t1 = rot.get(i);
                Team t2 = rot.get(n - 1 - i);
                if (!isBye(t1) && !isBye(t2)) {
                    roundMatches.add(new Match(t1.key, t2.key, 0, "0:00", 0, 0, bracketLabel));
                }
            }
            rounds.add(roundMatches);

            List<Team> next = new ArrayList<>(n);
            next.add(rot.get(0));
            next.add(rot.get(n - 1));
            next.addAll(rot.subList(1, n - 1));
            rot = next;
        }

        return rounds;
    }

    private static boolean isBye(Team t) {
        return t == null || "__BYE__".equals(t.key);
    }

    private static List<Match> interleaveOneByOne(ArrayList<List<Match>> perBracketOrderedMatches) {
        List<Iterator<Match>> iters = new ArrayList<>();
        for (List<Match> list : perBracketOrderedMatches) iters.add(list.iterator());

        List<Match> out = new ArrayList<>();
        int alive = iters.size();
        int idx = 0;

        while (alive > 0) {
            Iterator<Match> it = iters.get(idx);
            if (it.hasNext()) {
                out.add(it.next());
            }

            alive = 0;
            for (Iterator<Match> it2 : iters) if (it2.hasNext()) alive++;
            idx = (idx + 1) % iters.size();
        }
        return out;
    }

    public static int nMatchesItalianBracket(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        int nMatch = 0;

        for (int i = 0; i < tournament.teams.size(); i++) {
            nMatch += tournament.teams.size() - i - 1;
        }

        return nMatch;
    }

    public static int nMatchesMultipleBracket(String tournamentKey, int nBrackets) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);
        ArrayList<ArrayList<Team>> brackets = new ArrayList<>();
        int nMatch = 0;

        for (int i = 0; i < nBrackets; i++) {
            ArrayList<Team> bracket = new ArrayList<>();
            brackets.add(bracket);
        }

        for (int i = 0; i < tournament.teams.size(); i++) {
            brackets.get(i % nBrackets).add(tournament.teams.get(i));
        }

        for (int i = 0; i < nBrackets; i++) {
            for (int j = 0; j < brackets.get(i).size(); j++) {
                for (int k = j + 1; k < brackets.get(i).size(); k++) {
                    nMatch += 1;
                }
            }
        }

        return nMatch;
    }

    public static String getBracketLabel(int index) {
        StringBuilder label = new StringBuilder();
        while (index >= 0) {
            label.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        }
        return "BRACKET " + label;
    }

    public static String getBracketLetter(int index) {
        StringBuilder label = new StringBuilder();
        while (index >= 0) {
            label.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        }
        return "" + label;
    }
}
