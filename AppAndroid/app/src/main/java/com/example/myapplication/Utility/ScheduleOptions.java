package com.example.myapplication.Utility;

/**
 * Opzioni di generazione del calendario. Ossatura del refactor: separa la scelta
 * del formato (chi gioca contro chi) dai parametri di scheduling. Verrà usata dai
 * generatori Italiana / Gironi / Champions nelle tappe successive.
 */
public class ScheduleOptions {

    public enum Format { ITALIAN, GROUPS, CHAMPIONS }

    public Format format;
    public boolean homeAndAway;   // ITALIAN / GROUPS
    public int nBrackets;         // GROUPS
    public int matchesPerTeam;    // CHAMPIONS (K)
    public int minutesPerMatch;
}