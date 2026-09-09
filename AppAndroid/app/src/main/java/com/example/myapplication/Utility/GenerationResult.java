package com.example.myapplication.Utility;

import com.example.myapplication.Team;

import java.util.List;

/**
 * Esito della generazione squadre: il chiamante distingue successo, mancanza di
 * giocatori e timeout senza dover leggere Constants globali. Le squadre sono
 * quelle salvate in Constants.teams su successo (stesso riferimento).
 */
public class GenerationResult {

    public enum Reason {
        OK,
        NOT_ENOUGH_PLAYERS,
        UNKNOWN_ALGORITHM,
        TIMEOUT
    }

    public final boolean success;
    public final Reason reason;
    public final List<Team> teams;   // popolato solo su successo
    public final int retries;

    private GenerationResult(boolean success, Reason reason, List<Team> teams, int retries) {
        this.success = success;
        this.reason = reason;
        this.teams = teams;
        this.retries = retries;
    }

    public static GenerationResult ok(List<Team> teams, int retries) {
        return new GenerationResult(true, Reason.OK, teams, retries);
    }

    public static GenerationResult fail(Reason reason) {
        return new GenerationResult(false, reason, null, 0);
    }
}
