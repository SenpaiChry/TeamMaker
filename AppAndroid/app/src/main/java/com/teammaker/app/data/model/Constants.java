package com.teammaker.app.data.model;

import java.util.ArrayList;

/**
 * Contenitore residuale di stato globale. In via di smantellamento:
 *   - cache dati (players, tournaments, downloadEnd, dbRoot) → repository
 *   - flag admin (logged) → AdminAuth.isAdmin()
 *   - stato generatore (nCycle, inputMaxDifference, lastDifference, teams) → TODO Step B
 *   - selezione giocatori (playersSelected) → TODO Step C
 */
public class Constants {
    public static float inputMaxDifference = 1.0F;
    public static float lastDifference = 0.0F;
    public static int nCycle = 0;
    public static ArrayList<Player> playersSelected = new ArrayList<>();
    public static ArrayList<Team> teams = new ArrayList<>();
}
