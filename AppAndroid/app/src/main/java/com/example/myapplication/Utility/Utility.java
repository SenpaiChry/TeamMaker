package com.example.myapplication.Utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.myapplication.Match;
import com.example.myapplication.Tournament;

public class Utility {
    public interface FirebaseCallback {
        void onComplete(boolean success);
    }

    public static void generateTable(String tournamentKey) {
        Tournament tournament = TournamentUtility.getTournamentByKey(tournamentKey);

        for (Match match : tournament.matches) {
            if (match.type.contains("BRACKET") || match.type.contains("GIRONE")) { // todo sistema
                if  (match.points1 > match.points2) {
                    if (match.points1 == 15 || match.points1 == 25) {
                        tournament.teams.get(tournament.getNTeamByKey(match.keyTeam1) - 1).points += 3;
                    } else {
                        tournament.teams.get(tournament.getNTeamByKey(match.keyTeam1) - 1).points += 2;
                        tournament.teams.get(tournament.getNTeamByKey(match.keyTeam2) - 1).points += 1;
                    }
                } else if (match.points2 > match.points1) {
                    if (match.points2 == 15 || match.points2 == 25) {
                        tournament.teams.get(tournament.getNTeamByKey(match.keyTeam2) - 1).points += 3;
                    } else {
                        tournament.teams.get(tournament.getNTeamByKey(match.keyTeam2) - 1).points += 2;
                        tournament.teams.get(tournament.getNTeamByKey(match.keyTeam1) - 1).points += 1;
                    }
                }
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }
}
