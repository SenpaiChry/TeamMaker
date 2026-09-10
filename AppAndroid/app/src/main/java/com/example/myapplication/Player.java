package com.example.myapplication;

import com.example.myapplication.Utility.StatsUtility;

import java.util.HashMap;

public class Player implements Comparable<Player> {
    public String key;
    public String name;
    public String surname;
    public String nickname;
    public String gender;
    public boolean isActive;
    public HashMap<String, Object> stats = new HashMap<>();
    /** Bonus per-stat: {statKey: true} solo se il giocatore ha il bonus attivo per quella stat. */
    public HashMap<String, Boolean> bonus = new HashMap<>();

    public Player(String key, String name, String surname, String nickname, String gender, boolean isActive, HashMap<String, Object> stats) {
        this.key = key;
        this.name = name;
        this.surname = surname;
        this.nickname = nickname;
        this.gender = gender;
        this.isActive = isActive;
        this.stats = stats;
    }

    public Player(String name, String surname, String nickname, String gender, HashMap<String, Object> stats) {
        this.name = name;
        this.surname = surname;
        this.nickname = nickname;
        this.isActive = true;
        this.gender = gender;
        this.stats = stats;
    }

    public Player() {
        if (this.stats.isEmpty()) {
            // Inizializza a 0 tutte le stat del catalogo dinamico corrente
            for (StatDefinition def : StatsUtility.getDefinitions()) {
                this.stats.put(def.key, 0);
            }
        }
    }

    public Player(Player other) {
        this.key = other.key;
        this.name = other.name;
        this.surname = other.surname;
        this.nickname = other.nickname;
        this.gender = other.gender;
        this.isActive = other.isActive;

        // Copia profonda della mappa stats
        this.stats = new HashMap<>();
        for (String key : other.stats.keySet()) {
            Object value = other.stats.get(key);

            // Per ora assumiamo che value sia un numero o tipo immutabile
            // Se servono oggetti più complessi, qui va fatto un clone profondo
            this.stats.put(key, value);
        }
        this.bonus = new HashMap<>(other.bonus);
    }

    public Float getVote() {
        float voteTemp = 0;
        // Somma i valori di tutte le stat presenti nel catalogo dinamico. Valori
        // orfani (stat cancellate) non contribuiscono; stat nuove senza valore
        // salvato valgono 0. Se una stat ammette bonus e il giocatore ce l'ha,
        // aggiunge un ulteriore def.step al voto.
        for (StatDefinition def : StatsUtility.getDefinitions()) {
            Object value = stats.get(def.key);
            if (value != null) {
                try {
                    voteTemp += Float.parseFloat(String.valueOf(value));
                } catch (NumberFormatException ignored) { }
            }
            if (def.allowBonus && Boolean.TRUE.equals(bonus.get(def.key))) {
                voteTemp += (float) def.step;
            }
        }
        return voteTemp;
    }

    public HashMap<String, Object> getStats() {
        return stats;
    }

    public void setTo(Player player) {
        this.key = player.key;
        this.name = player.name;
        this.surname = player.surname;
        this.nickname = player.nickname;
        this.gender = player.gender;
        this.isActive = player.isActive;
        this.stats = player.stats;
    }

    public String getSurnameOrNickname() {
        if (surname == null || surname.isEmpty()) {
            return nickname;
        }

        return surname;
    }

    public String getNameAndSurname(int nCharSurname) {
        String toReturn = this.name;

        if (surname.length() > nCharSurname) {
            toReturn += " " + this.surname.substring(0, nCharSurname) + ".";
        } else if (!surname.isEmpty()) {
            toReturn += " " + this.surname;
        }

        return toReturn;
    }

    public boolean containsString(String s) {
        s = s.toLowerCase();

        return name.toLowerCase().contains(s)
                || surname.toLowerCase().contains(s)
                || nickname.toLowerCase().contains(s)
                || (name + " " + surname + " " + name + " " + surname).toLowerCase().contains(s);
    }

    @Override
    public String toString() {
        return "Player: " + "name='" + name + '\'';
    }

    @Override
    public int compareTo(Player o) {
        return - Float.compare(this.getVote(), o.getVote());
    }
}
