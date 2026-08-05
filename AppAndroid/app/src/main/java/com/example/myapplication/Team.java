package com.example.myapplication;

import com.example.myapplication.Model.Constants;

import java.util.ArrayList;

public class Team {
    public String key;
    public float totalVote = 0;
    public int points = 0;
    public String bracket = "";
//    float average = 0;
    public ArrayList<Player> players = new ArrayList<>();

    public Team() {}

    public void addPlayer(Player player) {
        this.players.add(player);
        this.totalVote += player.getVote();
        this.totalVote = (float) (Math.round(this.totalVote * 10.0) / 10.0);
//        this.average = (float) (Math.round(this.totalVote / this.players.size() * 10.0) / 10.0);
    }

    public float getTotalVote() {
        return totalVote;
    }

    public void addPlayerByKey(String key) {
        for (Player p : Constants.players) {
            if (p.key.equals(key)) {
                Player temp = new Player();
                temp.setTo(p);
                addPlayer(temp);
            }
        }
    }

    @Override
    public String toString() {
        return  key + " - " + totalVote + " - " + players;
    }

    public String toStringOnlyName() {
        StringBuilder toReturn = new StringBuilder();

        for (int i = 0; i < players.size(); i++) {
            toReturn.append(players.get(i).name);

            if (i != players.size() - 1) {
                toReturn.append(" - ");
            }
        }

        return toReturn.toString();
    }

    public String toStringShortName() {
        StringBuilder toReturn = new StringBuilder();

        for (int i = 0; i < players.size(); i++) {
            toReturn.append(players.get(i).name);

            if (!players.get(i).surname.isEmpty())
                toReturn.append(" ").append(players.get(i).surname.charAt(0));

            if (i != players.size() - 1)
                toReturn.append(" - ");
        }

        return toReturn.toString();
    }

    public String toStringNameAndSurname() {
        StringBuilder toReturn = new StringBuilder();

        for (int i = 0; i < players.size(); i++) {
            toReturn.append(players.get(i).name);

            if (!players.get(i).surname.isEmpty())
                toReturn.append(" ").append(players.get(i).surname);

            if (i != players.size() - 1)
                toReturn.append(" - ");
        }

        return toReturn.toString();
    }

    public String toStringNameAndSurnameNChar(int nChar) {
        StringBuilder toReturn = new StringBuilder();

        for (int i = 0; i < players.size(); i++) {
            toReturn.append(players.get(i).getNameAndSurname(nChar));

            if (i != players.size() - 1)
                toReturn.append(" - ");
        }

        return toReturn.toString();
    }
}
