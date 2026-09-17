package com.teammaker.app.data.mapper;

import com.google.firebase.database.DataSnapshot;

/**
 * Estrazione difensiva dei valori da Firebase. E' il *solo* posto dove
 * concentriamo il "il campo poteva essere null / di tipo inatteso": i mapper
 * chiamanti lavorano sempre con valori normalizzati, senza null-check sparsi.
 *
 * Tollerante ai formati misti: intOr accetta sia Number sia stringhe numeriche,
 * boolOr accetta sia Boolean sia stringhe "true"/"false" (retrocompatibile con
 * lo schema dove is_valid era salvato come stringa).
 */
public final class MapperUtils {

    private MapperUtils() { }

    /** Stringa del nodo figlio, o "" se assente/null. */
    public static String str(DataSnapshot s, String child) {
        return str(s, child, "");
    }

    public static String str(DataSnapshot s, String child, String def) {
        String v = s.child(child).getValue(String.class);
        return v != null ? v : def;
    }

    /** Intero del nodo figlio (accetta anche stringhe numeriche); def se null/malformato. */
    public static int intOr(DataSnapshot s, String child, int def) {
        Object v = s.child(child).getValue();
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Boolean tollerante (Boolean vero o stringa "true"/"false"); def se assente. */
    public static boolean boolOr(DataSnapshot s, String child, boolean def) {
        Object v = s.child(child).getValue();
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;
        return "true".equalsIgnoreCase(String.valueOf(v));
    }
}
