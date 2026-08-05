package com.example.myapplication;

import android.content.Context;

import com.example.myapplication.Utility.TimeUtility;
import com.example.myapplication.Utility.TournamentTeamUtility;
import com.example.myapplication.Utility.Utility;

public class Match implements Comparable<Match> {
    public String key;
    public String keyTeam1;
    public String keyTeam2;
    public int day;
    public String time;
    public int points1;
    public int points2;
    public String type = "";

    public Match(String keyTeam1, String keyTeam2, int day, String time, int points1, int points2) {
        this.keyTeam1 = keyTeam1;
        this.keyTeam2 = keyTeam2;
        this.day = day;
        this.time = time;
        this.points1 = points1;
        this.points2 = points2;
    }

    public Match(String keyTeam1, String keyTeam2, int day, String time, int points1, int points2, String type) {
        this.keyTeam1 = keyTeam1;
        this.keyTeam2 = keyTeam2;
        this.day = day;
        this.time = time;
        this.points1 = points1;
        this.points2 = points2;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Match{" +
                "key='" + key + '\'' +
                ", keyTeam1='" + keyTeam1 + '\'' +
                ", keyTeam2='" + keyTeam2 + '\'' +
                ", day=" + day +
                ", time='" + time + '\'' +
                ", points1=" + points1 +
                ", points2=" + points2 +
                ", type='" + type + '\'' +
                '}';
    }

    public String toStringForNextMatch(Context context) {
        Team team1 = TournamentTeamUtility.getTeamByKey(keyTeam1);
        Team team2 = TournamentTeamUtility.getTeamByKey(keyTeam2);

        String team1String = team1 != null ? team1.toStringNameAndSurname() : "";
        String team2String = team2 != null ? team2.toStringNameAndSurname() : "";

        String toReturn = context.getString(R.string.next_match) + ": "
                + team1String + " VS " + team2String + " "
                + context.getString(R.string.day) + ": " + day + " "
                + context.getString(R.string.time) + ": " + time;

        return toReturn;
    }

    @Override
    public int compareTo(Match m) {
        if (m.day > this.day || (m.day == this.day && TimeUtility.isAfter(this.time, m.time))) {
            return - 1;
        } else if (m.day == this.day && this.time.equals(m.time)) {
            return 0;
        }

        return 1;
    }
}
