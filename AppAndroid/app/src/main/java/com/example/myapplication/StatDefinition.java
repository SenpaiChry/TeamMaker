package com.example.myapplication;

import java.util.ArrayList;

/**
 * Definizione di una statistica: la lista di stat non e' piu' hardcoded in Constants,
 * arriva da Firebase (nodo teammaker/stats). Le chiavi sono push-key opache (non le
 * label), cosi' l'admin puo' rinominare senza rompere i valori nei player.
 */
public class StatDefinition {
    public static final String TYPE_STARS = "STARS";
    public static final String TYPE_RANGE = "RANGE";

    public String key;                        // push-key Firebase
    public String label;                      // testo mostrato all'utente
    public String type = TYPE_STARS;          // STARS (default) o RANGE
    public double max = 4;                    // valore massimo (per STARS)
    public double step = 1;                   // ogni "stella" quanto vale
    public int order = 0;                     // ordine di visualizzazione
    public boolean allowBonus = false;        // se true, il giocatore puo' avere una stella bonus in piu' (solo STARS)
    public ArrayList<String> values;          // etichette per RANGE (es. fasce altezza)
}
