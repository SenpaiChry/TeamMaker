package com.example.myapplication.Utility;

import android.util.Log;

import com.example.myapplication.MainActivity;
import com.example.myapplication.Player;
import com.example.myapplication.Team;
import com.example.myapplication.Model.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TeamGeneratorUtility {

    public static void makeTeams(int nPlayerInSquad, int nAlgorithm, ArrayList<Player> playersSelected) {
        Constants.nCycle = 0;

        if (nAlgorithm == 2) {
            initTeams(playersSelected, nPlayerInSquad);
            algorithm2(playersSelected);
        } else if (nAlgorithm == 5) {
            int nThreads = Runtime.getRuntime().availableProcessors(); // o un valore dinamico, tipo
            makeTeamsThreads(nPlayerInSquad, nThreads, playersSelected);
        }
    }

    public static void initTeams(ArrayList<Player> playersSelected, int nPlayerInSquad) {
        Constants.teams.clear();

        for (int i = 0; i < Math.ceil((float)playersSelected.size() / nPlayerInSquad); i ++) {
            Constants.teams.add(new Team());
        }
    }

    public static void algorithm2(ArrayList<Player> playersSelected) {
        int nPlayers = playersSelected.size();
        int nTeams = Constants.teams.size();
        Collections.sort(playersSelected);

        for (int i = 0; i < nPlayers; i ++) {
            if (i % nTeams == 0 && i != 0) {
                Collections.reverse(Constants.teams);
            }

            Constants.teams.get(i % nTeams).addPlayer(playersSelected.get(i));
        }
    }

    public static void makeTeamsThreads(int nPlayerInSquad, int nThreads, ArrayList<Player> playersSelected) {
        final int maxRetries = 50_000;
        final AtomicBoolean solutionFound = new AtomicBoolean(false);
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        int nPlayers = playersSelected.size();
        int nTeams = (int) Math.ceil((float) nPlayers / nPlayerInSquad);

        final float[] votes = new float[nPlayers];
        final boolean[] isFemale = new boolean[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            votes[i] = playersSelected.get(i).getVote();
            isFemale[i] = "F".equalsIgnoreCase(playersSelected.get(i).gender);
        }

        Integer[] sortedByVote = new Integer[nPlayers];
        for (int i = 0; i < nPlayers; i++) sortedByVote[i] = i;
        Arrays.sort(sortedByVote, (a, b) -> Float.compare(votes[b], votes[a]));

        final float baseTolerance = computeBaseTolerance(votes, nTeams);

        for (int t = 0; t < nThreads; t++) {
            final int threadIndex = t;
            executor.submit(() -> {
                Random rng = new Random();

                Integer[] indices = sortedByVote.clone();
                int[] assignment = new int[nPlayers];
                float[] teamVotes = new float[nTeams];
                int[] teamSizes = new int[nTeams];
                int[] teamFemales = new int[nTeams];

                for (int retry = 0; retry < maxRetries && !solutionFound.get(); retry++) {
                    // Perturba a OGNI retry: il greedy parte da un ordine diverso ogni volta
                    perturbSorted(indices, rng, 1 + retry % 4 + threadIndex);
                    Arrays.fill(teamVotes, 0f);
                    Arrays.fill(teamSizes, 0);
                    Arrays.fill(teamFemales, 0);

                    for (int i = 0; i < nPlayers; i++) {
                        int playerIdx = indices[i];
                        int weakestTeam = findWeakestTeam(teamVotes, teamSizes, nPlayerInSquad);
                        assignment[playerIdx] = weakestTeam;
                        teamVotes[weakestTeam] += votes[playerIdx];
                        teamSizes[weakestTeam]++;
                        if (isFemale[playerIdx]) teamFemales[weakestTeam]++;
                    }

                    float tolerance = Math.max(Constants.inputMaxDifference, baseTolerance) + retry / 10000f;

                    if (isValid(teamVotes, teamFemales, tolerance)) {
                        tryCommitSolution(solutionFound, assignment, playersSelected, nTeams, retry, teamVotes);
                        return;
                    }

                    if (!femaleBalanced(teamFemales)) {
                        swapToBalanceFemales(assignment, votes, isFemale, teamVotes, teamFemales, nPlayers, nTeams);
                        if (isValid(teamVotes, teamFemales, tolerance)) {
                            tryCommitSolution(solutionFound, assignment, playersSelected, nTeams, retry, teamVotes);
                            return;
                        }
                    }

                    swapToBalanceVotes(assignment, votes, isFemale, teamVotes, teamFemales, nPlayers, nTeams, tolerance);
                    if (isValid(teamVotes, teamFemales, tolerance)) {
                        tryCommitSolution(solutionFound, assignment, playersSelected, nTeams, retry, teamVotes);
                        return;
                    }

                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Log.d("FATAL", e.toString());
        }
    }

    private static int findWeakestTeam(float[] teamVotes, int[] teamSizes, int maxSize) {
        int best = -1;
        for (int i = 0; i < teamVotes.length; i++) {
            if (teamSizes[i] >= maxSize) continue;
            if (best == -1 || teamVotes[i] < teamVotes[best]) best = i;
        }
        return best;
    }

    private static boolean isValid(float[] teamVotes, int[] teamFemales, float tolerance) {
        float minV = Float.MAX_VALUE, maxV = 0;
        for (float v : teamVotes) {
            if (v < minV) minV = v;
            if (v > maxV) maxV = v;
        }
        return (maxV - minV <= tolerance) && femaleBalanced(teamFemales);
    }

    private static boolean femaleBalanced(int[] teamFemales) {
        int min = Integer.MAX_VALUE, max = 0;
        for (int f : teamFemales) {
            if (f < min) min = f;
            if (f > max) max = f;
        }
        return (max - min) <= 1;
    }

    private static void swapToBalanceFemales(int[] assignment, float[] votes, boolean[] isFemale,
                                             float[] teamVotes, int[] teamFemales, int nPlayers, int nTeams) {

        for (int pass = 0; pass < nTeams; pass++) {
            int richTeam = 0, poorTeam = 0;
            for (int i = 1; i < nTeams; i++) {
                if (teamFemales[i] > teamFemales[richTeam]) richTeam = i;
                if (teamFemales[i] < teamFemales[poorTeam]) poorTeam = i;
            }
            if (teamFemales[richTeam] - teamFemales[poorTeam] <= 1) break;

            int bestF = -1, bestM = -1;
            float bestDelta = Float.MAX_VALUE;

            for (int p = 0; p < nPlayers; p++) {
                if (assignment[p] != richTeam || !isFemale[p]) continue;
                for (int q = 0; q < nPlayers; q++) {
                    if (assignment[q] != poorTeam || isFemale[q]) continue;
                    float delta = Math.abs(votes[p] - votes[q]);
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bestF = p;
                        bestM = q;
                    }
                }
            }

            if (bestF == -1 || bestM == -1) break;

            teamVotes[richTeam] += votes[bestM] - votes[bestF];
            teamVotes[poorTeam] += votes[bestF] - votes[bestM];
            teamFemales[richTeam]--;
            teamFemales[poorTeam]++;
            assignment[bestF] = poorTeam;
            assignment[bestM] = richTeam;
        }
    }

    private static void swapToBalanceVotes(int[] assignment, float[] votes, boolean[] isFemale,
                                           float[] teamVotes, int[] teamFemales, int nPlayers, int nTeams, float tolerance) {

        for (int pass = 0; pass < 20; pass++) {
            int richTeam = 0, poorTeam = 0;
            for (int i = 1; i < nTeams; i++) {
                if (teamVotes[i] > teamVotes[richTeam]) richTeam = i;
                if (teamVotes[i] < teamVotes[poorTeam]) poorTeam = i;
            }
            if (teamVotes[richTeam] - teamVotes[poorTeam] <= tolerance) break;

            int bestA = -1, bestB = -1;
            float bestImprovement = 0;

            for (int p = 0; p < nPlayers; p++) {
                if (assignment[p] != richTeam) continue;
                for (int q = 0; q < nPlayers; q++) {
                    if (assignment[q] != poorTeam) continue;
                    if (votes[p] <= votes[q]) continue;

                    float newRich = teamVotes[richTeam] - votes[p] + votes[q];
                    float newPoor = teamVotes[poorTeam] - votes[q] + votes[p];
                    float newDiff = Math.abs(newRich - newPoor);
                    float oldDiff = teamVotes[richTeam] - teamVotes[poorTeam];
                    float improvement = oldDiff - newDiff;

                    if (improvement <= bestImprovement) continue;

                    boolean swapChangesFemales = (isFemale[p] != isFemale[q]);
                    if (swapChangesFemales) {
                        int[] simulatedFemales = teamFemales.clone();
                        if (isFemale[p]) { simulatedFemales[richTeam]--; simulatedFemales[poorTeam]++; }
                        else             { simulatedFemales[richTeam]++; simulatedFemales[poorTeam]--; }
                        if (!femaleBalanced(simulatedFemales)) continue;
                    }

                    bestImprovement = improvement;
                    bestA = p;
                    bestB = q;
                }
            }

            if (bestA == -1) break;

            if (isFemale[bestA] != isFemale[bestB]) {
                if (isFemale[bestA]) { teamFemales[richTeam]--; teamFemales[poorTeam]++; }
                else                 { teamFemales[richTeam]++; teamFemales[poorTeam]--; }
            }

            teamVotes[richTeam] += votes[bestB] - votes[bestA];
            teamVotes[poorTeam] += votes[bestA] - votes[bestB];
            assignment[bestA] = poorTeam;
            assignment[bestB] = richTeam;
        }
    }

    private static void perturbSorted(Integer[] indices, Random rng, int strength) {
        int n = indices.length;
        int windowSize = Math.min(n, 4 * strength);
        int start = rng.nextInt(Math.max(1, n - windowSize + 1));
        for (int i = start + windowSize - 1; i > start; i--) {
            int j = start + rng.nextInt(i - start + 1);
            Integer tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
        }
    }

    private static float computeBaseTolerance(float[] votes, int nTeams) {
        float sum = 0;
        for (float v : votes) sum += v;
        float mean = sum / votes.length;
        float variance = 0;
        for (float v : votes) variance += (v - mean) * (v - mean);
        float stddev = (float) Math.sqrt(variance / votes.length);
        return stddev / nTeams;
    }

    private static void tryCommitSolution(AtomicBoolean solutionFound, int[] assignment,
                                          ArrayList<Player> players, int nTeams, int retry, float[] teamVotes) {

        if (!solutionFound.compareAndSet(false, true)) return;

        float minV = Float.MAX_VALUE, maxV = 0;
        for (float v : teamVotes) {
            if (v < minV) minV = v;
            if (v > maxV) maxV = v;
        }

        ArrayList<Team> result = new ArrayList<>();
        for (int i = 0; i < nTeams; i++) result.add(new Team());
        for (int i = 0; i < assignment.length; i++) {
            result.get(assignment[i]).addPlayer(players.get(i));
        }

        synchronized (MainActivity.class) {
            Constants.nCycle = retry;
            Constants.teams = result;
            Constants.lastDifference = maxV - minV; // utile per mostrare all'utente la qualità
        }
    }
}