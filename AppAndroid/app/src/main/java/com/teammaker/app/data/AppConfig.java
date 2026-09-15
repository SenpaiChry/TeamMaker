package com.teammaker.app.data;

/**
 * Configurazione globale immutabile.
 * DB_ROOT punta al nodo radice Firebase da usare (produzione o staging).
 */
public final class AppConfig {

    public static final String DB_ROOT = "teammaker/";
    // public static final String DB_ROOT = "teammakerStaging/";

    private AppConfig() { }
}
