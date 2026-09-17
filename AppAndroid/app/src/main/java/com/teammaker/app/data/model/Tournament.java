package com.teammaker.app.data.model;

import com.teammaker.app.data.repository.MatchRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

public class Tournament {
    public String key;
    public String name;
    public int nBracket;
    public Calendar date = null;
    public boolean isValid;
    /** Se true, il torneo e' congelato: da UI restano usabili solo SBLOCCA e ATTIVA/DISATTIVA. */
    public boolean locked = false;
    public ArrayList<Team> teams = new ArrayList<>();
    public ArrayList<Match> matches = new ArrayList<>();

    public Tournament() { }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    @Override
    public String toString() {
        return "Tournament{" +
                ", key='" + key +
                "teams=" + teams +
                ", matches=" + matches +
                ", isValid=" + isValid +
                '}';
    }

    public Team getTeamEntityByKey(String key) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).key.equals(key)) {
                return teams.get(i);
            }
        }

        return null;
    }

    public String getTeamByKey(String key) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).key.equals(key)) {
                return teams.get(i).toStringOnlyName();
            }
        }

        return "TO DO";
    }

    public Match getMatchByKey(String key) {
        for (Match match : matches) {
            if (match.key.equals(key)) {
                return match;
            }
        }

        return null;
    }

    public String toStringNTeamByKey(String key) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).key.equals(key)) {
                return "TEAM " + (i + 1);
            }
        }

        return "TO DO";
    }

    public int getNTeamByKey(String key) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).key.equals(key)) {
                return i + 1;
            }
        }

        return 0;
    }

    public int getNMatchByKey(String key) {
        ArrayList<Match> matchesTemp = new ArrayList<>(this.matches);
        Collections.sort(matchesTemp, MatchRepository.BY_DAY_TIME);

        for (int i = 0; i < matchesTemp.size(); i++) {
            if (matchesTemp.get(i).key.equals(key)) {
                return i + 1;
            }
        }

        return 0;
    }

    public String getTeamByIndex(int index) {
        if (index == 0) {
            return "TO DO";
        } else {
            return teams.get(index - 1).key;
        }
    }
}
