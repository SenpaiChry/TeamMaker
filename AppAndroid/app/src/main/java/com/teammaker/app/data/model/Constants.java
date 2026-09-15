package com.teammaker.app.data.model;

import java.util.ArrayList;

/**
 * Contenitore residuale di stato globale. In via di smantellamento:
 *   - cache dati e config → repository / AppConfig / AdminAuth (Step A)
 *   - stato generatore (nCycle, inputMaxDifference, lastDifference, teams) → TeamGenerator (Step B)
 *   - selezione giocatori (playersSelected) → GenerateActivity (TODO Step C)
 */
public class Constants {
    public static ArrayList<Player> playersSelected = new ArrayList<>();
}
